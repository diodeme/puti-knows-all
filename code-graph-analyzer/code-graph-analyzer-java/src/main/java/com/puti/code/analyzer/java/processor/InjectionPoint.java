package com.puti.code.analyzer.java.processor;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * 统一的注入点事实。
 */
@Value
@Builder
public class InjectionPoint {
    String ownerId;
    String ownerTypeName;
    String memberName;
    String memberKind;
    String requiredType;
    String requestedName;
    @Builder.Default
    List<String> qualifiers = List.of();
    @Builder.Default
    boolean required = true;
    String injectionMode;
    @Builder.Default
    Map<String, Object> frameworkHints = Map.of();

    public String preferredName() {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName;
        }
        return qualifiers.stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
