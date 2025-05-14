package com.puti.code.analyzer.java.processor;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 统一的 provider 候选事实。
 */
@Value
@Builder
public class ProviderCandidate {
    String sourceId;
    String sourceKind;
    String declaredType;
    String actualType;
    String providerName;
    @Builder.Default
    List<String> qualifiers = List.of();
    @Builder.Default
    Integer priority = 0;
    String scope;
    @Builder.Default
    Map<String, Object> frameworkHints = Map.of();

    public boolean supportsType(String requiredType) {
        return Objects.equals(requiredType, declaredType) || Objects.equals(requiredType, actualType);
    }

    public boolean matchesName(String candidateName) {
        if (candidateName == null || candidateName.isBlank()) {
            return false;
        }
        if (candidateName.equals(providerName)) {
            return true;
        }
        return qualifiers.stream().anyMatch(candidateName::equals);
    }

    public boolean hasPriority() {
        return priority != null && priority > 0;
    }
}
