package com.puti.code.analyzer.java.strategy.impl;

import com.puti.code.base.model.FunctionNode;
import com.puti.code.base.model.Node;
import com.puti.code.analyzer.java.strategy.NodeContentStrategy;
import com.puti.code.analyzer.java.support.ParseSupport;
import org.apache.commons.lang3.BooleanUtils;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;

import java.util.stream.Collectors;

/**
 * 函数节点内容获取策略
 * 统一处理方法和构造函数
 * 
 * @author AI Assistant
 */
public class FunctionNodeStrategy implements NodeContentStrategy {
    
    @Override
    public String getDigest(Node node, Object context) {
        if (!(node instanceof FunctionNode functionNode)) {
            throw new IllegalArgumentException("Node must be FunctionNode");
        }
        
        if (!(context instanceof Factory factory)) {
            throw new IllegalArgumentException("Context must be Factory");
        }
        
        // 判断是否为构造函数
        if (BooleanUtils.isTrue(functionNode.getIsConstructor())) {
            return getConstructorContent(functionNode, factory);
        } else {
            return getMethodContent(functionNode, factory);
        }
    }
    
    /**
     * 获取构造函数内容
     */
    private String getConstructorContent(FunctionNode functionNode, Factory factory) {
        CtType<?> declaringType = factory.Type().get(functionNode.getFullName().split("#")[0]);
        if (declaringType instanceof CtClass<?> ctClass) {
            String constructorSignature = functionNode.getFullName();
            for (CtConstructor<?> constructor : ctClass.getConstructors()) {
                if (!ParseSupport.getExecutableSignature(ctClass.getQualifiedName(), constructor).equals(constructorSignature)) {
                    continue;
                }
                StringBuilder content = new StringBuilder();

                // 添加类注释
                addClassComments(declaringType, content);

                // 添加构造函数注释
                constructor.getComments().forEach(comment ->
                    content.append(comment.toString()).append("\n"));

                // 添加构造函数注解
                constructor.getAnnotations().forEach(annotation -> 
                    content.append(annotation.toString()).append("\n"));

                // 添加可见性
                String visibility = determineConstructorVisibility(constructor);
                content.append(visibility);
                if (!visibility.isEmpty()) {
                    content.append(" ");
                }

                content.append(constructor.getDeclaringType().getSimpleName())
                        .append("(");

                // 添加构造函数参数
                content.append(constructor.getParameters().stream()
                        .map(p -> p.getType().getSimpleName() + " " + p.getSimpleName())
                        .collect(Collectors.joining(", ")));

                content.append(");");

                return content.toString();
            }
        }
        return "Constructor: " + functionNode.getName();
    }
    
    /**
     * 获取方法内容
     */
    private String getMethodContent(FunctionNode functionNode, Factory factory) {
        CtType<?> declaringType = factory.Type().get(functionNode.getFullName().split("#")[0]);
        if (declaringType != null) {
            String methodSignature = functionNode.getFullName();
            for (CtMethod<?> method : declaringType.getMethods()) {
                if (!ParseSupport.getExecutableSignature(declaringType.getQualifiedName(), method).equals(methodSignature)) {
                    continue;
                }

                StringBuilder content = new StringBuilder();
                addClassComments(declaringType, content);
                ParseSupport.genMethodContent(content, method);

                return content.toString();
            }
        }
        return "Method: " + functionNode.getName();
    }
    
    /**
     * 确定构造函数可见性
     */
    private String determineConstructorVisibility(CtConstructor<?> element) {
        if (element.isPublic()) {
            return "public";
        } else if (element.isPrivate()) {
            return "private";
        } else if (element.isProtected()) {
            return "protected";
        }
        return "default";
    }

    /**
     * 添加类注释到内容中
     *
     * @param declaringType 声明类型
     * @param content 内容构建器
     */
    private void addClassComments(CtType<?> declaringType, StringBuilder content) {
        if (declaringType != null && !declaringType.getComments().isEmpty()) {
            // 添加类注释标识
            content.append("// Class comments:\n");

            // 添加类注释
            declaringType.getComments().forEach(comment ->
                content.append(comment.toString()).append("\n"));

            // 添加分隔符
            content.append("// Method:\n");
        }
    }
}
