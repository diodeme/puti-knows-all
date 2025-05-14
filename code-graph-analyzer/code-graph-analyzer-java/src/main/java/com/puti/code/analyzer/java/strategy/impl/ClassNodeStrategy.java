package com.puti.code.analyzer.java.strategy.impl;

import com.puti.code.analyzer.java.strategy.NodeContentStrategy;
import com.puti.code.analyzer.java.support.ParseSupport;
import com.puti.code.base.model.ClassNode;
import com.puti.code.base.model.Node;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import java.util.stream.Collectors;

/**
 * 类节点内容获取策略
 * 
 * @author AI Assistant
 */
public class ClassNodeStrategy implements NodeContentStrategy {
    
    @Override
    public String getDigest(Node node, Object context) {
        if (!(node instanceof ClassNode classNode)) {
            throw new IllegalArgumentException("Node must be ClassNode");
        }
        
        if (!(context instanceof Factory factory)) {
            throw new IllegalArgumentException("Context must be Factory");
        }
        
        CtType<?> ctType = factory.Type().get(classNode.getFullName());
        if (ctType != null) {
            StringBuilder content = new StringBuilder();

            // 添加类注释
            ctType.getComments().forEach(comment -> 
                content.append(comment.toString()).append("\n"));

            // 添加类注解
            ctType.getAnnotations().forEach(annotation -> 
                content.append(annotation.toString()).append("\n"));

            // 添加类定义（不包含方法体）
            content.append(ctType.getModifiers().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(" ")));
            if (!ctType.getModifiers().isEmpty()) {
                content.append(" ");
            }

            // 添加类型关键字
            if (ctType.isClass()) {
                content.append("class ");
            } else if (ctType.isInterface()) {
                content.append("interface ");
            } else if (ctType.isEnum()) {
                content.append("enum ");
            } else if (ctType.isAnnotationType()) {
                content.append("@interface ");
            }

            content.append(ctType.getSimpleName());

            // 添加泛型参数
            if (!ctType.getFormalCtTypeParameters().isEmpty()) {
                content.append("<");
                content.append(ctType.getFormalCtTypeParameters().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(", ")));
                content.append(">");
            }

            // 添加继承和实现
            if (ctType.getSuperclass() != null) {
                content.append(" extends ").append(ctType.getSuperclass().getSimpleName());
            }
            if (!ctType.getSuperInterfaces().isEmpty()) {
                content.append(" implements ");
                content.append(ctType.getSuperInterfaces().stream()
                        .map(CtTypeReference::getSimpleName)
                        .collect(Collectors.joining(", ")));
            }

            content.append(" {\n");

            // 添加字段
            ctType.getFields().forEach(field -> {
                content.append("    ");
                content.append(field.getModifiers().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(" ")));
                if (!field.getModifiers().isEmpty()) {
                    content.append(" ");
                }
                content.append(field.getType().getSimpleName())
                        .append(" ")
                        .append(field.getSimpleName())
                        .append(";\n");
            });

            // 添加方法签名（不包含方法体）
            for (CtMethod<?> method : ctType.getMethods()) {
                ParseSupport.genMethodContent(content, method);
            }

            content.append("}");
            return content.toString();
        } else {
            return "Class: " + classNode.getName();
        }
    }
}
