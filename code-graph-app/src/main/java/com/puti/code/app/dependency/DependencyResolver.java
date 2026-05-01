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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        return filterExcluded(result);
    }

    /**
     * 按 groupId 前缀排除通用框架库。
     * 这些库 LLM 已充分了解，无需入库，排除可大幅减少分析时间和存储开销。
     */
    private Map<String, File> filterExcluded(Map<String, File> gavToJar) {
        List<String> excludeGroups = AppConfig.getInstance().getDependencyExcludeGroups();
        if (excludeGroups == null || excludeGroups.isEmpty()) {
            return gavToJar;
        }
        Map<String, File> filtered = new LinkedHashMap<>();
        int excluded = 0;
        for (Map.Entry<String, File> entry : gavToJar.entrySet()) {
            String gav = entry.getKey();
            String groupId = gav.substring(0, gav.indexOf(':'));
            if (excludeGroups.stream().anyMatch(groupId::startsWith)) {
                excluded++;
                continue;
            }
            filtered.put(entry.getKey(), entry.getValue());
        }
        if (excluded > 0) {
            log.info("Excluded {} / {} dependencies by group filter ({} groups)",
                    excluded, gavToJar.size(), excludeGroups.size());
        }
        return filtered;
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

    // ======================== Gradle ========================

    private int resolveGradle(String projectRootPath, Path libraryDir) {
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
