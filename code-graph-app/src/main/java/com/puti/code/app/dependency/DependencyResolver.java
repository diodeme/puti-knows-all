package com.puti.code.app.dependency;

import com.puti.code.app.dependency.BuildToolDetector.BuildTool;
import com.puti.code.base.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * 根据构建工具类型，调用对应命令导出依赖 JAR 到项目 .library/ 目录。
 * 支持自动检测项目所需 JDK 版本，并使用匹配的 JDK 执行构建命令。
 */
@Slf4j
public class DependencyResolver {

    private static final String LIBRARY_DIR_NAME = ".library";
    private static final String GRADLE_INIT_SCRIPT_NAME = "puti-copy-deps-init.gradle";

    private final CommandRunner commandRunner;
    private final JdkResolver jdkResolver;

    /** 最近一次过滤被排除的 JAR 文件，用于过滤后删除 */
    private List<File> lastExcludedJars = List.of();

    /** 最近一次过滤后被排除的 GAV→JAR 映射 */
    private Map<String, File> lastExcludedGavToJar = Map.of();

    /**
     * 依赖解析完整结果
     */
    public record DependencyResolveResult(
            Map<String, File> kept,
            Map<String, File> excluded,
            Map<String, String> classIndex,
            Set<String> projectModuleGavs
    ) {}

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Windows 下 Maven 需要 cmd /c 前缀才能正确执行 mvn.cmd。
     * Linux/macOS 直接传递原数组。
     */
    private String[] mavenCommand(String... args) {
        if (!isWindows()) return args;
        String[] result = new String[args.length + 2];
        result[0] = "cmd";
        result[1] = "/c";
        System.arraycopy(args, 0, result, 2, args.length);
        return result;
    }

    public DependencyResolver(long timeoutMinutes, JdkResolver jdkResolver) {
        this.commandRunner = new CommandRunner(timeoutMinutes);
        this.jdkResolver = jdkResolver;
    }

    /**
     * 解析项目依赖并导出到 {projectRootPath}/.library/
     *
     * @param projectRootPath 项目根目录绝对路径
     * @param buildTool       检测到的构建工具类型
     * @return 导出的 JAR 数量，-1 表示失败
     */
    public int resolve(String projectRootPath, BuildTool buildTool) {
        Path libraryDir = Path.of(projectRootPath, LIBRARY_DIR_NAME);
        cleanLibraryDir(libraryDir);

        return switch (buildTool) {
            case MAVEN -> resolveMaven(projectRootPath, libraryDir);
            case GRADLE -> resolveGradle(projectRootPath, libraryDir);
            case UNKNOWN -> {
                log.warn("Unknown build tool, skipping dependency resolution");
                yield -1;
            }
        };
    }

    /**
     * 解析项目依赖，导出 JAR 到 .library/，并返回 GAV → JAR File 映射。
     * 用于共享依赖图谱，每个 JAR 使用 "lib:{GAV}" 作为 repo_id。
     *
     * 内部合并为一次 Maven/Gradle 调用：同时导出 JAR 和获取 GAV 列表。
     *
     * @param projectRootPath 项目根目录绝对路径
     * @param buildTool       检测到的构建工具类型
     * @return GAV → JAR File 映射，失败时返回空 Map
     */
    public Map<String, File> resolveWithGav(String projectRootPath, BuildTool buildTool) {
        Path libraryDir = Path.of(projectRootPath, LIBRARY_DIR_NAME);
        cleanLibraryDir(libraryDir);

        Map<String, File> result = switch (buildTool) {
            case MAVEN -> resolveMavenWithGav(projectRootPath, libraryDir);
            case GRADLE -> resolveGradleWithGav(projectRootPath, libraryDir);
            case UNKNOWN -> {
                log.warn("Unknown build tool, skipping dependency resolution");
                yield Map.<String, File>of();
            }
        };

        result = filterDependencies(result);
        deleteExcludedJars();
        return result;
    }

    /**
     * 完整依赖解析：导出 JAR + GAV 映射，构建 classIndex，检测项目内部模块。
     * 用于 ProjectHandler 的共享依赖图谱流程。
     */
    public DependencyResolveResult resolveWithGavFull(String projectRootPath, BuildTool buildTool) {
        Path libraryDir = Path.of(projectRootPath, LIBRARY_DIR_NAME);
        cleanLibraryDir(libraryDir);

        Map<String, File> allGavToJar = switch (buildTool) {
            case MAVEN -> resolveMavenWithGav(projectRootPath, libraryDir);
            case GRADLE -> resolveGradleWithGav(projectRootPath, libraryDir);
            case UNKNOWN -> {
                log.warn("Unknown build tool, skipping dependency resolution");
                yield Map.<String, File>of();
            }
        };

        // 过滤前检测项目内部模块（基于 Maven 多模块 install 产生的 JAR）
        Set<String> projectModuleGavs = detectProjectModules(allGavToJar, projectRootPath, buildTool);

        // 过滤
        Map<String, File> kept = filterDependencies(allGavToJar);

        // 从 kept 和 excluded 中移除项目内部模块
        // 项目模块的源码由 Phase 3 Spoon 直接解析，不应由 LibraryHandler 处理
        // 否则同一类会同时存在 lib 风格和项目风格两种 ID 的节点，导致边断裂
        Map<String, File> projectModules = new LinkedHashMap<>();
        for (String moduleGav : projectModuleGavs) {
            File jar = kept.remove(moduleGav);
            if (jar != null) {
                projectModules.put(moduleGav, jar);
            }
        }

        // 构建被排除的 GAV→JAR 映射（从全量中减去 kept 和 projectModules，保留 JAR 不删除以供 classIndex 扫描）
        Map<String, File> excluded = new LinkedHashMap<>(allGavToJar);
        for (String keptGav : kept.keySet()) {
            excluded.remove(keptGav);
        }
        for (String moduleGav : projectModuleGavs) {
            excluded.remove(moduleGav);
        }
        lastExcludedGavToJar = excluded;

        // 构建 classIndex：扫描所有 JAR（含 excluded），排除项目内部模块
        Map<String, String> classIndex = buildClassIndex(allGavToJar, projectModuleGavs);

        // 不在此处删除 excluded JAR！
        // Spoon 分析期间需要 excluded JAR 在 classpath 上（如 Spring MVC），
        // 否则 @RequestMapping 等注解无法被解析为完整 FQN，入口点判定失效。
        // 删除由调用方在 handle() 完成后执行。

        log.info("[DependencyResolver] Result: {} kept, {} excluded, {} project modules",
                kept.size(), excluded.size(), projectModules.size());

        return new DependencyResolveResult(kept, excluded, classIndex, projectModuleGavs);
    }

    /**
     * 检测项目内部模块 GAV：groupId 与项目自身 groupId 相同的依赖。
     * 根据构建工具类型分别处理 Maven 和 Gradle 项目。
     */
    private Set<String> detectProjectModules(Map<String, File> gavToJar, String projectRootPath, BuildTool buildTool) {
        if (buildTool == BuildTool.GRADLE) {
            return detectProjectModulesGradle(gavToJar, projectRootPath);
        }
        // Maven 默认逻辑
        Set<String> moduleGavs = new java.util.HashSet<>();
        Path pomXml = Path.of(projectRootPath, "pom.xml");
        if (Files.exists(pomXml)) {
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomXml.toFile());
                NodeList groupIdNodes = doc.getElementsByTagName("groupId");
                String projectGroupId = null;
                if (groupIdNodes.getLength() > 0) {
                    projectGroupId = groupIdNodes.item(0).getTextContent().trim();
                }
                if (projectGroupId != null) {
                    for (String gav : gavToJar.keySet()) {
                        int colonIdx = gav.indexOf(':');
                        if (colonIdx <= 0) continue;
                        String gavGroupId = gav.substring(0, colonIdx);
                        if (gavGroupId.equals(projectGroupId)) {
                            moduleGavs.add(gav);
                        }
                    }
                }
                if (!moduleGavs.isEmpty()) {
                    log.info("Detected {} project internal modules (groupId={})", moduleGavs.size(), projectGroupId);
                }
            } catch (Exception e) {
                log.debug("Failed to detect project modules from pom.xml", e);
            }
        }
        return moduleGavs;
    }

    /**
     * Gradle 项目内部模块检测：读取项目 group，匹配相同 groupId 的 GAV。
     * 优先从 build.gradle / build.gradle.kts 读取 group 声明，
     * 回退到 gradle.properties，最后尝试从 Gradle init 脚本输出的 rootProject.group 获取。
     */
    private Set<String> detectProjectModulesGradle(Map<String, File> gavToJar, String projectRootPath) {
        Set<String> moduleGavs = new java.util.HashSet<>();
        String projectGroup = readGradleProjectGroup(projectRootPath);
        if (projectGroup == null || projectGroup.isEmpty()) {
            log.debug("Could not detect Gradle project group, skipping module detection");
            return moduleGavs;
        }
        for (String gav : gavToJar.keySet()) {
            int colonIdx = gav.indexOf(':');
            if (colonIdx <= 0) continue;
            String gavGroupId = gav.substring(0, colonIdx);
            if (gavGroupId.equals(projectGroup)) {
                moduleGavs.add(gav);
            }
        }
        if (!moduleGavs.isEmpty()) {
            log.info("Detected {} Gradle project internal modules (group={})", moduleGavs.size(), projectGroup);
        }
        return moduleGavs;
    }

    /**
     * 从 Gradle 构建文件中读取项目 group。
     * 按优先级依次尝试：root build.gradle(.kts) → gradle.properties
     */
    private String readGradleProjectGroup(String projectRootPath) {
        // 1. 尝试从 root build.gradle(.kts) 读取 group 声明
        for (String fileName : List.of("build.gradle", "build.gradle.kts")) {
            Path buildFile = Path.of(projectRootPath, fileName);
            if (Files.exists(buildFile)) {
                String group = parseGradleGroupFromFile(buildFile);
                if (group != null) return group;
            }
        }
        // 2. 尝试从 gradle.properties 读取 group
        Path gradleProps = Path.of(projectRootPath, "gradle.properties");
        if (Files.exists(gradleProps)) {
            try {
                for (String line : Files.readAllLines(gradleProps)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("group")) {
                        String value = extractGradlePropertyValue(trimmed);
                        if (value != null && !value.isEmpty()) return value;
                    }
                }
            } catch (IOException e) {
                log.debug("Failed to read gradle.properties", e);
            }
        }
        return null;
    }

    /**
     * 从 build.gradle(.kts) 内容中解析 group 声明。
     * 支持格式：group = 'com.example', group "com.example", project.group = 'com.example'
     */
    private String parseGradleGroupFromFile(Path buildFile) {
        try {
            for (String line : Files.readAllLines(buildFile)) {
                String trimmed = line.trim();
                // group = 'xxx' / group = "xxx"
                if (trimmed.startsWith("group")) {
                    String value = extractGradlePropertyValue(trimmed);
                    if (value != null && !value.isEmpty()) return value;
                }
            }
        } catch (IOException e) {
            log.debug("Failed to read Gradle build file: {}", buildFile, e);
        }
        return null;
    }

    /**
     * 从 Gradle 属性声明行中提取值。
     * 支持：group = 'xxx', group = "xxx", group 'xxx', group "xxx"
     */
    private String extractGradlePropertyValue(String line) {
        // group = 'xxx' / group = "xxx" / project.group = 'xxx'
        java.util.regex.Matcher assignMatcher = java.util.regex.Pattern.compile(
                "(?:project\\.)?group\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(line);
        if (assignMatcher.find()) return assignMatcher.group(1);
        // group 'xxx' / group "xxx" (Groovy method-call style)
        java.util.regex.Matcher callMatcher = java.util.regex.Pattern.compile(
                "group\\s+['\"]([^'\"]+)['\"]").matcher(line);
        if (callMatcher.find()) return callMatcher.group(1);
        // gradle.properties: group=xxx (no quotes)
        java.util.regex.Matcher propsMatcher = java.util.regex.Pattern.compile(
                "group\\s*=\\s*(.+)").matcher(line);
        if (propsMatcher.find()) {
            String val = propsMatcher.group(1).trim();
            if (!val.isEmpty()) return val;
        }
        return null;
    }

    /**
     * 扫描所有 JAR 的 .class 文件，构建 className → GAV 映射。
     * 排除项目内部模块（它们的类应使用项目风格 ID）。
     */
    private Map<String, String> buildClassIndex(Map<String, File> gavToJar, Set<String> projectModuleGavs) {
        Map<String, String> classIndex = new LinkedHashMap<>();
        int totalClasses = 0;
        for (Map.Entry<String, File> entry : gavToJar.entrySet()) {
            String gav = entry.getKey();
            if (projectModuleGavs.contains(gav)) continue;
            File jar = entry.getValue();
            if (jar == null || !jar.exists()) continue;
            try (JarFile jarFile = new JarFile(jar)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry je = entries.nextElement();
                    String name = je.getName();
                    if (name.endsWith(".class") && !name.contains("$")) {
                        String className = name.replace('/', '.').replace(".class", "");
                        classIndex.putIfAbsent(className, gav);
                        totalClasses++;
                    }
                }
            } catch (IOException e) {
                log.debug("Failed to scan JAR for classIndex: {}", jar.getName());
            }
        }
        log.info("Built classIndex: {} classes from {} JARs (excluded {} project modules)",
                totalClasses, gavToJar.size() - projectModuleGavs.size(), projectModuleGavs.size());
        return classIndex;
    }

    /**
     * 根据 filter_mode 过滤依赖，并收集被排除的 JAR 文件以供后续删除。
     * blacklist: 排除匹配 exclude.groups 的依赖
     * whitelist: 仅保留匹配 include.groups 的依赖
     */
    private Map<String, File> filterDependencies(Map<String, File> gavToJar) {
        String mode = AppConfig.getInstance().getDependencyFilterMode();
        if ("whitelist".equalsIgnoreCase(mode)) {
            return filterByWhitelist(gavToJar);
        }
        if (!"blacklist".equalsIgnoreCase(mode)) {
            log.warn("Unknown dependency.filter_mode '{}', falling back to blacklist", mode);
        }
        return filterByBlacklist(gavToJar);
    }

    private Map<String, File> filterByBlacklist(Map<String, File> gavToJar) {
        List<String> excludeGroups = AppConfig.getInstance().getDependencyExcludeGroups();
        if (excludeGroups == null || excludeGroups.isEmpty()) {
            lastExcludedJars = List.of();
            return gavToJar;
        }
        Map<String, File> filtered = new LinkedHashMap<>();
        List<File> excludedJars = new ArrayList<>();
        for (Map.Entry<String, File> entry : gavToJar.entrySet()) {
            int colonIdx = entry.getKey().indexOf(':');
            if (colonIdx <= 0) continue;
            String groupId = entry.getKey().substring(0, colonIdx);
            if (excludeGroups.stream().anyMatch(groupId::startsWith)) {
                excludedJars.add(entry.getValue());
                log.debug("Excluded by blacklist: {}", entry.getKey());
                continue;
            }
            filtered.put(entry.getKey(), entry.getValue());
        }
        lastExcludedJars = excludedJars;
        if (!excludedJars.isEmpty()) {
            log.info("Excluded {} / {} dependencies by blacklist ({} groups)",
                    excludedJars.size(), gavToJar.size(), excludeGroups.size());
        }
        return filtered;
    }

    private Map<String, File> filterByWhitelist(Map<String, File> gavToJar) {
        List<String> includeGroups = AppConfig.getInstance().getDependencyIncludeGroups();
        if (includeGroups == null || includeGroups.isEmpty()) {
            lastExcludedJars = new ArrayList<>(gavToJar.values());
            log.info("Whitelist mode but no include.groups configured, skipping all {} dependencies", gavToJar.size());
            return Map.of();
        }
        Map<String, File> filtered = new LinkedHashMap<>();
        List<File> excludedJars = new ArrayList<>();
        for (Map.Entry<String, File> entry : gavToJar.entrySet()) {
            int colonIdx = entry.getKey().indexOf(':');
            if (colonIdx <= 0) continue;
            String groupId = entry.getKey().substring(0, colonIdx);
            if (includeGroups.stream().anyMatch(groupId::startsWith)) {
                filtered.put(entry.getKey(), entry.getValue());
            } else {
                excludedJars.add(entry.getValue());
                log.debug("Excluded by whitelist: {}", entry.getKey());
            }
        }
        lastExcludedJars = excludedJars;
        if (!excludedJars.isEmpty()) {
            log.info("Whitelist kept {} / {} dependencies ({} groups), excluded {}",
                    filtered.size(), gavToJar.size(), includeGroups.size(), excludedJars.size());
        }
        return filtered;
    }

    public void deleteExcludedJars() {
        if (lastExcludedJars.isEmpty()) return;
        int deleted = 0;
        for (File jar : lastExcludedJars) {
            try {
                if (Files.deleteIfExists(jar.toPath())) deleted++;
            } catch (IOException e) {
                log.warn("Failed to delete excluded JAR: {}", jar.getName(), e);
            }
        }
        log.info("Deleted {}/{} excluded JAR files from .library/", deleted, lastExcludedJars.size());
        lastExcludedJars = List.of();
        lastExcludedGavToJar = Map.of();
    }

    // ======================== Maven ========================

    private int resolveMaven(String projectRootPath, Path libraryDir) {
        Map<String, String> env = buildEnvVars(projectRootPath, BuildTool.MAVEN);

        // 多模块项目：先 install 内部模块到本地仓库，解决 SNAPSHOT 依赖
        if (isMultiModuleMaven(projectRootPath)) {
            log.info("Multi-module Maven project detected, running install first");
            String[] installCmd = mavenCommand("mvn", "install", "-DskipTests", "-Ddocker.skip=true", "-q");
            int installExit = commandRunner.run(installCmd, projectRootPath, env);
            if (installExit != 0) {
                log.error("Maven install failed (exit {}), dependency resolution may be incomplete", installExit);
            }
        }

        String[] copyCmd = mavenCommand(
                "mvn", "dependency:copy-dependencies",
                "-DoutputDirectory=" + libraryDir.toAbsolutePath(),
                "-DincludeScope=runtime",
                "-q"
        );
        int exitCode = commandRunner.run(copyCmd, projectRootPath, env);
        if (exitCode != 0) {
            log.error("Maven dependency resolution failed with exit code: {}", exitCode);
            return -1;
        }
        return countJars(libraryDir);
    }

    /**
     * 检测 pom.xml 是否包含 &lt;modules&gt; 元素（多模块项目）
     */
    private boolean isMultiModuleMaven(String projectRootPath) {
        Path pomXml = Path.of(projectRootPath, "pom.xml");
        if (!Files.exists(pomXml)) {
            return false;
        }
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomXml.toFile());
            NodeList modules = doc.getElementsByTagName("modules");
            return modules.getLength() > 0;
        } catch (SAXException | IOException | ParserConfigurationException e) {
            log.warn("Failed to parse pom.xml for multi-module detection", e);
            return false;
        }
    }

    /**
     * 检测 settings.gradle(.kts) 是否包含 include 指令（多模块 Gradle 项目）
     */
    private boolean isMultiModuleGradle(String projectRootPath) {
        for (String fileName : List.of("settings.gradle", "settings.gradle.kts")) {
            Path settingsFile = Path.of(projectRootPath, fileName);
            if (Files.exists(settingsFile)) {
                try {
                    for (String line : Files.readAllLines(settingsFile)) {
                        String trimmed = line.trim();
                        // include ':module1', ':module2' / include "module1" / include(':module1')
                        if (trimmed.startsWith("include") && !trimmed.startsWith("includeMetaInf")) {
                            return true;
                        }
                    }
                } catch (IOException e) {
                    log.debug("Failed to read {}", settingsFile, e);
                }
            }
        }
        return false;
    }

    // ======================== Gradle ========================

    private int resolveGradle(String projectRootPath, Path libraryDir) {
        // 多模块项目：先构建内部模块 JAR
        if (isMultiModuleGradle(projectRootPath)) {
            log.info("Multi-module Gradle project detected, running jar task first");
            Map<String, String> env = buildEnvVars(projectRootPath, BuildTool.GRADLE);
            String[] gradleBase = resolveGradleCommand(projectRootPath);
            String[] jarCmd = new String[gradleBase.length + 3];
            System.arraycopy(gradleBase, 0, jarCmd, 0, gradleBase.length);
            jarCmd[gradleBase.length]     = "jar";
            jarCmd[gradleBase.length + 1] = "-x";
            jarCmd[gradleBase.length + 2] = "test";
            int jarExit = commandRunner.run(jarCmd, projectRootPath, env);
            if (jarExit != 0) {
                log.error("Gradle jar task failed (exit {}), dependency resolution may be incomplete", jarExit);
            }
        }

        Path initScript = createGradleInitScript(libraryDir);
        try {
            Map<String, String> env = buildEnvVars(projectRootPath, BuildTool.GRADLE);
            String[] gradleBase = resolveGradleCommand(projectRootPath);
            String[] command = new String[gradleBase.length + 4];
            System.arraycopy(gradleBase, 0, command, 0, gradleBase.length);
            command[gradleBase.length]     = "--init-script";
            command[gradleBase.length + 1] = initScript.toAbsolutePath().toString();
            command[gradleBase.length + 2] = "copyPutiDependencies";
            command[gradleBase.length + 3] = "-q";
            int exitCode = commandRunner.run(command, projectRootPath, env);
            if (exitCode != 0) {
                log.error("Gradle dependency resolution failed with exit code: {}", exitCode);
                return -1;
            }
            return countJars(libraryDir);
        } finally {
            deleteIfExists(initScript);
        }
    }

    /**
     * 检测项目是否自带 gradlew，优先使用，否则回退到系统 gradle。
     * 返回 String[] 而非拼接字符串，避免 bash -c 包裹，兼容 Windows。
     */
    private String[] resolveGradleCommand(String projectRootPath) {
        if (isWindows()) {
            Path gradlewBat = Path.of(projectRootPath, "gradlew.bat");
            if (Files.exists(gradlewBat)) return new String[]{"cmd", "/c", "gradlew.bat"};
            Path gradlew = Path.of(projectRootPath, "gradlew");
            if (Files.exists(gradlew)) return new String[]{"cmd", "/c", "gradlew"};
            log.info("No gradlew found in project, falling back to system gradle");
            return new String[]{"cmd", "/c", "gradle"};
        } else {
            Path gradlew = Path.of(projectRootPath, "gradlew");
            if (Files.exists(gradlew) && Files.isExecutable(gradlew)) {
                return new String[]{"./gradlew"};
            }
            log.info("No gradlew found in project, falling back to system gradle");
            return new String[]{"gradle"};
        }
    }

    /**
     * 生成 Gradle init 脚本（零侵入方案）。
     * 使用 allprojects 确保多模块项目也能收集依赖。
     */
    private Path createGradleInitScript(Path libraryDir) {
        String initScriptContent = """
                allprojects {
                    tasks.register('copyPutiDependencies', Copy) {
                        from(configurations.runtimeClasspath)
                        into('%s')
                    }
                }
                """.formatted(libraryDir.toAbsolutePath().toString().replace("\\", "/"));

        try {
            Path initScript = Files.createTempFile(GRADLE_INIT_SCRIPT_NAME, ".gradle");
            Files.writeString(initScript, initScriptContent);
            log.debug("Created Gradle init script at: {}", initScript);
            return initScript;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Gradle init script", e);
        }
    }

    // ======================== JDK ========================

    /**
     * 构建 JAVA_HOME 环境变量。根据项目所需 JDK 版本，从已配置的安装路径中选择匹配的 JDK。
     */
    private Map<String, String> buildEnvVars(String projectRootPath, BuildTool buildTool) {
        if (!jdkResolver.hasInstallations()) {
            return Map.of();
        }
        String javaHome = jdkResolver.resolveJavaHome(projectRootPath, buildTool);
        if (javaHome != null) {
            return Map.of("JAVA_HOME", javaHome);
        }
        return Map.of();
    }

    // ======================== GAV Resolution (Combined) ========================

    /**
     * Maven: 单次调用同时执行 copy-dependencies + dependency:list。
     * 先拷贝 JAR，再解析 GAV 列表与 JAR 文件名匹配。
     */
    private Map<String, File> resolveMavenWithGav(String projectRootPath, Path libraryDir) {
        // 多模块项目：先 install 内部模块
        if (isMultiModuleMaven(projectRootPath)) {
            log.info("Multi-module Maven project detected, running install first");
            Map<String, String> env = buildEnvVars(projectRootPath, BuildTool.MAVEN);
            String[] installCmd = mavenCommand("mvn", "install", "-DskipTests", "-Ddocker.skip=true", "-q");
            int installExit = commandRunner.run(installCmd, projectRootPath, env);
            if (installExit != 0) {
                log.error("Maven install failed (exit {}), dependency resolution may be incomplete", installExit);
            }
        }

        // 将依赖列表输出到临时文件
        Path gavOutputFile;
        try {
            gavOutputFile = Files.createTempFile("puti-deps-", ".txt");
        } catch (IOException e) {
            log.error("Failed to create temp file for dependency list", e);
            return Map.of();
        }

        try {
            Map<String, String> env = buildEnvVars(projectRootPath, BuildTool.MAVEN);
            // 单次 Maven 调用：同时拷贝 JAR + 输出 GAV 列表
            String[] combinedCmd = mavenCommand(
                    "mvn", "dependency:copy-dependencies", "dependency:list",
                    "-DoutputDirectory=" + libraryDir.toAbsolutePath(),
                    "-DincludeScope=runtime",
                    "-DoutputFile=" + gavOutputFile.toAbsolutePath(),
                    "-q"
            );
            int exitCode = commandRunner.run(combinedCmd, projectRootPath, env);
            if (exitCode != 0) {
                log.error("Maven dependency resolution failed with exit code: {}", exitCode);
                return Map.of();
            }

            // 解析 GAV 列表
            // Maven dependency:list -DoutputFile 实际格式: groupId:artifactId:type:version:scope
            List<String> lines = Files.readAllLines(gavOutputFile);
            Map<String, String> gavToJarName = new LinkedHashMap<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("[")) continue;
                String[] parts = trimmed.split(":");
                if (parts.length >= 4) {
                    String groupId = parts[0];
                    String artifactId = parts[1];
                    String type = parts[2].toLowerCase();
                    String version = parts[3];
                    String gav = groupId + ":" + artifactId + ":" + version;
                    // JAR 文件名格式: artifactId-version.type（仅处理 jar 类型）
                    if ("jar".equals(type) || "bundle".equals(type)) {
                        gavToJarName.put(gav, artifactId + "-" + version + ".jar");
                    }
                }
            }
            log.info("Parsed {} GAV entries from Maven dependency list", gavToJarName.size());

            return matchGavToJarFiles(gavToJarName, libraryDir);
        } catch (IOException e) {
            log.error("Failed to read Maven dependency list output", e);
            return Map.of();
        } finally {
            deleteIfExists(gavOutputFile);
        }
    }

    /**
     * Gradle: 单次调用同时拷贝 JAR + 输出 GAV 列表。
     * 使用合并的 init 脚本注册两个 task，通过 task 依赖确保拷贝先完成。
     */
    private Map<String, File> resolveGradleWithGav(String projectRootPath, Path libraryDir) {
        // 多模块项目：先构建内部模块 JAR，解决子项目间依赖
        if (isMultiModuleGradle(projectRootPath)) {
            log.info("Multi-module Gradle project detected, running jar task first");
            Map<String, String> env = buildEnvVars(projectRootPath, BuildTool.GRADLE);
            String[] gradleBase = resolveGradleCommand(projectRootPath);
            String[] jarCmd = new String[gradleBase.length + 3];
            System.arraycopy(gradleBase, 0, jarCmd, 0, gradleBase.length);
            jarCmd[gradleBase.length]     = "jar";
            jarCmd[gradleBase.length + 1] = "-x";
            jarCmd[gradleBase.length + 2] = "test";
            int jarExit = commandRunner.run(jarCmd, projectRootPath, env);
            if (jarExit != 0) {
                log.error("Gradle jar task failed (exit {}), dependency resolution may be incomplete", jarExit);
            }
        }

        Path gavOutputFile;
        try {
            gavOutputFile = Files.createTempFile("puti-gradle-deps-", ".txt");
        } catch (IOException e) {
            log.error("Failed to create temp file for Gradle dependency list", e);
            return Map.of();
        }

        // 创建合并的 init 脚本：同时拷贝 JAR + 输出 GAV
        Path initScript = createCombinedGradleInitScript(libraryDir, gavOutputFile);
        try {
            Map<String, String> env = buildEnvVars(projectRootPath, BuildTool.GRADLE);
            String[] gradleBase = resolveGradleCommand(projectRootPath);
            // 单次 Gradle 调用：copyPutiDependencies → printPutiDependencies（GAV task 依赖 copy task）
            String[] command = new String[gradleBase.length + 4];
            System.arraycopy(gradleBase, 0, command, 0, gradleBase.length);
            command[gradleBase.length]     = "--init-script";
            command[gradleBase.length + 1] = initScript.toAbsolutePath().toString();
            command[gradleBase.length + 2] = "printPutiDependencies";
            command[gradleBase.length + 3] = "-q";
            int exitCode = commandRunner.run(command, projectRootPath, env);
            if (exitCode != 0) {
                log.error("Gradle dependency resolution failed with exit code: {}", exitCode);
                return Map.of();
            }

            // 解析 GAV 列表（自动去重：多模块项目可能输出重复 GAV）
            List<String> lines = Files.readAllLines(gavOutputFile);
            Map<String, String> gavToJarName = new LinkedHashMap<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                // 格式: groupId:artifactId:version
                String[] parts = trimmed.split(":");
                if (parts.length >= 3) {
                    String gav = parts[0] + ":" + parts[1] + ":" + parts[2];
                    gavToJarName.putIfAbsent(gav, parts[1] + "-" + parts[2] + ".jar");
                }
            }
            log.info("Parsed {} unique GAV entries from Gradle dependency list", gavToJarName.size());

            return matchGavToJarFiles(gavToJarName, libraryDir);
        } catch (IOException e) {
            log.error("Failed to read Gradle dependency list output", e);
            return Map.of();
        } finally {
            deleteIfExists(initScript);
            deleteIfExists(gavOutputFile);
        }
    }

    /**
     * 创建合并的 Gradle init 脚本。
     * 注册 copyPutiDependencies（拷贝 JAR）和 printPutiDependencies（输出 GAV）两个 task。
     * printPutiDependencies 依赖 copyPutiDependencies，确保 JAR 先拷贝再获取 GAV。
     * 仅从 root project 收集依赖（避免多模块项目重复）。
     */
    private Path createCombinedGradleInitScript(Path libraryDir, Path gavOutputFile) {
        String script = """
                allprojects {
                    tasks.register('copyPutiDependencies', Copy) {
                        from(configurations.runtimeClasspath)
                        into('%s')
                    }
                }

                gradle.projectsEvaluated {
                    def rootProject = gradle.rootProject
                    if (rootProject != null) {
                        tasks.register('printPutiDependencies') {
                            dependsOn gradle.rootProject.tasks.named('copyPutiDependencies')
                            doLast {
                                def outputFile = new File('%s')
                                Set<String> seen = new HashSet<>()
                                rootProject.configurations.runtimeClasspath.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                                    def id = artifact.moduleVersion.id
                                    def gav = "${id.group}:${id.name}:${id.version}"
                                    if (seen.add(gav)) {
                                        outputFile << gav + '\\n'
                                    }
                                }
                            }
                        }
                    }
                }
                """.formatted(
                libraryDir.toAbsolutePath().toString().replace("\\", "/"),
                gavOutputFile.toAbsolutePath().toString().replace("\\", "/"));
        try {
            Path initScript = Files.createTempFile("puti-combined-init-", ".gradle");
            Files.writeString(initScript, script);
            log.debug("Created combined Gradle init script at: {}", initScript);
            return initScript;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create combined Gradle init script", e);
        }
    }

    /**
     * 将 GAV → JAR文件名 映射与 .library/ 目录中的实际 JAR 文件匹配。
     * 支持 classifier 后缀的模糊匹配（如 artifactId-version-classifier.jar）。
     */
    private Map<String, File> matchGavToJarFiles(Map<String, String> gavToJarName, Path libraryDir) {
        Map<String, File> result = new LinkedHashMap<>();
        File[] jarFiles = libraryDir.toFile().listFiles((d, name) -> name.endsWith(".jar"));
        if (jarFiles == null) {
            log.warn("No JAR files found in library directory: {}", libraryDir);
            return result;
        }

        // 建立 JAR 文件名 → File 的快速查找
        Map<String, File> fileNameMap = new LinkedHashMap<>();
        for (File jar : jarFiles) {
            fileNameMap.put(jar.getName(), jar);
        }

        int matched = 0;
        for (Map.Entry<String, String> entry : gavToJarName.entrySet()) {
            String gav = entry.getKey();
            String expectedJarName = entry.getValue();
            File jarFile = fileNameMap.get(expectedJarName);
            if (jarFile != null) {
                result.put(gav, jarFile);
                matched++;
            } else {
                // 模糊匹配：忽略 classifier 后缀（如 artifactId-version-classifier.jar）
                String baseName = expectedJarName.replace(".jar", "");
                File fuzzyMatch = fileNameMap.entrySet().stream()
                        .filter(e -> e.getKey().startsWith(baseName))
                        .map(Map.Entry::getValue)
                        .findFirst().orElse(null);
                if (fuzzyMatch != null) {
                    result.put(gav, fuzzyMatch);
                    matched++;
                } else {
                    log.warn("No JAR file found for GAV {} (expected: {})", gav, expectedJarName);
                }
            }
        }
        log.info("Matched {} out of {} GAV entries to JAR files", matched, gavToJarName.size());
        return result;
    }

    // ======================== Utilities ========================

    /**
     * 清理 .library/ 目录中的旧 JAR 文件，保留 dependency-manifest.json（增量更新需要）。
     */
    private void cleanLibraryDir(Path libraryDir) {
        if (!Files.exists(libraryDir)) {
            return;
        }
        try (Stream<Path> paths = Files.list(libraryDir)) {
            paths.filter(p -> {
                String name = p.getFileName().toString();
                // 保留 manifest 文件和子目录（如 output/）
                return name.endsWith(".jar");
            }).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("Failed to delete: {}", p, e);
                }
            });
        } catch (IOException e) {
            log.warn("Failed to clean library directory: {}", libraryDir, e);
        }
        log.info("Cleaned old JAR files from library directory: {}", libraryDir);
    }

    private int countJars(Path dir) {
        if (!Files.exists(dir)) {
            return 0;
        }
        try (Stream<Path> paths = Files.list(dir)) {
            long count = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                    .count();
            log.info("Found {} JAR files in {}", count, dir);
            return (int) count;
        } catch (IOException e) {
            log.error("Failed to count JAR files in {}", dir, e);
            return 0;
        }
    }

    private void deleteIfExists(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete init script: {}", file, e);
        }
    }
}
