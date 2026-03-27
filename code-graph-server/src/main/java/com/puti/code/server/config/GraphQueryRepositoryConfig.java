package com.puti.code.server.config;

import com.puti.code.base.config.AppConfig;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import com.puti.code.repository.graph.query.GraphQueryRepositoryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphQueryRepositoryConfig {

    @Bean
    public GraphQueryRepository graphQueryRepository() {
        return GraphQueryRepositoryFactory.create(AppConfig.getInstance());
    }
}
