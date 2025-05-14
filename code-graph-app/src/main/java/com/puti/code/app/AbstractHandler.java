package com.puti.code.app;

import com.puti.code.ai.vector.VectorGenerator;
import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.analyzer.java.rule.SpoonEntryPointRuleEngine;
import com.puti.code.app.pipeline.DefaultJavaSemanticPipelineFactory;
import com.puti.code.app.pipeline.JavaSemanticPipeline;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.enums.RuleEngineType;
import com.puti.code.base.util.FileTool;
import com.puti.code.repository.graph.GraphStorageRepository;
import com.puti.code.repository.graph.GraphStorageRepositoryFactory;
import com.puti.code.repository.milvus.GraphVectorMilvusClient;
import com.puti.code.rule.factory.RuleEngineFactory;
import lombok.extern.slf4j.Slf4j;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.reflect.factory.Factory;

import java.net.URL;
import java.util.Arrays;

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

                // 配置Spoon环境
                Environment environment = launcher.getEnvironment();
                environment.setAutoImports(true);
                environment.setCommentEnabled(true);
                environment.setComplianceLevel(17); // Java 17
                environment.setIgnoreDuplicateDeclarations(true);
                environment.setIgnoreSyntaxErrors(true);
                // 获取全局库路径
                String globalLibraryPath = appConfig.getGlobalLibraryPath();
                URL[] jarPaths = FileTool.getAllJarUrls(globalLibraryPath);
//            inputClassLoader要构建影子模型，从classLoader中获取类A时，类A的依赖（B、C等）必须也都在classLoader里
                //优先使用SourceClasspath，类加载器会有重复类定义的问题
//            environment.setInputClassLoader(new URLClassLoader(jarPaths, null));
//            environment.setNoClasspath(false);
                environment.setSourceClasspath(Arrays.stream(jarPaths).map(URL::getPath).toArray(String[]::new));
                log.info("Added {} jar files to source classpath", jarPaths.length);

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
}
