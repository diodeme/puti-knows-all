package com.puti.code.app.pipeline;

import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.analyzer.java.processor.AnnotationProcessor;
import com.puti.code.analyzer.java.processor.ClassProcessor;
import com.puti.code.analyzer.java.processor.ConstructorProcessor;
import com.puti.code.analyzer.java.processor.DataLineageProcessor;
import com.puti.code.analyzer.java.processor.DependencyInjectionProcessor;
import com.puti.code.analyzer.java.processor.DependencyInjectionResolutionSupport;
import com.puti.code.analyzer.java.processor.FileProcessor;
import com.puti.code.analyzer.java.processor.MethodProcessor;

import java.util.List;

/**
 * 默认 Java 语义 pipeline 注册。
 */
public class DefaultJavaSemanticPipelineFactory {

    public JavaSemanticPipeline create(GraphContext graphContext) {
        DependencyInjectionResolutionSupport dependencyInjectionSupport = new DependencyInjectionResolutionSupport();
        return new JavaSemanticPipeline()
                .addStage(new ProcessorStage("fact-extract", List.of(
                        new FileProcessor(graphContext),
                        new ClassProcessor(graphContext, dependencyInjectionSupport),
                        new DependencyInjectionProcessor(graphContext, dependencyInjectionSupport)
                )))
                .addStage(new ProcessorStage("semantic-enhance", List.of(
                        new MethodProcessor(graphContext, dependencyInjectionSupport),
                        new ConstructorProcessor(graphContext, dependencyInjectionSupport),
                        new AnnotationProcessor(graphContext)
                )))
                .addStage(new ProcessorStage("data-flow-enhance", List.of(
                        new DataLineageProcessor(graphContext)
                ), false, true));
    }
}
