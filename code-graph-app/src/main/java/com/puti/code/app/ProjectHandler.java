package com.puti.code.app;


import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.app.dependency.BuildToolDetector;
import com.puti.code.app.dependency.BuildToolDetector.BuildTool;
import com.puti.code.app.dependency.DependencyDiff;
import com.puti.code.app.dependency.DependencyDiff.DiffResult;
import com.puti.code.app.dependency.DependencyManifest;
import com.puti.code.app.dependency.DependencyResolver;
import com.puti.code.app.dependency.JdkResolver;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.enums.ParseType;
import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeType;
import com.puti.code.base.util.IdGenerator;
import com.puti.code.repository.graph.GraphStorageRepository;
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

        // Phase 1: 自动检测构建工具（Maven/Gradle），拉取依赖 JAR 到 .library/，增量运行 LibraryHandler 入库
        if (appConfig.isDependencyAutoResolve()) {
            log.info("[Project] Dependency auto-resolve enabled, resolving dependencies...");
            resolveDependencies(appConfig);
        } else {
            log.info("[Project] Dependency auto-resolve disabled, skipping library analysis");
        }

        // Phase 2: 清理反编译产物，避免 Spoon 扫描到 .library/output/ 中的依赖源码
        cleanDecompileOutput(appConfig.getProjectRootPath() + "/.library/output");

        // Phase 3: 分析项目源码（Spoon 解析 → 语义 pipeline → 写入 NebulaGraph/Milvus）
        log.info("[Project] Starting project source code analysis");
        handle();
        log.info("[Project] Full analysis completed");
    }

    /**
     * 依赖解析 + 增量更新流程。
     *
     * 1. 解析当前依赖列表（GAV → JAR 映射）
     * 2. 读取上次 manifest，计算差异
     * 3. 只处理新增/变更的依赖（共享 repo_id = lib:{GAV}）
     * 4. 更新 manifest
     */
    private void resolveDependencies(AppConfig appConfig) {
        String projectRootPath = appConfig.getProjectRootPath();
        BuildTool buildTool = BuildToolDetector.detect(projectRootPath);
        log.info("[Project] Detected build tool: {} for project: {}", buildTool, projectRootPath);

        if (buildTool == BuildTool.UNKNOWN) {
            log.warn("[Project] Cannot detect build tool (no pom.xml or build.gradle found), skipping dependency resolution");
            return;
        }

        // Step 1: 解析依赖并导出 JAR + GAV 映射
        DependencyResolver resolver = new DependencyResolver(
                appConfig.getDependencyTimeoutMinutes(),
                new JdkResolver(appConfig.getDependencyJdkPaths()));
        Map<String, File> currentGavToJar = resolver.resolveWithGav(projectRootPath, buildTool);

        if (currentGavToJar.isEmpty()) {
            log.warn("[Project] No dependency JARs resolved, skipping library analysis");
            return;
        }

        // 构建当前 GAV → scope 映射（全为 runtime scope）
        Map<String, String> currentDeps = new LinkedHashMap<>();
        for (String gav : currentGavToJar.keySet()) {
            currentDeps.put(gav, "runtime");
        }

        // Step 2: 读取上次 manifest，计算增量差异
        Path manifestPath = DependencyManifest.defaultManifestPath(projectRootPath);
        Map<String, String> previousDeps = DependencyManifest.load(manifestPath);
        DiffResult diff = DependencyDiff.diff(previousDeps, currentDeps);

        log.info("[Project] Dependency diff: {} added, {} removed, {} changed, {} unchanged",
                diff.getAdded().size(), diff.getRemoved().size(),
                diff.getChanged().size(), diff.getUnchanged().size());

        // Step 3: 只处理新增和变更的依赖
        if (diff.hasChanges()) {
            Map<String, File> jarsToProcess = new LinkedHashMap<>();
            for (String gav : diff.getAddedAndChanged()) {
                File jar = currentGavToJar.get(gav);
                if (jar != null) {
                    jarsToProcess.put(gav, jar);
                }
            }

            if (!jarsToProcess.isEmpty()) {
                log.info("[Project] Processing {} new/changed dependencies", jarsToProcess.size());
                String libraryPath = projectRootPath + "/.library";
                String decompileOutputPath = libraryPath + "/output";
                LibraryHandler libraryHandler = new LibraryHandler(libraryPath, decompileOutputPath, jarsToProcess);
                libraryHandler.handle();
                log.info("[Project] Library analysis completed for {} JARs", jarsToProcess.size());
            }

            // TODO: 处理删除的依赖 - 删除 uses_dependency 边（需 GraphStorageRepository 支持）
            if (!diff.getRemoved().isEmpty()) {
                log.info("[Project] {} dependencies removed, uses_dependency edges may need cleanup", diff.getRemoved().size());
            }
        } else {
            log.info("[Project] No dependency changes detected, skipping library analysis");
        }

        // Step 4: 创建 uses_dependency 边（项目 → 依赖）
        createUsesDependencyEdges(appConfig, currentGavToJar);

        // Step 5: 更新 manifest
        DependencyManifest.save(manifestPath, currentDeps);
    }

    /**
     * 为项目的每个依赖创建 uses_dependency 边。
     * 边从项目的 "project:{projectId}" 虚拟节点指向依赖库的代表性节点（lib:{GAV} 的 file 节点）。
     * 如果依赖的 file 节点不存在（被过滤或解析失败），则跳过该边。
     */
    private void createUsesDependencyEdges(AppConfig appConfig, Map<String, File> gavToJar) {
        String projectId = appConfig.getProjectId();
        String branch = appConfig.getBranch();
        String projectNodeId = IdGenerator.generate(IdGenerator.builder()
                .fullQualifiedName("project:" + projectId)
                .isShadow(true)
                .build());

        List<Edge> edges = new ArrayList<>();
        for (Map.Entry<String, File> entry : gavToJar.entrySet()) {
            String gav = entry.getKey();
            String[] parts = gav.split(":");
            if (parts.length < 3) continue;
            String version = parts[2];

            // 依赖库的 file 节点 ID（与 LibraryHandler 中 IdGenerator 使用相同逻辑）
            String depNodeId = IdGenerator.generate(IdGenerator.builder()
                    .fullQualifiedName("lib:" + gav)
                    .isShadow(true)
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
}