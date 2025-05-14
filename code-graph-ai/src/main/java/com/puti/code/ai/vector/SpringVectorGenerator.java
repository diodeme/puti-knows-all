package com.puti.code.ai.vector;

import com.puti.code.base.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring AI 向量生成器
 * 使用 Spring AI 的 EmbeddingClient 生成文本向量
 * 
 * @author diodehe
 */
@Slf4j
@Service
public class SpringVectorGenerator {
    
    private final OpenAiEmbeddingModel embeddingClient;
    private final AppConfig config;

    @Autowired
    public SpringVectorGenerator(OpenAiEmbeddingModel embeddingClient) {
        this.embeddingClient = embeddingClient;
        this.config = AppConfig.getInstance();
        log.info("Spring AI 向量生成器已初始化");
    }

    /**
     * 生成文本向量
     * 
     * @param text 文本
     * @return 向量
     */
    public float[] generateVector(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("提供的文本为空，返回零向量");
            return new float[config.getMilvusDimension()];
        }

        try {
            log.debug("开始生成向量，文本长度: {}", text.length());
            
            // 预处理文本：移除换行符，替换为空格
            String processedText = preprocessText(text);
            
            // 使用 Spring AI EmbeddingClient 生成向量
            EmbeddingResponse response = embeddingClient.embedForResponse(List.of(processedText));
            
            if (response.getResults().isEmpty()) {
                log.error("向量生成响应为空");
                return new float[config.getMilvusDimension()];
            }

            // 获取第一个结果的向量
            float[] embedding = response.getResults().getFirst().getOutput();

            if (embedding.length == 0) {
                log.error("向量数据为空");
                return new float[config.getMilvusDimension()];
            }
            
            log.debug("成功生成向量，维度: {}", embedding.length);
            return embedding;
            
        } catch (Exception e) {
            log.error("生成向量时发生错误", e);
            return new float[config.getMilvusDimension()];
        }
    }

    /**
     * 预处理文本
     */
    private String preprocessText(String text) {
        if (text == null) {
            return "";
        }
        
        return text.replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .replaceAll("\\s+", " ")  // 合并多个空格
                .trim();
    }
}
