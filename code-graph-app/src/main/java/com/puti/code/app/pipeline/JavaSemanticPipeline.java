package com.puti.code.app.pipeline;

import lombok.extern.slf4j.Slf4j;
import spoon.reflect.factory.Factory;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JavaSemanticPipeline {
    private final List<JavaAnalysisStage> stages = new ArrayList<>();

    public JavaSemanticPipeline addStage(JavaAnalysisStage stage) {
        stages.add(stage);
        return this;
    }

    public void execute(Factory factory) {
        for (JavaAnalysisStage stage : stages) {
            log.info("Executing analysis stage: {}", stage.getName());
            stage.execute(factory);
        }
    }

    public List<String> getStageNames() {
        return stages.stream()
                .map(JavaAnalysisStage::getName)
                .toList();
    }
}
