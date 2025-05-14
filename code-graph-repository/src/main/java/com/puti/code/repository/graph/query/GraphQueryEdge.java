package com.puti.code.repository.graph.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用图查询边。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQueryEdge {

    private String source;
    private String target;
    private String type;
    private String category;
    @Builder.Default
    private Map<String, Object> properties = new LinkedHashMap<>();
}
