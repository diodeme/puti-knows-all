package com.puti.code.app;

import com.puti.code.analyzer.java.context.GraphContext;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.enums.ParseType;
import lombok.extern.slf4j.Slf4j;
import spoon.DecompiledResource;
import spoon.Launcher;
import spoon.decompiler.CFRDecompiler;

/**
 * Java代码知识图谱构建工具主类
 */
@Slf4j
public class LibraryHandler extends AbstractHandler {

    public static void main(String[] args) {
        LibraryHandler libraryHandler = new LibraryHandler();
        libraryHandler.handle();
    }

    @Override
    GraphContext initGraphContext(GraphContext.GraphContextBuilder builder) {
        AppConfig.getInstance().setParseType(ParseType.LIBRARY);
        return builder.parseType(ParseType.LIBRARY).build();
    }

    @Override
    void initLauncher(Launcher launcher) {
        AppConfig appConfig = AppConfig.getInstance();
        launcher.addInputResource(new DecompiledResource(appConfig.getGlobalLibraryPath(), new String[]{}, new CFRDecompiler(), appConfig.getGlobalDecompileOutputPath()));
    }
}