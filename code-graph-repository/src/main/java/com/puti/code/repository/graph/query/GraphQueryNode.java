package com.puti.code.repository.graph.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用图查询节点。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQueryNode {

    private String id;
    private String tag;
    @Builder.Default
    private Map<String, Object> properties = new LinkedHashMap<>();
}
