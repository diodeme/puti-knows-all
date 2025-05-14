package com.puti.code.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Code Graph AI 模块自动配置类
 * 
 * @author diodehe
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.ai.chat.client.ChatClient")
@ComponentScan(basePackages = {
    "com.puti.code.ai.documentation",
    "com.puti.code.ai.vector",
})
@Import(SpringAiConfig.class)
public class CodeGraphAiAutoConfiguration {

    public CodeGraphAiAutoConfiguration() {
        log.info("Code Graph AI 模块自动配置已启用");
    }
}
