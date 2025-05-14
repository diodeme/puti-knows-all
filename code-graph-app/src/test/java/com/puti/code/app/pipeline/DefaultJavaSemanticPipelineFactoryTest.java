package com.puti.code.app.pipeline;

import com.puti.code.analyzer.java.context.GraphContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultJavaSemanticPipelineFactoryTest {

    @Test
    void shouldRegisterDefaultStagesOutsideMainHandlerFlow() {
        GraphContext graphContext = GraphContext.builder().build();

        JavaSemanticPipeline pipeline = new DefaultJavaSemanticPipelineFactory().create(graphContext);

        assertEquals(
                java.util.List.of("fact-extract", "semantic-enhance", "data-flow-enhance"),
                pipeline.getStageNames());
    }
}
