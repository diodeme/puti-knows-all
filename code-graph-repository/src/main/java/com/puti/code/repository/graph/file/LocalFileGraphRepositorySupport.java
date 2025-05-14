package com.puti.code.repository.graph.file;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puti.code.base.config.AppConfig;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地图存储公共支持类。
 */
public final class LocalFileGraphRepositorySupport {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private LocalFileGraphRepositorySupport() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Path resolveStorageDir(AppConfig config) {
        AppConfig safeConfig = config != null ? config : AppConfig.getInstance();
        Path baseDir = StringUtils.isNotBlank(safeConfig.getProjectRootPath())
                ? Paths.get(safeConfig.getProjectRootPath())
                : Paths.get(System.getProperty("user.dir"));
        Path storageDir = baseDir.resolve(".maling").resolve("graph-storage");
        if (StringUtils.isNotBlank(safeConfig.getProjectId())) {
            storageDir = storageDir.resolve(sanitizePathSegment(safeConfig.getProjectId()));
        }
        if (StringUtils.isNotBlank(safeConfig.getBranch())) {
            storageDir = storageDir.resolve(sanitizePathSegment(safeConfig.getBranch()));
        }
        return storageDir;
    }

    public static Path resolveNodeFile(AppConfig config) {
        return resolveStorageDir(config).resolve("nodes.jsonl");
    }

    public static Path resolveEdgeFile(AppConfig config) {
        return resolveStorageDir(config).resolve("edges.jsonl");
    }

    private static String sanitizePathSegment(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
