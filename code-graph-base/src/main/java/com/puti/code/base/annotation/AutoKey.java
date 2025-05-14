package com.puti.code.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动生成分布式ID注解
 * 用于标记需要自动生成分布式ID的字段
 * 
 * @author diodehe
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoKey {
    
    /**
     * ID生成策略
     * @return ID生成策略
     */
    KeyStrategy strategy() default KeyStrategy.SNOWFLAKE;
    
    /**
     * ID生成策略枚举
     */
    enum KeyStrategy {
        /**
         * 雪花算法
         */
        SNOWFLAKE,
        
        /**
         * UUID（去掉横线）
         */
        UUID
    }
}
