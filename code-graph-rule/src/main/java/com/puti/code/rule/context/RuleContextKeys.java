package com.puti.code.rule.context;

/**
 * 规则上下文字段常量，避免规则 key 在业务代码中散落。
 */
public final class RuleContextKeys {

    public static final String FACT_KIND = "factKind";
    public static final String CANDIDATE_MATCHED = "candidateMatched";
    public static final String REQUESTED_NAME_PRESENT = "requestedNamePresent";
    public static final String PRIORITY_CANDIDATE_MATCHED = "priorityCandidateMatched";
    public static final String UNIQUE_CANDIDATE_MATCHED = "uniqueCandidateMatched";
    public static final String QUALIFIER_PRESENT = "qualifierPresent";
    public static final String REQUIRED_TYPE_NAME = "requiredTypeName";
    public static final String DECLARED_TARGET_TYPE_NAME = "declaredTargetTypeName";
    public static final String MEMBER_NAME = "memberName";
    public static final String MEMBER_KIND = "memberKind";
    public static final String INJECTION_MODE = "injectionMode";
    public static final String RESOLUTION_KIND = "resolutionKind";
    public static final String RESOLVED_TYPE_NAME = "resolvedTypeName";
    public static final String REQUESTED_NAME = "requestedName";

    private RuleContextKeys() {
    }
}
