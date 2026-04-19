package com.puti.code.app.dependency;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 检测目标项目使用的构建工具类型
 */
public class BuildToolDetector {

    public enum BuildTool {
        MAVEN,
        GRADLE,
        UNKNOWN
    }

    /**
     * 检测项目根目录使用的构建工具。
     * 优先检测 Maven（pom.xml），再检测 Gradle（build.gradle / build.gradle.kts）。
     *
     * @param projectRootPath 项目根目录绝对路径
     * @return 检测到的构建工具类型
     */
    public static BuildTool detect(String projectRootPath) {
        Path root = Path.of(projectRootPath);

        if (Files.exists(root.resolve("pom.xml"))) {
            return BuildTool.MAVEN;
        }

        if (Files.exists(root.resolve("build.gradle")) || Files.exists(root.resolve("build.gradle.kts"))) {
            return BuildTool.GRADLE;
        }

        return BuildTool.UNKNOWN;
    }
}
