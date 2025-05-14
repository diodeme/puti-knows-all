package com.puti.code.app.pipeline;

import spoon.processing.ProcessingManager;
import spoon.processing.Processor;
import spoon.reflect.factory.Factory;
import spoon.support.QueueProcessingManager;

import java.util.List;

public class ProcessorStage implements JavaAnalysisStage {
    private final String name;
    private final List<Processor<?>> processors;
    private final boolean processCompilationUnits;
    private final boolean processTypes;

    public ProcessorStage(String name, List<Processor<?>> processors) {
        this(name, processors, true, true);
    }

    public ProcessorStage(String name, List<Processor<?>> processors,
                          boolean processCompilationUnits, boolean processTypes) {
        this.name = name;
        this.processors = processors;
        this.processCompilationUnits = processCompilationUnits;
        this.processTypes = processTypes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void execute(Factory factory) {
        ProcessingManager processingManager = new QueueProcessingManager(factory);
        for (Processor<?> processor : processors) {
            processingManager.addProcessor(processor);
        }
        if (processCompilationUnits) {
            processingManager.process(factory.CompilationUnit().getMap().values());
        }
        if (processTypes) {
            processingManager.process(factory.getModel().getAllTypes());
        }
    }
}
