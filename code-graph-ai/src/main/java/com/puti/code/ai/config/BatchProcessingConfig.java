package com.puti.code.ai.config;

import com.puti.code.ai.documentation.ModelConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 分批处理配置类
 * 管理AI文档生成的分批处理参数，支持动态配置不同模型的token限制
 *
 * @author diodehe
 */
@Slf4j
@Data
public class BatchProcessingConfig {

    private static final int DEFAULT_MAX_CONVERSATION_ROUNDS = 10;

    // 默认配置（当无法获取模型配置时使用）
    private static final int DEFAULT_MAX_TOKENS = 4000;
    private static final int DEFAULT_CHARS_PER_TOKEN = 4;
    private static final int DEFAULT_RESERVED_TOKENS = 1000;

    // 层级使用比例配置
    private static final double LEVEL_1_TOKEN_RATIO = 0.4;  // 第1层使用30%的token
    private static final double LEVEL_2_TOKEN_RATIO = 0.4;  // 第2层使用60%的token
    private static final double LEVEL_3_TOKEN_RATIO = 0.4;  // 第3层使用90%的token
    
    // 分批处理配置
    private static final int MAX_CONCURRENT_BATCHES = 3;   // 最大并发批次数
    
    // 重试配置
    private static final int MAX_RETRY_ATTEMPTS = 3;       // 最大重试次数
    private static final long RETRY_DELAY_MS = 1000;       // 重试延迟毫秒数
    
    /**
     * 获取指定层级的最大token数（基于模型配置动态计算）
     */
    public static int getMaxTokensForLevel(int level, int modelMaxTokens) {
        double ratio = switch (level) {
            case 1 -> LEVEL_1_TOKEN_RATIO;
            case 2 -> LEVEL_2_TOKEN_RATIO;
            default -> LEVEL_3_TOKEN_RATIO;
        };
        return (int) (modelMaxTokens * ratio);
    }

    /**
     * 获取指定层级的最大token数（扣除预留token）
     */
    public static int getMaxTokensForLevel(int level) {
        ModelConfig defaultConfig = ModelConfig.getDefaultConfig();
        int maxTokens = getMaxTokensForLevel(level, defaultConfig.getMaxTokens());
        return Math.max(0, maxTokens - defaultConfig.getReservedTokens());
    }

    /**
     * 获取最大并发批次数
     */
    public static int getMaxConcurrentBatches() {
        return MAX_CONCURRENT_BATCHES;
    }
}
