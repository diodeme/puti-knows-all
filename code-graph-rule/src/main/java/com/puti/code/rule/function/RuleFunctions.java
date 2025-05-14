package com.puti.code.rule.function;

import com.puti.code.rule.context.RuleContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 规则表达式中可用的自定义函数基类
 * 提供通用的规则函数，不依赖具体的代码分析框架
 */
@Slf4j
public class RuleFunctions {

    protected final RuleContext context;

    public RuleFunctions(RuleContext context) {
        this.context = context;
    }
    
    /**
     * 检查方法是否有指定注解
     * 
     * @param annotationName 注解全限定名
     * @return 是否有该注解
     */
    public boolean hasAnnotation(String annotationName) {
        if (context.getAnnotations() == null) {
            return false;
        }
        return context.getAnnotations().contains(annotationName);
    }
    
    /**
     * 检查类是否实现了指定接口
     * 
     * @param interfaceName 接口全限定名
     * @return 是否实现了该接口
     */
    public boolean implementsInterface(String interfaceName) {
        if (context.getImplementedInterfaces() == null) {
            return false;
        }
        return context.getImplementedInterfaces().contains(interfaceName);
    }
    
    /**
     * 检查类是否有指定注解
     * 
     * @param annotationName 注解全限定名
     * @return 是否有该注解
     */
    public boolean hasClassAnnotation(String annotationName) {
        if (context.getClassAnnotations() == null) {
            return false;
        }
        return context.getClassAnnotations().contains(annotationName);
    }
    
    /**
     * 检查方法是否有任意一个指定的注解
     * 
     * @param annotationNames 注解全限定名列表
     * @return 是否有任意一个注解
     */
    public boolean hasAnyAnnotation(List<String> annotationNames) {
        if (context.getAnnotations() == null || annotationNames == null) {
            return false;
        }
        return annotationNames.stream().anyMatch(this::hasAnnotation);
    }
    
    /**
     * 检查类是否实现了任意一个指定的接口
     * 
     * @param interfaceNames 接口全限定名列表
     * @return 是否实现了任意一个接口
     */
    public boolean implementsAnyInterface(List<String> interfaceNames) {
        if (context.getImplementedInterfaces() == null || interfaceNames == null) {
            return false;
        }
        return interfaceNames.stream().anyMatch(this::implementsInterface);
    }
    
    /**
     * 获取方法名
     *
     * @return 方法名
     */
    public String getMethodName() {
        if (context.getMethodName() != null && !context.getMethodName().isBlank()) {
            return context.getMethodName();
        }
        if (context.getMethodSignature() == null || context.getMethodSignature().isBlank()) {
            return "";
        }
        String signature = context.getMethodSignature();
        int hashIndex = signature.lastIndexOf('#');
        String tail = hashIndex >= 0 ? signature.substring(hashIndex + 1) : signature;
        int parameterIndex = tail.indexOf('(');
        return parameterIndex >= 0 ? tail.substring(0, parameterIndex) : tail;
    }

    /**
     * 检查方法名是否匹配
     *
     * @param methodName 方法名
     * @return 是否匹配
     */
    public boolean methodNameEquals(String methodName) {
        return methodName.equals(getMethodName());
    }

    /**
     * 检查方法名是否匹配正则表达式
     *
     * @param pattern 正则表达式
     * @return 是否匹配
     */
    public boolean methodNameMatches(String pattern) {
        return getMethodName().matches(pattern);
    }

    /**
     * 检查方法是否为public
     *
     * @return 是否为public
     */
    public boolean isPublic() {
        return Boolean.TRUE.equals(context.getIsPublic());
    }

    /**
     * 检查方法是否为static
     *
     * @return 是否为static
     */
    public boolean isStatic() {
        return Boolean.TRUE.equals(context.getIsStatic());
    }
    
    /**
     * 检查类是否继承自指定类
     * 
     * @param className 类全限定名
     * @return 是否继承自该类
     */
    public boolean extendsClass(String className) {
        return context.getSuperClassPath().contains(className);
    }
}
