package com.puti.code.rule.context;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 规则执行上下文
 * 包含规则执行所需的通用信息，不依赖具体的代码分析框架
 */
@Getter
@Builder
public class RuleContext {

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 方法的完整签名
     */
    private String methodSignature;

    private String ownerTypeName;

    private String returnTypeName;

    /**
     * 方法是否为public
     */
    private Boolean isPublic;

    /**
     * 方法是否为static
     */
    private Boolean isStatic;

    /**
     * 方法的注解信息（全限定名列表）
     */
    private List<String> annotations;

    /**
     * 类实现的接口列表（全限定名列表）
     */
    private List<String> implementedInterfaces;

    /**
     * 类的父类（全限定名）
     */
    private List<String> superClassPath;

    /**
     * 类的注解信息（全限定名列表）
     */
    private List<String> classAnnotations;

    private Map<String, Object> annotationAttributes;

    private List<String> parameterTypes;

    /**
     * 额外的上下文数据
     */
    private Map<String, Object> additionalData;
    
    /**
     * 获取额外数据
     * 
     * @param key 键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAdditionalData(String key, T defaultValue) {
        if (additionalData == null) {
            return defaultValue;
        }
        return (T) additionalData.getOrDefault(key, defaultValue);
    }
}
