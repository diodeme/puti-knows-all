package com.puti.code.documentation.config;

import com.puti.code.base.config.AppConfig;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import com.puti.code.repository.graph.query.GraphQueryRepositoryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 说明书生成器Spring配置类
 * 
 * @author diodehe
 */
@Slf4j
@Configuration
public class DocumentationSpringConfig implements AsyncConfigurer {

    @Bean
    public GraphQueryRepository graphQueryRepository() {
        return GraphQueryRepositoryFactory.create(AppConfig.getInstance());
    }
    
    /**
     * 配置异步执行器
     */
    @Override
    @Bean(name = "codeDocumentTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(3);
        // 最大线程数
        executor.setMaxPoolSize(10);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名前缀
        executor.setThreadNamePrefix("Documentation-");
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("异步执行器配置完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * 配置定时任务执行器
     */
    @Bean(name = "scheduledTaskExecutor")
    public Executor getScheduledExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Scheduled-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("定时任务执行器配置完成");
        
        return executor;
    }
}
