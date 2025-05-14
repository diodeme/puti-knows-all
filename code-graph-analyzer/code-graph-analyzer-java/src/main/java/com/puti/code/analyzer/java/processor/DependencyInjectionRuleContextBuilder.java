package com.puti.code.analyzer.java.processor;

import com.puti.code.rule.context.RuleContext;
import com.puti.code.rule.context.RuleFactAdapter;
import com.puti.code.rule.context.RuleContextKeys;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将依赖注入事实转换为规则上下文。
 */
public final class DependencyInjectionRuleContextBuilder implements RuleFactAdapter<DependencyInjectionFact> {

    @Override
    public RuleContext adapt(DependencyInjectionFact fact) {
        Map<String, Object> additionalData = new LinkedHashMap<>();
        additionalData.put(RuleContextKeys.FACT_KIND, fact.factKind());
        additionalData.put(RuleContextKeys.CANDIDATE_MATCHED, fact.candidateMatched());
        additionalData.put(RuleContextKeys.REQUESTED_NAME_PRESENT, fact.requestedNamePresent());
        additionalData.put(RuleContextKeys.PRIORITY_CANDIDATE_MATCHED, fact.priorityCandidateMatched());
        additionalData.put(RuleContextKeys.UNIQUE_CANDIDATE_MATCHED, fact.uniqueCandidateMatched());
        additionalData.put(RuleContextKeys.QUALIFIER_PRESENT, fact.qualifierPresent());
        additionalData.put(RuleContextKeys.REQUIRED_TYPE_NAME, fact.requiredTypeName());
        additionalData.put(RuleContextKeys.DECLARED_TARGET_TYPE_NAME, fact.declaredTargetTypeName());
        additionalData.put(RuleContextKeys.MEMBER_NAME, fact.memberName());
        additionalData.put(RuleContextKeys.MEMBER_KIND, fact.memberKind());
        additionalData.put(RuleContextKeys.INJECTION_MODE, fact.injectionMode());
        additionalData.put(RuleContextKeys.RESOLUTION_KIND, fact.resolutionKind());
        additionalData.put(RuleContextKeys.RESOLVED_TYPE_NAME, fact.resolvedTypeName());
        additionalData.put(RuleContextKeys.REQUESTED_NAME, fact.requestedName());

        return RuleContext.builder()
                .methodName(resolveMethodName(fact))
                .ownerTypeName(fact.ownerTypeName())
                .methodSignature(fact.methodSignature())
                .returnTypeName(fact.returnTypeName())
                .annotations(fact.annotations() != null ? fact.annotations() : List.of())
                .classAnnotations(fact.classAnnotations() != null ? fact.classAnnotations() : List.of())
                .implementedInterfaces(fact.implementedInterfaces() != null ? fact.implementedInterfaces() : List.of())
                .superClassPath(fact.superClassPath() != null ? fact.superClassPath() : List.of())
                .annotationAttributes(fact.annotationAttributes() != null ? fact.annotationAttributes() : Map.of())
                .parameterTypes(fact.parameterTypes() != null ? fact.parameterTypes() : List.of())
                .additionalData(additionalData)
                .build();
    }

    private String resolveMethodName(DependencyInjectionFact fact) {
        if (fact.annotationAttributes() != null) {
            Object methodName = fact.annotationAttributes().get("methodName");
            if (methodName != null && !String.valueOf(methodName).isBlank()) {
                return String.valueOf(methodName);
            }
        }
        if (fact.methodSignature() == null || fact.methodSignature().isBlank()) {
            return null;
        }
        String signature = fact.methodSignature();
        int hashIndex = signature.lastIndexOf('#');
        String tail = hashIndex >= 0 ? signature.substring(hashIndex + 1) : signature;
        int parameterIndex = tail.indexOf('(');
        return parameterIndex >= 0 ? tail.substring(0, parameterIndex) : tail;
    }
}
