package com.puti.code.rule.engine;

import com.puti.code.base.model.EdgeCategory;
import com.puti.code.rule.context.RuleContext;
import com.puti.code.rule.context.RuleContextKeys;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericMutationRuleEvaluatorTest {

    private final GenericMutationRuleEvaluator evaluator =
            new GenericMutationRuleEvaluator(new SpelRuleEvaluationSupport());

    @Test
    void shouldEvaluateMutationRuleDefinitions() {
        RuleContext context = RuleContext.builder()
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .additionalData(Map.of(
                        RuleContextKeys.CANDIDATE_MATCHED, true,
                        RuleContextKeys.QUALIFIER_PRESENT, true))
                .build();

        List<MutationRuleDefinition> rules = List.of(
                new TestRule("qualified", "#candidateMatched and #qualifierPresent"),
                new TestRule("fallback", "#candidateMatched"));

        RuleMatchResult result = evaluator.evaluate(rules, context);

        assertTrue(result.isMatched());
        assertEquals("qualified", result.getMatchedRule());
        assertEquals("instance_of", result.getMutations().getFirst().getEdgeType());
    }

    @Test
    void shouldReturnUnmatchedWhenNoRuleHits() {
        RuleContext context = RuleContext.builder()
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .additionalData(Map.of(RuleContextKeys.CANDIDATE_MATCHED, false))
                .build();

        RuleMatchResult result = evaluator.evaluate(List.of(new TestRule("qualified", "#candidateMatched")), context);

        assertFalse(result.isMatched());
    }

    @Test
    void shouldPreferHigherPriorityRuleAndEmitProperties() {
        RuleContext context = RuleContext.builder()
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .additionalData(Map.of(
                        RuleContextKeys.CANDIDATE_MATCHED, true,
                        RuleContextKeys.RESOLUTION_KIND, "priority"))
                .build();

        List<MutationRuleDefinition> rules = List.of(
                new TestRule("lowPriority", "#candidateMatched", 10, Map.of("resolution_kind", "'fallback'")),
                new TestRule("highPriority", "#candidateMatched", 100, Map.of("resolution_kind", "#resolutionKind")));

        RuleMatchResult result = evaluator.evaluate(rules, context);

        assertTrue(result.isMatched());
        assertEquals("highPriority", result.getMatchedRule());
        assertEquals("priority", result.getMutations().getFirst().getProperties().get("resolution_kind"));
    }

    @Test
    void shouldRequireMatchBeforeEvaluatingWhen() {
        RuleContext context = RuleContext.builder()
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .additionalData(Map.of(
                        RuleContextKeys.FACT_KIND, "binding",
                        RuleContextKeys.CANDIDATE_MATCHED, true))
                .build();

        List<MutationRuleDefinition> rules = List.of(
                new TestRule("callRetargetRule", "#factKind == 'callRetarget'", "#candidateMatched", 100, Map.of()),
                new TestRule("bindingRule", "#factKind == 'binding'", "#candidateMatched", 10, Map.of()));

        RuleMatchResult result = evaluator.evaluate(rules, context);

        assertTrue(result.isMatched());
        assertEquals("bindingRule", result.getMatchedRule());
    }

    @Test
    void shouldDropNullEmittedProperties() {
        RuleContext context = RuleContext.builder()
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .additionalData(Map.of(RuleContextKeys.CANDIDATE_MATCHED, true))
                .build();

        List<MutationRuleDefinition> rules = List.of(
                new TestRule("nullPropertyRule", "#candidateMatched", 10, Map.of(
                        "requested_name", "#requestedName",
                        "resolution_kind", "'memberName'")));

        RuleMatchResult result = evaluator.evaluate(rules, context);

        assertTrue(result.isMatched());
        assertFalse(result.getMutations().getFirst().getProperties().containsKey("requested_name"));
        assertEquals("memberName", result.getMutations().getFirst().getProperties().get("resolution_kind"));
    }

    @Test
    void shouldEvaluateConfiguredCustomFunctionVariables() {
        RuleContext context = RuleContext.builder()
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .additionalData(Map.of(
                        RuleContextKeys.FACT_KIND, "binding",
                        RuleContextKeys.CANDIDATE_MATCHED, true))
                .build();

        RuleMatchResult result = evaluator.evaluate(
                List.of(new TestRule("customFunctionRule", "#bindingFact", "#resolvedBinding", 10, Map.of())),
                context,
                Map.of(
                        "bindingFact", "#factKind == 'binding'",
                        "resolvedBinding", "#bindingFact and #candidateMatched"));

        assertTrue(result.isMatched());
        assertEquals("customFunctionRule", result.getMatchedRule());
    }

    @Test
    void shouldResolveDependentCustomFunctionVariablesRegardlessOfDeclarationOrder() {
        RuleContext context = RuleContext.builder()
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .superClassPath(List.of())
                .additionalData(Map.of(
                        RuleContextKeys.FACT_KIND, "binding",
                        RuleContextKeys.CANDIDATE_MATCHED, true))
                .build();

        Map<String, String> customFunctions = new LinkedHashMap<>();
        customFunctions.put("resolvedBinding", "#bindingFact and #candidateMatched");
        customFunctions.put("bindingFact", "#factKind == 'binding'");

        RuleMatchResult result = evaluator.evaluate(
                List.of(new TestRule("customFunctionRule", "#bindingFact", "#resolvedBinding", 10, Map.of())),
                context,
                customFunctions);

        assertTrue(result.isMatched());
        assertEquals("customFunctionRule", result.getMatchedRule());
    }

    @Test
    void shouldExposeMethodNameAndSuperClassPathVariablesAndSupportPropertyOnlyMutations() {
        RuleContext context = RuleContext.builder()
                .methodSignature("com.demo.UserService#findUser(java.lang.String)")
                .superClassPath(List.of("com.demo.BaseService"))
                .annotations(List.of())
                .classAnnotations(List.of())
                .implementedInterfaces(List.of())
                .additionalData(Map.of(RuleContextKeys.CANDIDATE_MATCHED, true))
                .build();

        RuleMatchResult result = evaluator.evaluate(List.of(
                new TestRule(
                        "propertyOnlyRule",
                        null,
                        "#candidateMatched and #methodName == 'findUser' and #superClassPath.contains('com.demo.BaseService')",
                        null,
                        EdgeCategory.FRAMEWORK,
                        10,
                        Map.of("resolution_kind", "'property-only'"))),
                context);

        assertTrue(result.isMatched());
        assertEquals("propertyOnlyRule", result.getMatchedRule());
        assertEquals(GraphMutation.MutationType.PROPERTY, result.getMutations().getFirst().getMutationType());
        assertEquals("property-only", result.getMutations().getFirst().getProperties().get("resolution_kind"));
    }

    private record TestRule(String name, String match, String when, String edgeType,
                            EdgeCategory edgeCategory, int priority, Map<String, Object> emitProperties)
            implements MutationRuleDefinition {
        private TestRule(String name, String when) {
            this(name, null, when, "instance_of", EdgeCategory.FRAMEWORK, 0, Map.of());
        }

        private TestRule(String name, String when, int priority, Map<String, Object> emitProperties) {
            this(name, null, when, "instance_of", EdgeCategory.FRAMEWORK, priority, emitProperties);
        }

        private TestRule(String name, String match, String when, int priority, Map<String, Object> emitProperties) {
            this(name, match, when, "instance_of", EdgeCategory.FRAMEWORK, priority, emitProperties);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getMatch() {
            return match;
        }

        @Override
        public String getWhen() {
            return when;
        }

        @Override
        public String getEdgeType() {
            return edgeType;
        }

        @Override
        public EdgeCategory getEdgeCategory() {
            return edgeCategory;
        }

        @Override
        public Integer getPriority() {
            return priority;
        }

        @Override
        public Map<String, Object> getEmitProperties() {
            return emitProperties;
        }
    }
}
