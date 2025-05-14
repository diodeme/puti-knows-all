package com.puti.code.app.pipeline;

import org.junit.jupiter.api.Test;
import spoon.Launcher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSemanticPipelineTest {

    @Test
    void shouldExecuteStagesInRegistrationOrder() {
        List<String> executed = new ArrayList<>();
        JavaSemanticPipeline pipeline = new JavaSemanticPipeline()
                .addStage(new TestStage("stage-1", executed))
                .addStage(new TestStage("stage-2", executed));

        pipeline.execute(new Launcher().getFactory());

        assertEquals(List.of("stage-1", "stage-2"), executed);
    }

    private record TestStage(String name, List<String> executed) implements JavaAnalysisStage {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public void execute(spoon.reflect.factory.Factory factory) {
            executed.add(name);
        }
    }
}
