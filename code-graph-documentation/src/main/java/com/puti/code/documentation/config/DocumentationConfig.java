package com.puti.code.documentation.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 说明书生成配置类
 * 使用Spring Boot配置属性绑定
 *
 * @author diodehe
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "documentation")
public class DocumentationConfig {
    
    // 渐进式生成配置
    private Level1 level1 = new Level1();
    private Level2 level2 = new Level2();
    private Level3 level3 = new Level3();

    // AI生成配置
    private Ai ai = new Ai();

    // 归档策略配置
    private Archive archive = new Archive();

    // 并发控制配置
    private Concurrent concurrent = new Concurrent();

    // 存储配置
    private Storage storage = new Storage();

    @Data
    public static class Level1 {
        private int maxSteps = 2;           // 第1层最大步数
        private int maxLength = 1000;       // 第1层文档最大字符数
    }

    @Data
    public static class Level2 {
        private int maxSteps = 5;           // 第2层最大步数
        private int maxLength = 3000;       // 第2层文档最大字符数
    }

    @Data
    public static class Level3 {
        private int maxSteps = 10;          // 第3层最大步数
        private int maxLength = 10000;      // 第3层文档最大字符数
    }

    @Data
    public static class Ai {
        private int maxTokens = 4000;       // 每次请求最大token数
        private int maxRetry = 3;           // 最大重试次数
        private long retryDelayMs = 1000;   // 重试延迟毫秒数
    }

    @Data
    public static class Archive {
        private boolean autoEnabled = true;        // 是否自动归档
        private int delayDays = 7;                // 归档延迟天数
        private int maxIntermediateVersions = 5;  // 最大中间版本数
    }

    @Data
    public static class Concurrent {
        private int maxTasks = 3;              // 最大并发任务数
        private long timeoutMinutes = 30;      // 任务超时分钟数
    }

    @Data
    public static class Storage {
        private String path = "documentation";  // 文档存储路径
        private boolean useDatabase = true;     // 是否使用数据库存储
        private boolean useFilesystem = false;  // 是否使用文件系统存储
    }

    
    /**
     * 根据层级获取最大步数
     */
    public int getMaxStepsForLevel(int level) {
        return switch (level) {
            case 1 -> level1.getMaxSteps();
            case 2 -> level2.getMaxSteps();
            case 3 -> level3.getMaxSteps();
            default -> level3.getMaxSteps();
        };
    }

    /**
     * 根据层级获取最大文档长度
     */
    public int getMaxLengthForLevel(int level) {
        return switch (level) {
            case 1 -> level1.getMaxLength();
            case 2 -> level2.getMaxLength();
            case 3 -> level3.getMaxLength();
            default -> level3.getMaxLength();
        };
    }

    // 兼容性方法，保持向后兼容
    public int getMaxConcurrentTasks() {
        return concurrent.getMaxTasks();
    }

    public boolean isAutoArchiveEnabled() {
        return archive.isAutoEnabled();
    }

    public int getArchiveDelayDays() {
        return archive.getDelayDays();
    }

    public int getMaxIntermediateVersions() {
        return archive.getMaxIntermediateVersions();
    }

    public String getDocumentationStoragePath() {
        return storage.getPath();
    }

    public boolean isUseDatabase() {
        return storage.isUseDatabase();
    }

    public boolean isUseFileSystem() {
        return storage.isUseFilesystem();
    }
}
