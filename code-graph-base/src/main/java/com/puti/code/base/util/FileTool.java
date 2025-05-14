package com.puti.code.base.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@UtilityClass
@Slf4j
public class FileTool {


    /**
     * 获取指定目录下所有jar文件的URL数组。
     *
     * @param directoryPath 目录路径
     * @return jar文件的URL数组，可直接用于URLClassLoader
     */
    public URL[] getAllJarUrls(String directoryPath) {
        List<URL> urlList = new ArrayList<>();
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            log.warn("Directory path is null or empty.");
            return new URL[0];
        }

        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            log.warn("Library directory does not exist or is not a directory: {}", directoryPath);
            return new URL[0];
        }

        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            urlList = paths
                    // 1. 筛选出普通文件
                    .filter(Files::isRegularFile)
                    // 2. 筛选出以 .jar 结尾的文件（忽略大小写）
                    .filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                    // 3. 将每个 Path 对象转换为 URL 对象
                    .map(path -> {
                        try {
                            // 使用 .toUri().toURL() 是最可靠的转换方式
                            return path.toUri().toURL();
                        } catch (MalformedURLException e) {
                            // 如果某个特定路径转换失败，记录警告并返回 null
                            log.warn("Could not convert path to URL: {}", path, e);
                            return null;
                        }
                    })
                    // 4. 过滤掉所有在转换中失败的 null 对象
                    .filter(Objects::nonNull)
                    // 5. 将结果收集到一个 List<URL> 中
                    .toList();

            log.info("Found {} jar files in {}", urlList.size(), directoryPath);
            for (URL url : urlList) {
                log.debug("Found JAR URL: {}", url);
            }

        } catch (IOException e) {
            log.error("Error while searching for jar files in directory: {}", directoryPath, e);
        }

        return urlList.toArray(new URL[0]);
    }
}
