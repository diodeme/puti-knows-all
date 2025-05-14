package com.puti.code.app.pipeline;

import spoon.reflect.factory.Factory;

public interface JavaAnalysisStage {
    String getName();

    void execute(Factory factory);
}
