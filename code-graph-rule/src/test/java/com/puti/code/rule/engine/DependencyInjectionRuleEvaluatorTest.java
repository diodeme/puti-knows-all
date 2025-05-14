package com.puti.code.rule.engine;

import com.puti.code.base.model.EdgeCategory;
import com.puti.code.rule.context.RuleContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyInjectionRuleEvaluatorTest {

    private final DependencyInjectionRuleEvaluator evaluator = new DependencyInjectionRuleEvaluator();

    @Test
    void shouldMatchPriorityCandidateRuleAndEmitFrameworkMutation() {
        RuleContext context = RuleContext.builder()
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .additionalData(Map.of(
                        "factKind", "binding",
                        "candidateMatched", true,
                        "requestedNamePresent", false,
                        "priorityCandidateMatched", true,
                        "uniqueCandidateMatched", false,
                        "qualifierPresent", false,
                        "injectionMode", "autowired",
                        "resolutionKind", "priority",
                        "requestedName", "",
                        "resolvedTypeName", "com.demo.UserService"))
                .build();

        RuleMatchResult result = evaluator.evaluate(context);

        assertTrue(result.isMatched());
        assertEquals("instance_of", result.getMutations().getFirst().getEdgeType());
        assertEquals(EdgeCategory.FRAMEWORK, result.getMutations().getFirst().getEdgeCategory());
        assertEquals("priorityCandidateBinding", result.getMatchedRule());
        assertEquals("priority", result.getMutations().getFirst().getProperties().get("resolution_kind"));
    }

    @Test
    void shouldMatchRequestedNameRule() {
        RuleContext context = RuleContext.builder()
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .additionalData(Map.of(
                        "factKind", "binding",
                        "candidateMatched", true,
                        "requestedNamePresent", true,
                        "priorityCandidateMatched", false,
                        "uniqueCandidateMatched", false,
                        "qualifierPresent", false,
                        "injectionMode", "resource",
                        "resolutionKind", "requestedName",
                        "requestedName", "userService",
                        "resolvedTypeName", "com.demo.UserService"))
                .build();

        RuleMatchResult result = evaluator.evaluate(context);

        assertTrue(result.isMatched());
        assertEquals("requestedNameBinding", result.getMatchedRule());
    }

    @Test
    void shouldNotMatchWhenNoCandidateResolved() {
        RuleContext context = RuleContext.builder()
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .additionalData(Map.of(
                        "factKind", "binding",
                        "candidateMatched", false,
                        "requestedNamePresent", false,
                        "priorityCandidateMatched", false,
                        "uniqueCandidateMatched", false,
                        "qualifierPresent", false,
                        "injectionMode", "autowired",
                        "resolutionKind", "",
                        "requestedName", "",
                        "resolvedTypeName", ""))
                .build();

        RuleMatchResult result = evaluator.evaluate(context);

        assertFalse(result.isMatched());
    }

    @Test
    void shouldMatchRetargetedInjectionCallRule() {
        RuleContext context = RuleContext.builder()
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .additionalData(Map.ofEntries(
                        Map.entry("factKind", "callRetarget"),
                        Map.entry("candidateMatched", true),
                        Map.entry("requestedNamePresent", false),
                        Map.entry("priorityCandidateMatched", false),
                        Map.entry("uniqueCandidateMatched", false),
                        Map.entry("qualifierPresent", false),
                        Map.entry("injectionMode", "autowired"),
                        Map.entry("resolutionKind", "priority"),
                        Map.entry("requestedName", ""),
                        Map.entry("resolvedTypeName", "com.demo.UserService"),
                        Map.entry("declaredTargetTypeName", "com.demo.IUserService")))
                .build();

        RuleMatchResult result = evaluator.evaluate(context);

        assertTrue(result.isMatched());
        assertEquals("retargetedInjectionCall", result.getMatchedRule());
        assertEquals("injection_calls", result.getMutations().getFirst().getEdgeType());
        assertEquals("com.demo.IUserService",
                result.getMutations().getFirst().getProperties().get("declared_target_type"));
    }
}
