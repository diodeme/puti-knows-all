package com.puti.code.documentation.config;

import com.puti.code.repository.milvus.DocumentationVectorClient;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文档向量配置类
 * 管理DocumentationVectorClient的生命周期
 * 
 * @author diodehe
 */
@Slf4j
@Configuration
public class DocumentationVectorConfig {

    private DocumentationVectorClient vectorClient;

    /**
     * 创建文档向量客户端Bean
     */
    @Bean
    public DocumentationVectorClient documentationVectorClient() {
        if (vectorClient == null) {
            vectorClient = new DocumentationVectorClient();
            log.info("DocumentationVectorClient Bean 已创建");
        }
        return vectorClient;
    }

    /**
     * 应用关闭时清理资源
     */
    @PreDestroy
    public void cleanup() {
        if (vectorClient != null) {
            try {
                vectorClient.close();
                log.info("DocumentationVectorClient 已关闭");
            } catch (Exception e) {
                log.error("关闭 DocumentationVectorClient 时发生错误", e);
            }
        }
    }
}
