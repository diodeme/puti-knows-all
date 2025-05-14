package com.puti.code.rule.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 统一注册 rule pack 中声明的自定义函数变量。
 */
@Slf4j
public class CustomFunctionRegistry {

    private static final Pattern VARIABLE_REFERENCE = Pattern.compile("#([A-Za-z_][A-Za-z0-9_]*)");

    public void register(Map<String, String> customFunctions,
                         StandardEvaluationContext evaluationContext,
                         ExpressionParser parser) {
        if (customFunctions == null || customFunctions.isEmpty()) {
            return;
        }
        Set<String> resolved = new HashSet<>();
        Set<String> resolving = new HashSet<>();
        customFunctions.keySet().forEach(name ->
                resolve(name, customFunctions, evaluationContext, parser, resolved, resolving));
    }

    private void resolve(String name,
                         Map<String, String> customFunctions,
                         StandardEvaluationContext evaluationContext,
                         ExpressionParser parser,
                         Set<String> resolved,
                         Set<String> resolving) {
        if (resolved.contains(name)) {
            return;
        }
        if (!resolving.add(name)) {
            log.warn("Detected cyclic custom function dependency: {}", name);
            return;
        }

        String expression = customFunctions.get(name);
        if (expression == null || expression.isBlank()) {
            resolving.remove(name);
            resolved.add(name);
            return;
        }

        extractDependencies(expression, customFunctions.keySet()).forEach(dependency ->
                resolve(dependency, customFunctions, evaluationContext, parser, resolved, resolving));

        try {
            Object value = parser.parseExpression(expression).getValue(evaluationContext);
            evaluationContext.setVariable(name, value);
            resolved.add(name);
        } catch (Exception e) {
            log.warn("Failed to register custom function: {} with expression: {}",
                    name, expression, e);
        } finally {
            resolving.remove(name);
        }
    }

    private Set<String> extractDependencies(String expression, Set<String> customFunctionNames) {
        Set<String> dependencies = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_REFERENCE.matcher(expression);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (customFunctionNames.contains(candidate)) {
                dependencies.add(candidate);
            }
        }
        return dependencies;
    }
}
