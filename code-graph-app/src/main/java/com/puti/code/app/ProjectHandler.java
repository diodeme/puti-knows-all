package com.puti.code.app;


import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.enums.ParseType;
import lombok.extern.slf4j.Slf4j;
import spoon.Launcher;

import java.nio.file.Paths;

/**
 * Java代码知识图谱构建工具主类
 */
@Slf4j
public class ProjectHandler extends AbstractHandler {

    public static void main(String[] args) {
        ProjectHandler projectHandler = new ProjectHandler();
        projectHandler.handle();
    }

    @Override
    GraphContext initGraphContext(GraphContext.GraphContextBuilder builder) {
        AppConfig.getInstance().setParseType(ParseType.PROJECT);
        return builder.parseType(ParseType.PROJECT).build();
    }

    @Override
    void initLauncher(Launcher launcher) {
        AppConfig appConfig = AppConfig.getInstance();
        launcher.addInputResource(Paths.get(appConfig.getProjectRootPath()).toString());
    }
}