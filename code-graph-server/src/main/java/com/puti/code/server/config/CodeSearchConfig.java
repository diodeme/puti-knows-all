package com.puti.code.server.config;

import com.puti.code.ai.vector.VectorGenerator;
import com.puti.code.repository.milvus.MilvusSearchClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodeSearchConfig {

    @Bean
    public MilvusSearchClient milvusSearchClient() {
        return new MilvusSearchClient();
    }

    @Bean
    public VectorGenerator vectorGenerator() {
        return new VectorGenerator();
    }
}
