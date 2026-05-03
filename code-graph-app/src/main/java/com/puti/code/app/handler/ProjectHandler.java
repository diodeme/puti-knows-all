package com.puti.code.app.handler;


import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.app.dependency.BuildToolDetector;
import com.puti.code.app.dependency.BuildToolDetector.BuildTool;
import com.puti.code.app.dependency.DependencyDiff;
import com.puti.code.app.dependency.DependencyDiff.DiffResult;
import com.puti.code.app.dependency.DependencyManifest;
import com.puti.code.app.dependency.DependencyResolver;
import com.puti.code.app.dependency.DependencyResolver.DependencyResolveResult;
import com.puti.code.app.dependency.JdkResolver;
import com.puti.code.app.dependency.VersionMigrator;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.enums.ParseType;
import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeType;
import com.puti.code.base.model.FileNode;
import com.puti.code.base.model.Node;
import com.puti.code.base.model.NodeType;
import com.puti.code.base.util.IdGenerator;
import com.puti.code.repository.graph.GraphStorageRepository;
import com.puti.code.repository.tracker.DependencyTracker;
import com.puti.code.repository.graph.GraphStorageRepositoryFactory;
import lombok.extern.slf4j.Slf4j;
import spoon.Launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 项目源码分析处理器。
 * 在分析源码之前，自动检测构建工具、解析依赖并运行 LibraryHandler 入库。
 * 支持共享依赖图谱和增量更新。
 */
@Slf4j
public class ProjectHandler extends AbstractHandler {

    public static void main(String[] args) {
        ProjectHandler projectHandler = new ProjectHandler();
        projectHandler.run();
    }

    /**
     * 完整流程：自动解析依赖 → 增量分析依赖库 → 分析项目源码
     */
    public void run() {
        AppConfig appConfig = AppConfig.getInstance();
        log.info("[Project] Starting full analysis for project: {}", appConfig.getProjectRootPath());

        DependencyResolver resolver = null;

        // Phase 1: 自动检测构建工具（Maven/Gradle），拉取依赖 JAR 到 .library/，增量运行 LibraryHandler 入库
        if (appConfig.isDependencyAutoResolve()) {
            log.info("[Project] Dependency auto-resolve enabled, resolving dependencies...");
            resolver = resolveDependencies(appConfig);
        } else {
            log.info("[Project] Dependency auto-resolve disabled, skipping library analysis");
            // 即使不自动解析依赖，也要确保项目信息注册到 MySQL
            ensureProjectRegistered(appConfig);
        }

        // Phase 2: 清理反编译产物，避免 Spoon 扫描到 .library/output/ 中的依赖源码
        cleanDecompileOutput(Path.of(appConfig.getProjectRootPath(), ".library", "output").toString());

        // Phase 3: 分析项目源码（Spoon 解析 → 语义 pipeline → 写入 NebulaGraph/Milvus）
        log.info("[Project] Starting project source code analysis");
        handle();
        log.info("[Project] Full analysis completed");

        // Phase 3.5: 源码分析完成后释放 classIndex（IdGenerator/ExecutableProcessor 不再需要）
        appConfig.setClassToGavIndex(null);
        log.info("[Project] Released classIndex cache after source analysis");

        // Phase 4: 更新 last_analyzed_commit
        updateAnalyzedCommit(appConfig);

        // Phase 5: 分析完成后删除被过滤的 JAR（必须放在 Phase 3 之后，Spoon 分析期间需要 excluded JAR 在 classpath 上）
        if (resolver != null) {
            resolver.deleteExcludedJars();
        }
    }

    /**
     * 依赖解析 + 增量更新流程。
     *
     * 1. 解析当前依赖列表（GAV → JAR 映射），含被过滤的依赖
     * 2. 读取上次 manifest，计算差异
     * 3. 只处理新增/变更的依赖（共享 repo_id = lib:{GAV}）
     * 4. 写入全量依赖到 DB（含 is_filtered 标记）+ className → GAV 映射
     * 5. 加载 classIndex 到 AppConfig（供 IdGenerator 使用）
     * 6. 分析完成后释放 classIndex
     */
    private DependencyResolver resolveDependencies(AppConfig appConfig) {
        String projectRootPath = appConfig.getProjectRootPath();
        BuildTool buildTool = BuildToolDetector.detect(projectRootPath);
        log.info("[Project] Detected build tool: {} for project: {}", buildTool, projectRootPath);

        if (buildTool == BuildTool.UNKNOWN) {
            log.warn("[Project] Cannot detect build tool (no pom.xml or build.gradle found), skipping dependency resolution");
            return null;
        }

        // Step 1: 解析依赖并导出 JAR + GAV 映射（含被过滤的依赖和 classIndex）
        DependencyResolver resolver = new DependencyResolver(
                appConfig.getDependencyTimeoutMinutes(),
                new JdkResolver(appConfig.getDependencyJdkPaths()));
        DependencyResolveResult resolveResult = resolver.resolveWithGavFull(projectRootPath, buildTool);
        Map<String, File> currentGavToJar = resolveResult.kept();
        Map<String, File> excludedGavToJar = resolveResult.excluded();

        if (currentGavToJar.isEmpty() && excludedGavToJar.isEmpty()) {
            log.warn("[Project] No dependency JARs resolved, skipping library analysis");
            return resolver;
        }

        // Step 2: 读取上次 manifest，计算增量差异
        Map<String, String> currentDeps = new LinkedHashMap<>();
        for (String gav : currentGavToJar.keySet()) {
            currentDeps.put(gav, "runtime");
        }
        Path manifestPath = DependencyManifest.defaultManifestPath(projectRootPath);
        Map<String, String> previousDeps = DependencyManifest.load(manifestPath);
        DiffResult diff = DependencyDiff.diff(previousDeps, currentDeps);

        log.info("[Project] Dependency diff: {} added, {} removed, {} changed, {} unchanged",
                diff.getAdded().size(), diff.getRemoved().size(),
                diff.getChanged().size(), diff.getUnchanged().size());

        // Step 3: 使用 DependencyTracker 同步依赖信息（含 is_filtered）
        DependencyTracker tracker = new DependencyTracker();
        try {
            String projectId = appConfig.getProjectId();
            String branch = appConfig.getBranch();
            tracker.syncDependencies(projectId, branch, currentGavToJar, excludedGavToJar);
            tracker.syncClassIndex(resolveResult.classIndex());

            // Step 4: 加载 classIndex 到 AppConfig（供 IdGenerator 使用），排除项目内部模块
            Map<String, String> classIndex = tracker.loadClassIndex(projectId, branch, resolveResult.projectModuleGavs());
            appConfig.setClassToGavIndex(classIndex);
            log.info("[Project] Loaded {} class→GAV mappings for ID generation (excluded {} project module GAVs)",
                    classIndex.size(), resolveResult.projectModuleGavs().size());

            // classIndex 必须在 handle()（Phase 3）期间保持可用，
            // 因为 IdGenerator 和 isInClassIndex 依赖它生成正确的 lib 风格 ID 和 OUT_CALLS 边。
            // 不能在 resolveDependencies 的 finally 中释放，否则 Phase 3 拿到 null。
            try {
                // Step 5: 只处理新增和变更的依赖
                if (diff.hasChanges()) {
                    Map<String, File> jarsToProcess = new LinkedHashMap<>();
                    for (String gav : diff.getAddedAndChanged()) {
                        File jar = currentGavToJar.get(gav);
                        if (jar != null) {
                            // 尝试标记为 BUILDING，跳过已在构建/已构建的
                            String status = tracker.getLibraryGraphStatus(gav);
                            if ("BUILT".equals(status)) {
                                log.info("[Project] Library {} already built, skipping", gav);
                                continue;
                            }
                            if (tracker.tryMarkLibraryBuilding(gav)) {
                                jarsToProcess.put(gav, jar);
                            } else {
                                // 等待其他项目构建完成
                                log.info("[Project] Library {} is being built by another process, waiting...", gav);
                                tracker.waitForLibraryBuilt(gav, appConfig.getDependencyTimeoutMinutes() * 60_000L);
                            }
                        }
                    }

                    if (!jarsToProcess.isEmpty()) {
                        log.info("[Project] Processing {} new/changed dependencies", jarsToProcess.size());
                        String libraryPath = Path.of(projectRootPath, ".library").toString();
                        String decompileOutputPath = Path.of(projectRootPath, ".library", "output").toString();
                        LibraryHandler libraryHandler = new LibraryHandler(libraryPath, decompileOutputPath, jarsToProcess);
                        libraryHandler.setOnJarCompleted(result ->
                                tracker.markLibraryBuilt(result.gav(), result.nodeCount(), result.edgeCount()));
                        libraryHandler.handle();
                        log.info("[Project] Library analysis completed for {} JARs", jarsToProcess.size());
                    }

                    // 处理删除的依赖
                    if (!diff.getRemoved().isEmpty()) {
                        log.info("[Project] {} dependencies removed, uses_dependency edges may need cleanup", diff.getRemoved().size());
                    }

                    // 版本迁移：检测依赖版本变更，迁移边关系
                    if (!diff.getChanged().isEmpty()) {
                        Map<String, String> changedGavs = new LinkedHashMap<>();
                        for (String newGav : diff.getChanged()) {
                            // 从 previousDeps 中找到对应的旧 GAV
                            String oldGav = previousDeps.entrySet().stream()
                                    .filter(e -> newGav.startsWith(e.getKey().split(":")[0] + ":" + e.getKey().split(":")[1] + ":"))
                                    .map(Map.Entry::getKey)
                                    .findFirst()
                                    .orElse(null);
                            if (oldGav != null && !oldGav.equals(newGav)) {
                                changedGavs.put(oldGav, newGav);
                            }
                        }
                        if (!changedGavs.isEmpty()) {
                            boolean projectSourceChanged = detectProjectSourceChange(tracker, projectId, branch, appConfig);
                            VersionMigrator migrator = new VersionMigrator();
                            var result = migrator.migrate(projectId, branch, changedGavs, projectSourceChanged);
                            log.info("[Project] Migration completed: {} edges migrated, {} removed, strategy={}",
                                    result.migratedEdges(), result.removedEdges(), result.strategy());
                        }
                    }
                } else {
                    log.info("[Project] No dependency changes detected, skipping library analysis");
                }

                // Step 6: 创建 uses_dependency 边（项目 → 所有外部依赖，含被过滤的）
                // 排除项目内部模块（它们作为项目源码分析，不是外部依赖）
                Map<String, File> allExternalDeps = new LinkedHashMap<>(currentGavToJar);
                for (Map.Entry<String, File> entry : excludedGavToJar.entrySet()) {
                    if (!resolveResult.projectModuleGavs().contains(entry.getKey())) {
                        allExternalDeps.put(entry.getKey(), entry.getValue());
                    }
                }
                createUsesDependencyEdges(appConfig, allExternalDeps);
            } finally {
                // classIndex 不在此处释放！它在 Phase 3 handle() 之后由 run() 释放
            }
        } finally {
            tracker.close();
        }

        // Step 9: 更新 manifest
        DependencyManifest.save(manifestPath, currentDeps);

        return resolver;
    }

    /**
     * 检测项目源码是否变更（通过 git diff 对比上次分析的 commit）。
     */
    private boolean detectProjectSourceChange(DependencyTracker tracker, String projectId, String branch, AppConfig appConfig) {
        try {
            String lastCommit = tracker.getLastAnalyzedCommit(projectId, branch);
            if (lastCommit == null || lastCommit.isEmpty()) {
                // 首次分析，无法比较，保守选择全量重建
                log.info("[Project] No previous commit found, treating as source changed");
                return true;
            }

            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
            pb.directory(new File(appConfig.getProjectRootPath()));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String currentCommit = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();

            if (exitCode != 0 || currentCommit.isEmpty()) {
                log.warn("[Project] Failed to get current git commit, treating as source changed");
                return true;
            }

            if (lastCommit.equals(currentCommit)) {
                log.info("[Project] No source code change detected (commit unchanged: {})", currentCommit);
                return false;
            }

            log.info("[Project] Source code changed: {} → {}", lastCommit, currentCommit);
            return true;
        } catch (Exception e) {
            log.warn("[Project] Failed to detect source change, treating as source changed", e);
            return true;
        }
    }

    /**
     * 为项目的每个依赖创建 uses_dependency 边。
     * 边从项目的 "project:{projectId}" 虚拟节点指向依赖库的代表性节点（lib:{GAV} 的 file 节点）。
     * 先确保虚拟节点存在，再插入边。
     */
    private void createUsesDependencyEdges(AppConfig appConfig, Map<String, File> gavToJar) {
        String projectId = appConfig.getProjectId();
        String branch = appConfig.getBranch();
        String projectNodeId = IdGenerator.generate(IdGenerator.builder()
                .fullQualifiedName("project:" + projectId + "#" + branch)
                .isShadow(true)
                .build());

        List<Edge> edges = new ArrayList<>();
        List<Node> shadowNodes = new ArrayList<>();

        // 项目虚拟节点
        shadowNodes.add(FileNode.builder()
                .id(projectNodeId)
                .fullName("project:" + projectId + "#" + branch)
                .nodeType(NodeType.FILE)
                .repoId(projectId)
                .branchName(branch)
                .isLibrary(false)
                .build());

        for (Map.Entry<String, File> entry : gavToJar.entrySet()) {
            String gav = entry.getKey();
            String[] parts = gav.split(":");
            if (parts.length < 3) continue;
            String version = parts[2];

            String depNodeId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName("lib:" + gav)
                    .isShadow(true)
                    .build());

            // 依赖库虚拟节点
            shadowNodes.add(FileNode.builder()
                    .id(depNodeId)
                    .fullName("lib:" + gav)
                    .nodeType(NodeType.FILE)
                    .repoId("lib:" + gav)
                    .isLibrary(true)
                    .build());

            edges.add(Edge.builder()
                    .srcId(projectNodeId)
                    .dstId(depNodeId)
                    .type(EdgeType.USES_DEPENDENCY)
                    .property("project_id", projectId)
                    .property("scope", "runtime")
                    .property("version", version)
                    .build());
        }

        if (edges.isEmpty()) return;

        try (GraphStorageRepository repo = GraphStorageRepositoryFactory.create(appConfig)) {
            repo.batchInsertNodes(shadowNodes);
            repo.batchInsertEdges(edges);
            log.info("[Project] Created {} uses_dependency edges for project {}", edges.size(), projectId);
        } catch (Exception e) {
            log.warn("[Project] Failed to create uses_dependency edges", e);
        }
    }

    @Override
    GraphContext initGraphContext(GraphContext.GraphContextBuilder builder) {
        AppConfig.getInstance().setParseType(ParseType.PROJECT);
        return builder.parseType(ParseType.PROJECT).build();
    }

    /**
     * 清理反编译输出目录。该目录包含依赖库的反编译 .java 文件，
     * 若不清理，Spoon 会将它们当作项目源码处理（ParseType.PROJECT），导致依赖代码误入 Milvus。
     * LibraryHandler 每次运行时会重建该目录，因此清理是安全的。
     */
    private void cleanDecompileOutput(String outputDir) {
        Path path = Path.of(outputDir);
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
            log.info("[Project] Cleaned decompile output: {}", outputDir);
        } catch (IOException e) {
            log.warn("[Project] Failed to clean decompile output: {}", outputDir, e);
        }
    }

    @Override
    void initLauncher(Launcher launcher) {
        AppConfig appConfig = AppConfig.getInstance();
        launcher.addInputResource(Paths.get(appConfig.getProjectRootPath()).toString());
    }

    /**
     * 确保项目信息注册到 MySQL（dependencyAutoResolve=false 时的兜底）。
     */
    private void ensureProjectRegistered(AppConfig appConfig) {
        try (DependencyTracker tracker = new DependencyTracker()) {
            tracker.ensureProjectRegistered(appConfig.getProjectId(), appConfig.getBranch());
        } catch (Exception e) {
            log.warn("[Project] Failed to register project info in database", e);
        }
    }

    /**
     * 项目源码分析完成后，更新 last_analyzed_commit。
     */
    private void updateAnalyzedCommit(AppConfig appConfig) {
        try {
            String currentCommit = getCurrentGitCommit(appConfig.getProjectRootPath());
            if (currentCommit == null) {
                log.warn("[Project] Failed to get current git commit, skipping commit update");
                return;
            }
            try (DependencyTracker tracker = new DependencyTracker()) {
                tracker.updateLastAnalyzedCommit(appConfig.getProjectId(), appConfig.getBranch(), currentCommit);
                log.info("[Project] Updated last_analyzed_commit: {}", currentCommit);
            }
        } catch (Exception e) {
            log.warn("[Project] Failed to update last_analyzed_commit", e);
        }
    }

    private String getCurrentGitCommit(String projectRootPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
            pb.directory(new File(projectRootPath));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();
            return exitCode == 0 && !output.isEmpty() ? output : null;
        } catch (Exception e) {
            return null;
        }
    }
}
