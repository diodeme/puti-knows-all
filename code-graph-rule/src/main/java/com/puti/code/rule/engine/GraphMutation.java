package com.puti.code.rule.engine;

import com.puti.code.base.model.EdgeCategory;
import lombok.Builder;
import lombok.Value;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 规则执行后产出的图谱变更。
 */
@Value
@Builder
public class GraphMutation {
    MutationType mutationType;
    String edgeType;
    EdgeCategory edgeCategory;
    @Builder.Default
    Map<String, Object> properties = new LinkedHashMap<>();

    public enum MutationType {
        EDGE,
        PROPERTY
    }
}
