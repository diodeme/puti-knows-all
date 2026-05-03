package com.puti.code.app.handler;

import com.puti.code.ai.vector.VectorGenerator;
import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.app.pipeline.DefaultJavaSemanticPipelineFactory;
import com.puti.code.app.pipeline.JavaSemanticPipeline;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.enums.ParseType;
import com.puti.code.repository.graph.GraphStorageRepository;
import com.puti.code.repository.graph.GraphStorageRepositoryFactory;
import com.puti.code.repository.milvus.GraphVectorMilvusClient;
import lombok.extern.slf4j.Slf4j;
import spoon.Launcher;
import com.puti.code.app.decompiler.CFRDecompiler;
import spoon.reflect.factory.Factory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 依赖库代码分析处理器。
 * 可独立运行（使用 AppConfig 默认路径），也可被 ProjectHandler 调用（使用自定义路径）。
 * 按 JAR 逐个处理，单个 JAR 反编译/解析失败不影响其他 JAR。
 *
 * 共享依赖图谱支持：通过 gavToJarMap 传入 GAV → JAR 映射，每个 JAR 处理时
 * 临时将 AppConfig.projectId 设置为 "lib:{GAV}"，使所有节点和边使用共享 repo_id。
 */
@Slf4j
public class LibraryHandler extends AbstractHandler {

    /** 单个 JAR 处理完成后的回调。参数：{gav, nodeCount, edgeCount} */
    public record JarResult(String gav, int nodeCount, int edgeCount) {}

    private static final String LIB_REPO_ID_PREFIX = "lib:";

    private final String libraryPath;
    private final String decompileOutputPath;
    /** GAV → JAR 文件名映射。非空时按映射处理每个 JAR 并设置共享 repo_id。 */
    private Map<String, File> gavToJarMap;
    /** 每个 GAV 的 [nodeCount, edgeCount] 统计，handle() 执行后可读取。 */
    private Map<String, int[]> gavStats = new LinkedHashMap<>();
    /** 单个 JAR 处理完成后的回调，用于逐个更新依赖状态。 */
    private Consumer<JarResult> onJarCompleted;

    public LibraryHandler() {
        this.libraryPath = null;
        this.decompileOutputPath = null;
    }

    /**
     * 使用自定义库路径和反编译输出路径构造。
     * 用于 ProjectHandler 自动解析依赖后，指定项目级 .library/ 路径。
     */
    public LibraryHandler(String libraryPath, String decompileOutputPath) {
        this.libraryPath = libraryPath;
        this.decompileOutputPath = decompileOutputPath;
    }

    /**
     * 使用 GAV → JAR 映射构造，用于共享依赖图谱。
     * 每个 JAR 使用 "lib:{GAV}" 作为 repo_id。
     */
    public LibraryHandler(String libraryPath, String decompileOutputPath, Map<String, File> gavToJarMap) {
        this.libraryPath = libraryPath;
        this.decompileOutputPath = decompileOutputPath;
        this.gavToJarMap = gavToJarMap;
    }

    public void setOnJarCompleted(Consumer<JarResult> onJarCompleted) {
        this.onJarCompleted = onJarCompleted;
    }

    public Map<String, int[]> getGavStats() {
        return gavStats;
    }

    public static void main(String[] args) {
        LibraryHandler libraryHandler = new LibraryHandler();
        libraryHandler.handle();
    }

    /**
     * 依赖库分析入口。重写父类 handle()，实现按 JAR 逐个处理以隔离错误。
     *
     * 流程：扫描 JAR 目录 → 逐个处理（CFR 反编译 → 清理合成文件 → Spoon 建模 → pipeline 入库）
     * 单个 JAR 失败不影响其他 JAR 的分析。
     *
     * 当 gavToJarMap 不为空时，进入共享依赖图谱模式：
     * 只处理 map 中指定的 JAR，处理前临时将 projectId 设为 "lib:{GAV}"。
     */
    @Override
    public void handle() {
        // 无参构造时使用 AppConfig 全局路径，有参构造时使用 ProjectHandler 传入的项目级路径
        String libPath = libraryPath != null ? libraryPath : AppConfig.getInstance().getGlobalLibraryPath();
        String decompileBasePath = decompileOutputPath != null ? decompileOutputPath : AppConfig.getInstance().getGlobalDecompileOutputPath();
        log.info("[Library] Starting library analysis, libPath={}, decompileOutput={}", libPath, decompileBasePath);

        // 确定 JAR 处理列表：有 gavToJarMap 时按映射处理，否则扫描目录下所有 JAR
        Map<String, File> jarEntries = gavToJarMap;
        if (jarEntries == null || jarEntries.isEmpty()) {
            // 兼容旧模式：扫描目录所有 JAR，使用默认 projectId
            File libDir = new File(libPath);
            File[] jars = libDir.listFiles((d, name) -> name.endsWith(".jar"));
            if (jars == null || jars.length == 0) {
                log.warn("[Library] No JAR files found in {}, aborting", libPath);
                return;
            }
            jarEntries = new LinkedHashMap<>();
            for (File jar : jars) {
                jarEntries.put(jar.getName(), jar);
            }
        }
        log.info("[Library] {} JAR files to process (shared mode: {})",
                jarEntries.size(), gavToJarMap != null && !gavToJarMap.isEmpty());

        AppConfig appConfig = AppConfig.getInstance();
        String originalProjectId = appConfig.getProjectId();
        gavStats.clear();
        try (GraphStorageRepository rawRepo = GraphStorageRepositoryFactory.create(appConfig)) {
            CountingGraphStorageRepository graphStorageRepository = new CountingGraphStorageRepository(rawRepo);
            log.info("[Library] Connected to graph storage: {}", appConfig.getGraphStorageType());
            GraphVectorMilvusClient graphVectorMilvusClient = null;
            VectorGenerator vectorGenerator = null;
            try {
                if (appConfig.isGraphVectorEnabled()) {
                    graphVectorMilvusClient = new GraphVectorMilvusClient();
                    vectorGenerator = new VectorGenerator();
                    log.info("[Library] Vector support enabled (Milvus + embedding)");
                } else {
                    log.info("[Library] Vector support disabled");
                }

                GraphContext graphContext = GraphContext.builder()
                        .graphStorageRepository(graphStorageRepository)
                        .graphVectorMilvusClient(graphVectorMilvusClient)
                        .vectorGenerator(vectorGenerator)
                        .parseType(ParseType.LIBRARY)
                        .build();
                appConfig.setParseType(ParseType.LIBRARY);

                // 按 JAR 逐个处理：反编译 → 清理 → Spoon 建模 → pipeline 分析入库
                int success = 0, skipped = 0;
                int index = 0;
                int total = jarEntries.size();
                for (Map.Entry<String, File> entry : jarEntries.entrySet()) {
                    index++;
                    String key = entry.getKey();   // GAV（共享模式）或 JAR 文件名（兼容模式）
                    File jar = entry.getValue();
                    log.info("[Library] Processing JAR {}/{}: {} ({})", index, total, jar.getName(), key);

                    // 共享依赖图谱模式：临时设置 projectId 为 lib:{GAV}
                    String repoId = resolveRepoId(key);
                    appConfig.setProjectId(repoId);
                    log.debug("[Library] Set projectId to {} for JAR {}", repoId, jar.getName());

                    graphStorageRepository.reset();
                    if (processSingleJar(jar, decompileBasePath, graphContext, appConfig)) {
                        int nodeCount = graphStorageRepository.getNodeCount();
                        int edgeCount = graphStorageRepository.getEdgeCount();
                        gavStats.put(key, new int[]{nodeCount, edgeCount});
                        if (onJarCompleted != null) {
                            onJarCompleted.accept(new JarResult(key, nodeCount, edgeCount));
                        }
                        success++;
                    } else {
                        if (onJarCompleted != null) {
                            onJarCompleted.accept(new JarResult(key, 0, 0));
                        }
                        skipped++;
                    }
                }
                log.info("[Library] Analysis complete: {} succeeded, {} skipped out of {} total", success, skipped, total);
            } finally {
                // 恢复原始 projectId
                appConfig.setProjectId(originalProjectId);
                if (vectorGenerator != null) {
                    vectorGenerator.close();
                }
                if (graphVectorMilvusClient != null) {
                    graphVectorMilvusClient.close();
                }
            }
        } catch (Exception e) {
            log.error("[Library] Fatal error during library analysis", e);
        }
    }

    /**
     * 根据入口 key 确定 repo_id。
     * 共享模式（gavToJarMap 非空）时使用 "lib:{GAV}"，兼容模式保持原始 projectId。
     */
    private String resolveRepoId(String key) {
        if (gavToJarMap != null && !gavToJarMap.isEmpty()) {
            return LIB_REPO_ID_PREFIX + key;
        }
        return AppConfig.getInstance().getProjectId();
    }

    /**
     * 处理单个 JAR 文件的完整流程。
     *
     * 1. CFR 反编译 JAR → .java 源文件到独立子目录
     * 2. 清理 package-info.java / module-info.java（无分析价值且会导致 Spoon 语法错误）
     * 3. Spoon 解析 .java 文件构建 AST 模型（使用普通文件源码而非 DecompiledResource，
     *    避免 NoSourcePosition → DeclarationSourcePosition 的 ClassCastException）
     * 4. 语义分析 pipeline 提取节点和关系，写入 NebulaGraph / Milvus
     *
     * @return true 处理成功，false 处理失败（已跳过）
     */
    private boolean processSingleJar(File jar, String decompileBasePath, GraphContext graphContext, AppConfig appConfig) {
        String jarOutputDir = decompileBasePath + "/" + jar.getName().replace(".jar", "");
        try {
            Files.createDirectories(Path.of(jarOutputDir));

            // Step 1: CFR 反编译 JAR → .java 源文件
            log.debug("[Library] Decompiling {} to {}", jar.getName(), jarOutputDir);
            new CFRDecompiler().decompile(jar.getAbsolutePath(), jarOutputDir, new String[]{});

            // Step 2: 清理无分析价值的合成文件（package-info/module-info 会导致 Spoon 语法错误）
            cleanDecompiledSources(jarOutputDir);

            // Step 3: Spoon 解析反编译后的 .java 文件，构建 AST 模型
            Launcher launcher = new Launcher();
            launcher.addInputResource(jarOutputDir);
            configureSpoonEnvironment(launcher, appConfig);
            log.debug("[Library] Building Spoon model for {}", jar.getName());
            launcher.buildModel();

            // Step 4: 语义分析 pipeline（节点提取、关系构建、写入 NebulaGraph/Milvus）
            Factory factory = launcher.getFactory();
            JavaSemanticPipeline pipeline = new DefaultJavaSemanticPipelineFactory().create(graphContext);
            pipeline.execute(factory);

            log.info("[Library] Successfully processed JAR: {}", jar.getName());
            return true;
        } catch (ClassCastException e) {
            log.warn("[Library] Skipped JAR {} - Spoon NoSourcePosition bug in decompiled code: {}", jar.getName(), e.getMessage());
            return false;
        } catch (Throwable e) {
            log.warn("[Library] Skipped JAR {} - {}: {}", jar.getName(), e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * 清理反编译输出中的合成文件。
     * package-info.java 和 module-info.java 对代码图谱分析无价值，
     * 且 CFR 反编译后常有语法错误，会导致 Spoon 的 JDT 编译器报警。
     */
    private void cleanDecompiledSources(String outputDir) {
        try (Stream<Path> walk = Files.walk(Path.of(outputDir))) {
            List<Path> toDelete = walk.filter(p -> {
                String name = p.getFileName().toString();
                return name.equals("package-info.java") || name.equals("module-info.java");
            }).toList();
            if (!toDelete.isEmpty()) {
                log.debug("[Library] Cleaning {} synthetic files from {}", toDelete.size(), outputDir);
                for (Path p : toDelete) {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception e) {
                        log.warn("[Library] Failed to delete {}", p);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[Library] Failed to scan decompiled sources for cleanup: {}", e.getMessage());
        }
    }

    @Override
    GraphContext initGraphContext(GraphContext.GraphContextBuilder builder) {
        return builder.parseType(ParseType.LIBRARY).build();
    }

    @Override
    void initLauncher(Launcher launcher) {
        // LibraryHandler 重写了 handle()，不再使用 initLauncher
    }
}
