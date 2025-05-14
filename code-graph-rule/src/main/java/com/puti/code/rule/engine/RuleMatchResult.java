package com.puti.code.rule.engine;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * 规则匹配结果。
 */
@Value
@Builder
public class RuleMatchResult {
    boolean matched;
    String matchedRule;
    @Singular
    List<GraphMutation> mutations;
}
