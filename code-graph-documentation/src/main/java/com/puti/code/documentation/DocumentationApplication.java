package com.puti.code.documentation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 说明书生成器Spring Boot应用主类
 * 
 * @author diodehe
 */
@SpringBootApplication
@MapperScan("com.puti.code.documentation.repository.sql.mapper")
@EnableAsync
@EnableScheduling
public class DocumentationApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DocumentationApplication.class, args);
    }
}
