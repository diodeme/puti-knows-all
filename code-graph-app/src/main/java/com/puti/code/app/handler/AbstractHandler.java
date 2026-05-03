package com.puti.code.app.handler;

import com.puti.code.ai.vector.VectorGenerator;
import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.app.pipeline.DefaultJavaSemanticPipelineFactory;
import com.puti.code.app.pipeline.JavaSemanticPipeline;
import com.puti.code.base.config.AppConfig;
import com.puti.code.analyzer.java.rule.SpoonEntryPointRuleEngine;
import com.puti.code.base.enums.RuleEngineType;
import com.puti.code.base.util.FileTool;
import com.puti.code.repository.graph.GraphStorageRepository;
import com.puti.code.repository.graph.GraphStorageRepositoryFactory;
import com.puti.code.repository.milvus.GraphVectorMilvusClient;
import com.puti.code.app.dependency.BuildToolDetector;
import com.puti.code.app.dependency.JdkResolver;
import com.puti.code.rule.factory.RuleEngineFactory;
import lombok.extern.slf4j.Slf4j;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.reflect.factory.Factory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public abstract class AbstractHandler {
    private static boolean ruleEngineInitialized = false;

    public AbstractHandler(){
        initializeRuleEngine();
    }

    /**
     * 初始化规则引擎（只初始化一次）
     */
    private synchronized void initializeRuleEngine() {
        if (!ruleEngineInitialized) {
            RuleEngineFactory factory = RuleEngineFactory.getInstance();
            if (!factory.hasEngine(RuleEngineType.JAVA_ENTRY_POINT)) {
                SpoonEntryPointRuleEngine entryPointEngine = new SpoonEntryPointRuleEngine();
                factory.registerEngine(entryPointEngine);
                log.info("Registered java entry point rule engine");
            }
            ruleEngineInitialized = true;
        }
    }

    public void handle() {
        log.info("Starting Java Graph Builder");

        // 初始化客户端
        AppConfig appConfig = AppConfig.getInstance();
        try (GraphStorageRepository graphStorageRepository = GraphStorageRepositoryFactory.create(appConfig)) {
            log.info("Using graph storage backend: {}", appConfig.getGraphStorageType());
            GraphVectorMilvusClient graphVectorMilvusClient = null;
            VectorGenerator vectorGenerator = null;
            try {
                if (appConfig.isGraphVectorEnabled()) {
                    graphVectorMilvusClient = new GraphVectorMilvusClient();
                    vectorGenerator = new VectorGenerator();
                    log.info("Graph vector support enabled");
                } else {
                    log.info("Graph vector support disabled; skipping Milvus and embedding initialization");
                }

                GraphContext.GraphContextBuilder graphContextBuilder = GraphContext.builder()
                        .graphStorageRepository(graphStorageRepository)
                        .graphVectorMilvusClient(graphVectorMilvusClient)
                        .vectorGenerator(vectorGenerator);

                GraphContext graphContext = initGraphContext(graphContextBuilder);

                // 初始化Spoon
                Launcher launcher = new Launcher();
                initLauncher(launcher);
                configureSpoonEnvironment(launcher, appConfig);

                // 构建模型
                launcher.buildModel();
                Factory factory = launcher.getFactory();
                JavaSemanticPipeline pipeline = new DefaultJavaSemanticPipelineFactory().create(graphContext);

                log.info("Processing Java code with semantic pipeline...");
                pipeline.execute(factory);
                log.info("Java Graph Builder completed successfully");
            } finally {
                if (vectorGenerator != null) {
                    vectorGenerator.close();
                }
                if (graphVectorMilvusClient != null) {
                    graphVectorMilvusClient.close();
                }
            }
        } catch (Exception e) {
            log.error("Failed to build Java Graph", e);
        }
    }

    abstract GraphContext initGraphContext(GraphContext.GraphContextBuilder graphContextBuilder);

    abstract void initLauncher(Launcher launcher);

    protected void configureSpoonEnvironment(Launcher launcher, AppConfig appConfig) {
        Environment environment = launcher.getEnvironment();
        environment.setAutoImports(true);
        environment.setCommentEnabled(true);
        environment.setIgnoreDuplicateDeclarations(true);
        environment.setIgnoreSyntaxErrors(true);
        // 启用 noClasspath 模式：当 classpath 缺少依赖 JAR 时（如 Spring Web 被 exclude），
        // Spoon 会从 import 语句推断注解 FQN，而非错误地将注解解析为声明类的包路径。
        // 不开启时，@RequestMapping 会被解析为 com.xxx.controller.RequestMapping 而非
        // org.springframework.web.bind.annotation.RequestMapping，导致入口点规则失效。
        environment.setNoClasspath(true);
        log.info("Spoon noClasspath mode enabled (resolves annotation FQNs from imports when classpath is incomplete)");

        int complianceLevel = detectComplianceLevel(appConfig);
        environment.setComplianceLevel(complianceLevel);
        log.info("Spoon compliance level set to {} (project: {})", complianceLevel, appConfig.getProjectRootPath());

        // 合并全局库路径和项目级 .library/ 路径到 classpath，用于 Spoon 解析类型引用
        // inputClassLoader 方式构建影子模型时，类 A 的依赖 B、C 等也必须都在 classLoader 里，
        // 因此优先使用 SourceClasspath（类加载器会有重复类定义的问题）
        String globalLibraryPath = appConfig.getGlobalLibraryPath();
        String projectLibraryPath = appConfig.getProjectRootPath() + "/.library";
        List<URL> allJarUrls = new ArrayList<>();
        allJarUrls.addAll(Arrays.asList(FileTool.getAllJarUrls(globalLibraryPath)));
        allJarUrls.addAll(Arrays.asList(FileTool.getAllJarUrls(projectLibraryPath)));
        environment.setSourceClasspath(allJarUrls.stream().map(URL::getPath).toArray(String[]::new));
        log.info("Spoon source classpath: {} jars (global: {}, project: {})", allJarUrls.size(), globalLibraryPath, projectLibraryPath);
    }

    /**
     * 从项目构建文件（pom.xml / build.gradle）检测 Java 版本，设置 Spoon 的合规级别。
     * 检测不到时回退到 Java 17。
     */
    private int detectComplianceLevel(AppConfig appConfig) {
        String projectRootPath = appConfig.getProjectRootPath();
        BuildToolDetector.BuildTool buildTool = BuildToolDetector.detect(projectRootPath);
        JdkResolver jdkResolver = new JdkResolver(appConfig.getDependencyJdkPaths());
        int version = jdkResolver.detectRequiredVersion(projectRootPath, buildTool);
        if (version > 0) {
            log.info("Detected project Java version: {}, setting Spoon compliance level to {}", version, version);
            return version;
        }
        log.info("Could not detect project Java version, using default compliance level 17");
        return 17;
    }
}
