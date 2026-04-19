package com.puti.code.app.dependency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 依赖清单持久化工具。
 * 在 {projectRootPath}/.library/dependency-manifest.json 中记录项目的依赖列表（GAV → scope），
 * 用于增量更新时对比前后依赖差异。
 */
@Slf4j
public class DependencyManifest {

    private static final String MANIFEST_FILE_NAME = "dependency-manifest.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 从 manifest 文件加载依赖列表。
     *
     * @param manifestFile manifest 文件路径
     * @return GAV → scope 映射，文件不存在或解析失败时返回空 Map
     */
    public static Map<String, String> load(Path manifestFile) {
        if (!Files.exists(manifestFile)) {
            log.debug("Manifest file not found: {}", manifestFile);
            return Collections.emptyMap();
        }
        try {
            ManifestData data = MAPPER.readValue(manifestFile.toFile(), ManifestData.class);
            log.info("Loaded {} dependencies from manifest: {}", data.dependencies.size(), manifestFile);
            return data.dependencies != null ? data.dependencies : Collections.emptyMap();
        } catch (IOException e) {
            log.warn("Failed to parse manifest file: {}, starting fresh", manifestFile, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 将依赖列表保存到 manifest 文件。
     *
     * @param manifestFile manifest 文件路径
     * @param dependencies GAV → scope 映射
     */
    public static void save(Path manifestFile, Map<String, String> dependencies) {
        try {
            Files.createDirectories(manifestFile.getParent());
            ManifestData data = new ManifestData();
            data.lastUpdated = Instant.now().toString();
            data.dependencies = new LinkedHashMap<>(dependencies);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), data);
            log.info("Saved {} dependencies to manifest: {}", dependencies.size(), manifestFile);
        } catch (IOException e) {
            log.error("Failed to save manifest file: {}", manifestFile, e);
        }
    }

    /**
     * 检查 manifest 文件是否存在。
     */
    public static boolean exists(Path manifestFile) {
        return Files.exists(manifestFile);
    }

    /**
     * 获取默认 manifest 文件路径。
     *
     * @param projectRootPath 项目根目录
     * @return manifest 文件路径
     */
    public static Path defaultManifestPath(String projectRootPath) {
        return Path.of(projectRootPath, ".library", MANIFEST_FILE_NAME);
    }

    /**
     * Manifest 文件数据结构。
     */
    private static class ManifestData {
        public String lastUpdated;
        public Map<String, String> dependencies;
    }
}
