package com.puti.code.rule.engine;

import com.puti.code.rule.context.RuleContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用的 mutation 规则执行器。
 */
@Slf4j
@RequiredArgsConstructor
public class GenericMutationRuleEvaluator {

    private final SpelRuleEvaluationSupport evaluationSupport;
    private final ExpressionParser parser = new SpelExpressionParser();

    public RuleMatchResult evaluate(List<? extends MutationRuleDefinition> rules, RuleContext context) {
        return evaluate(rules, context, Map.of());
    }

    public RuleMatchResult evaluate(List<? extends MutationRuleDefinition> rules,
                                    RuleContext context,
                                    Map<String, String> customFunctions) {
        if (rules == null || rules.isEmpty()) {
            return RuleMatchResult.builder().matched(false).build();
        }

        StandardEvaluationContext evaluationContext = evaluationSupport.createEvaluationContext(context, customFunctions);
        List<? extends MutationRuleDefinition> sortedRules = rules.stream()
                .sorted(Comparator.comparing(
                        MutationRuleDefinition::getPriority,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        for (MutationRuleDefinition rule : sortedRules) {
            try {
                boolean scopeMatched = evaluateCondition(rule.getMatch(), evaluationContext, true);
                boolean conditionMatched = evaluateCondition(rule.getWhen(), evaluationContext, true);
                if (scopeMatched && conditionMatched) {
                    Map<String, Object> emittedProperties = evaluateProperties(rule.getEmitProperties(), evaluationContext);
                    GraphMutation.MutationType mutationType = (rule.getEdgeType() == null || rule.getEdgeType().isBlank())
                            ? GraphMutation.MutationType.PROPERTY
                            : GraphMutation.MutationType.EDGE;
                    return RuleMatchResult.builder()
                            .matched(true)
                            .matchedRule(rule.getName())
                            .mutation(GraphMutation.builder()
                                    .mutationType(mutationType)
                                    .edgeType(rule.getEdgeType())
                                    .edgeCategory(rule.getEdgeCategory())
                                    .properties(emittedProperties)
                                    .build())
                            .build();
                }
            } catch (Exception e) {
                log.warn("Failed to evaluate mutation rule: {}", rule.getName(), e);
            }
        }
        return RuleMatchResult.builder().matched(false).build();
    }

    private boolean evaluateCondition(String expressionValue,
                                      StandardEvaluationContext evaluationContext,
                                      boolean defaultValue) {
        if (expressionValue == null || expressionValue.isBlank()) {
            return defaultValue;
        }
        Expression expression = parser.parseExpression(expressionValue);
        Boolean matched = expression.getValue(evaluationContext, Boolean.class);
        return Boolean.TRUE.equals(matched);
    }

    private Map<String, Object> evaluateProperties(Map<String, Object> configuredProperties,
                                                   StandardEvaluationContext evaluationContext) {
        Map<String, Object> resolvedProperties = new LinkedHashMap<>();
        if (configuredProperties == null || configuredProperties.isEmpty()) {
            return resolvedProperties;
        }
        configuredProperties.forEach((key, value) -> {
            Object resolvedValue = evaluatePropertyValue(value, evaluationContext);
            if (resolvedValue != null) {
                resolvedProperties.put(key, resolvedValue);
            }
        });
        return resolvedProperties;
    }

    private Object evaluatePropertyValue(Object configuredValue, StandardEvaluationContext evaluationContext) {
        if (!(configuredValue instanceof String expressionValue)) {
            return configuredValue;
        }
        try {
            return parser.parseExpression(expressionValue).getValue(evaluationContext);
        } catch (Exception ignored) {
            return expressionValue;
        }
    }
}
