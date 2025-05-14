package com.puti.code.analyzer.java.processor;

import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeDefinitionResolver;
import com.puti.code.rule.context.RuleFactAdapter;
import com.puti.code.rule.engine.GraphMutation;
import com.puti.code.rule.engine.DependencyInjectionRuleEvaluator;
import com.puti.code.rule.engine.RuleMatchResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 将依赖注入事实转成图关系。
 */
public class DependencyInjectionEnhancer {

    private final DependencyInjectionRuleEvaluator evaluator = new DependencyInjectionRuleEvaluator();
    private final RuleFactAdapter<DependencyInjectionFact> factAdapter = new DependencyInjectionRuleContextBuilder();
    private final EdgeDefinitionResolver edgeDefinitionResolver = new EdgeDefinitionResolver();

    public Optional<Edge> createBindingEdge(DependencyInjectionFact fact, String srcId, String dstId) {
        return createEdge(fact, srcId, dstId);
    }

    public Optional<Edge> createInjectionCallEdge(DependencyInjectionFact fact, String srcId, String dstId) {
        return createEdge(fact, srcId, dstId);
    }

    private Optional<Edge> createEdge(DependencyInjectionFact fact, String srcId, String dstId) {
        RuleMatchResult result = evaluator.evaluate(factAdapter.adapt(fact));
        if (!result.isMatched() || result.getMutations() == null) {
            return Optional.empty();
        }

        return result.getMutations().stream()
                .filter(mutation -> mutation.getMutationType() == GraphMutation.MutationType.EDGE)
                .findFirst()
                .map(mutation -> Edge.builder()
                        .srcId(srcId)
                        .dstId(dstId)
                        .type(edgeDefinitionResolver.resolve(mutation.getEdgeType(), mutation.getEdgeCategory()))
                        .properties(new LinkedHashMap<>(safeProperties(mutation)))
                        .lineNumber(fact.lineNumber())
                        .build());
    }

    private Map<String, Object> safeProperties(GraphMutation mutation) {
        return mutation.getProperties() == null ? Map.of() : mutation.getProperties();
    }
}
