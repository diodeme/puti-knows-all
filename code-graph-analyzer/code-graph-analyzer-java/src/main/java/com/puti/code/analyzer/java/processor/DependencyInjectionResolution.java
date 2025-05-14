package com.puti.code.analyzer.java.processor;

import lombok.Builder;
import lombok.Value;

/**
 * 注入点解析结果。
 */
@Value
@Builder
public class DependencyInjectionResolution {
    ProviderCandidate candidate;
    String resolutionKind;
    boolean candidateMatched;
    boolean requestedNamePresent;
    boolean priorityCandidateMatched;
    boolean uniqueCandidateMatched;

    public String getResolvedTypeName() {
        return candidate != null ? candidate.getActualType() : null;
    }
}
