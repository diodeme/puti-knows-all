package com.puti.code.rule.engine;

import com.puti.code.base.model.EdgeCategory;

import java.util.Map;

/**
 * 可产生图谱变更的规则定义。
 */
public interface MutationRuleDefinition {

    String getName();

    default String getMatch() {
        return null;
    }

    String getWhen();

    String getEdgeType();

    EdgeCategory getEdgeCategory();

    default Integer getPriority() {
        return 0;
    }

    default Map<String, Object> getEmitProperties() {
        return Map.of();
    }
}
