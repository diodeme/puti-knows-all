package com.puti.code.ai.documentation;

import com.knuddels.jtokkit.api.EncodingType;
import com.puti.code.ai.support.TokenSupport;
import com.puti.code.base.config.AppConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * AI模型配置类
 * 管理不同AI模型的参数配置
 * 
 * @author diodehe
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfig {
    
    /**
     * 模型名称
     */
    private String modelName;
    
    /**
     * 最大token数
     */
    private int maxTokens;
    
    /**
     * 预留给指令和格式的token数
     */
    private int reservedTokens;

    /**
     * encoding集
     */
    private EncodingType encodingType;
    
    /**
     * 是否支持函数调用
     */
    private boolean supportsFunctionCalling;
    
    /**
     * 是否支持长上下文
     */
    private boolean supportsLongContext;
    
    // 预定义的模型配置
    private static final Map<String, ModelConfig> PREDEFINED_CONFIGS = new HashMap<>();
    
    static {

        PREDEFINED_CONFIGS.put("deepseek", ModelConfig.builder()
                .modelName("deepseek")
                .maxTokens(64000)
                .reservedTokens(1200)
                .encodingType(EncodingType.CL100K_BASE)
                .supportsFunctionCalling(false)
                .supportsLongContext(false)
                .build());

        PREDEFINED_CONFIGS.put("glm-4.7", ModelConfig.builder()
                .modelName("glm-4.7")
                .maxTokens(256000)
                .reservedTokens(1200)
                .encodingType(EncodingType.CL100K_BASE)
                .supportsFunctionCalling(false)
                .supportsLongContext(false)
                .build());
    }

    /**
     * 根据模型名称获取配置
     */
    public static ModelConfig getConfigByModelName(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            log.warn("模型名称为空，使用默认配置");
            return getDefaultConfig();
        }

        // 精确匹配
        ModelConfig config = PREDEFINED_CONFIGS.get(modelName.toLowerCase());
        if (config != null) {
            log.info("找到模型 {} 的预定义配置", modelName);
            return config;
        }

        // 模糊匹配
        for (String key : PREDEFINED_CONFIGS.keySet()) {
            if (modelName.toLowerCase().contains(key) || key.contains(modelName.toLowerCase())) {
                config = PREDEFINED_CONFIGS.get(key);
                log.info("通过模糊匹配为模型 {} 找到配置: {}", modelName, key);
                return config;
            }
        }

        log.warn("未找到模型 {} 的配置，使用默认配置", modelName);
        return getDefaultConfig();
    }

    /**
     * 获取默认配置
     */
    public static ModelConfig getDefaultConfig() {
        return getConfigByModelName(AppConfig.getInstance().getChatModel());
    }

}
