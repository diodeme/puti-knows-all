package com.puti.code.analyzer.java.rule;

import com.puti.code.rule.context.RuleContext;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spoon规则上下文构建器
 * 从Spoon元素构建规则上下文
 */
@Slf4j
public class SpoonRuleContextBuilder {
    
    /**
     * 从Spoon元素构建规则上下文
     * 
     * @param ctMethod 可执行元素
     * @param declaringType 声明类型
     * @return 规则上下文
     */
    public static RuleContext buildFromSpoon(CtMethod<?> ctMethod, CtType<?> declaringType) {
        RuleContext.RuleContextBuilder builder = RuleContext.builder();
        
        // 设置方法基本信息
        if (ctMethod != null) {
            builder.methodName(ctMethod.getSimpleName());
            builder.methodSignature(ctMethod.getSignature());
            builder.ownerTypeName(declaringType != null ? declaringType.getQualifiedName() : null);
            builder.returnTypeName(ctMethod.getType() != null ? ctMethod.getType().getQualifiedName() : null);
            builder.isPublic(ctMethod.isPublic());
            builder.isStatic(ctMethod.isStatic());
            builder.parameterTypes(ctMethod.getParameters().stream()
                    .map(parameter -> parameter.getType() != null ? parameter.getType().getQualifiedName() : "")
                    .toList());
        }
        
        // 提取方法注解
        List<String> annotations = extractExecutableAnnotations(ctMethod);
        builder.annotations(annotations);
        
        // 提取类注解
        List<String> classAnnotations = extractTypeAnnotations(declaringType);
        builder.classAnnotations(classAnnotations);
        
        // 提取实现的接口
        List<String> implementedInterfaces = extractImplementedInterfaces(declaringType);
        builder.implementedInterfaces(implementedInterfaces);
        
        // 提取父类
        List<String> superClassPath = extractSuperClass(declaringType);
        builder.superClassPath(superClassPath);
        
        // 初始化额外数据
        Map<String, Object> additionalData = new HashMap<>();
        builder.additionalData(additionalData);
        builder.annotationAttributes(new HashMap<>());
        
        return builder.build();
    }
    
    /**
     * 提取可执行元素的注解信息
     * 
     * @param executable 可执行元素
     * @return 注解全限定名列表
     */
    private static List<String> extractExecutableAnnotations(CtExecutable<?> executable) {
        if (executable == null || executable.getAnnotations() == null) {
            return new ArrayList<>();
        }
        
        return executable.getAnnotations().stream()
                .map(CtAnnotation::getAnnotationType)
                .map(CtTypeReference::getQualifiedName)
                .collect(Collectors.toList());
    }
    
    /**
     * 提取类型的注解信息
     * 
     * @param type 类型
     * @return 注解全限定名列表
     */
    private static List<String> extractTypeAnnotations(CtType<?> type) {
        if (type == null || type.getAnnotations() == null) {
            return new ArrayList<>();
        }
        
        return type.getAnnotations().stream()
                .map(CtAnnotation::getAnnotationType)
                .map(CtTypeReference::getQualifiedName)
                .collect(Collectors.toList());
    }
    
    /**
     * 提取实现的接口
     * 
     * @param type 类型
     * @return 接口全限定名列表
     */
    private static List<String> extractImplementedInterfaces(CtType<?> type) {
        if (type == null || type.getSuperInterfaces() == null) {
            return new ArrayList<>();
        }
        
        return type.getSuperInterfaces().stream()
                .map(CtTypeReference::getQualifiedName)
                .collect(Collectors.toList());
    }
    
    /**
     * 提取父类
     * 
     * @param type 类型
     * @return 父类全限定名
     */
    private static List<String> extractSuperClass(CtType<?> type) {
        List<String> superClassPath = new ArrayList<>();

        if (type == null || type.getSuperclass() == null) {
            return superClassPath;
        }
        CtTypeReference<?> superClass = type.getSuperclass();

        while(superClass!=null){
            superClassPath.add(superClass.getQualifiedName());
            superClass = superClass.getSuperclass();
        }

        return superClassPath;
    }
}
