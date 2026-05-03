package com.puti.code.analyzer.java.support;

import spoon.reflect.code.*;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;

import java.lang.IncompatibleClassChangeError;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ParseSupport {
    public static boolean isSimpleGetter(CtExecutable<?> method) {
        if (method == null || method.getBody() == null) {
            return false;
        }

        String methodName = method.getSimpleName();
        List<CtParameter<?>> parameters = method.getParameters();
        CtTypeReference<?> returnType = method.getType();

        boolean nameOk = (methodName.startsWith("get") && methodName.length() > 3 && !methodName.equals("getClass")) ||
                (methodName.startsWith("is") && methodName.length() > 2);
        if (!nameOk) return false;

        if (!parameters.isEmpty()) return false;

        if (methodName.startsWith("get") && returnType.isPrimitive() && returnType.getSimpleName().equals("void")) {
            return false;
        }
        if (methodName.startsWith("is") && !(returnType.getSimpleName().equals("boolean") || returnType.getQualifiedName().equals("java.lang.Boolean"))) {
            return false;
        }

        List<CtStatement> statements = method.getBody().getStatements();
        if (statements.size() != 1) return false;

        CtStatement firstStatement = statements.getFirst();
        if (!(firstStatement instanceof CtReturn<?> ctReturn)) return false;

        CtExpression<?> returnedExpression = ctReturn.getReturnedExpression();

        // 确保返回的是一个字段读取
        return returnedExpression instanceof CtFieldRead;
    }

    public static boolean isSimpleSetter(CtExecutable<?> method) {
        if (method == null || method.getBody() == null) {
            return false;
        }

        String methodName = method.getSimpleName();
        List<CtParameter<?>> parameters = method.getParameters();
        CtTypeReference<?> returnType = method.getType();

        if (!(methodName.startsWith("set") && methodName.length() > 3)) return false;
        if (parameters.size() != 1) return false;
        if (!(returnType.isPrimitive() && returnType.getSimpleName().equals("void"))) {
            // 检查是否为 java.lang.Void (虽然不常见，但理论上可能)
            if (returnType.getQualifiedName() != null && !returnType.getQualifiedName().equals("java.lang.Void")) {
                // 也可能 returnType.getActualClass() == void.class
                if (! (returnType.isPrimitive() && returnType.getSimpleName().equals("void"))) {
                    return false;
                }
            } else if (!returnType.isPrimitive()) { // 如果不是原始类型void也不是java.lang.Void
                return false;
            }
        }


        List<CtStatement> statements = method.getBody().getStatements();
        if (statements.size() != 1) return false;

        CtStatement firstStatement = statements.getFirst();
        if (!(firstStatement instanceof CtAssignment<?, ?> ctAssignment)) return false;

        CtExpression<?> assigned = ctAssignment.getAssigned(); // 左边

        if (!(assigned instanceof CtFieldWrite<?> fieldWrite)) return false; // 左边必须是字段写入

        // 检查 CtFieldWrite 的 target 是否是 this
        return fieldWrite.getTarget() == null || fieldWrite.getTarget() instanceof CtThisAccess;
    }

    public static boolean isIgnoreType(CtTypeReference<?> typeReference){
        if (typeReference == null || typeReference.isPrimitive()) {
            return true;
        }
        // 泛型类型变量（T, E, K, V 等）不是真正的类型，不应生成边或节点
        if (typeReference instanceof CtTypeParameterReference) {
            return true;
        }
        try {
            // Only ignore JDK/standard library classes.
            // Types with null typeDeclaration (unresolved from classpath) are NOT ignored -
            // they represent dependency library classes that Spoon can't fully resolve,
            // but their calls should still create edges (OUT_CALLS) for graph connectivity.
            return JdkClassChecker.isJdkClass(typeReference);
        } catch (Exception | IncompatibleClassChangeError e) {
            log.debug("Ignoring type due to shadow class build failure: {} - {}", typeReference.getQualifiedName(), e.getClass().getSimpleName());
            return true;
        }
    }

    public static void genMethodContent(StringBuilder content, CtMethod<?> method){
        // 添加方法注释
        method.getComments().forEach(comment ->
                content.append("    ").append(comment.toString()).append("\n"));

        // 添加方法注解
        method.getAnnotations().forEach(annotation ->
                content.append("    ").append(annotation.toString()).append("\n"));

        // 添加方法签名
        content.append("    ");
        content.append(method.getModifiers().stream()
                .map(Object::toString)
                .collect(Collectors.joining(" ")));
        if (!method.getModifiers().isEmpty()) {
            content.append(" ");
        }

        if (!method.getFormalCtTypeParameters().isEmpty()) {
            content.append("<");
            content.append(method.getFormalCtTypeParameters().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")));
            content.append("> ");
        }

        content.append(method.getType().getSimpleName())
                .append(" ")
                .append(method.getSimpleName())
                .append("(");

        // 添加方法参数
        content.append(method.getParameters().stream()
                .map(p -> p.getType().getSimpleName() + " " + p.getSimpleName())
                .collect(Collectors.joining(", ")));

        content.append(");\n");
    }

    public static String getExecutableSignature(String targetClassName, CtExecutable<?> method) {
        return getExecutableSignature(targetClassName, method.getSignature());
    }

    private static String getExecutableSignature(String targetClassName, String signature) {
        return targetClassName + "#" + signature;
    }


}
