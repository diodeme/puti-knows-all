package com.puti.code.documentation.config;

import com.puti.code.documentation.global.AutoKeyInterceptor;
import jakarta.annotation.PostConstruct;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MyBatis配置类
 * 用于注册自定义拦截器
 * 
 * @author diodehe
 */
@Configuration
public class MyBatisConfig {
    
    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;
    
    @Autowired
    private AutoKeyInterceptor autoKeyInterceptor;
    
    /**
     * 注册拦截器
     */
    @PostConstruct
    public void addInterceptors() {
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            sqlSessionFactory.getConfiguration().addInterceptor(autoKeyInterceptor);
        }
    }
}
