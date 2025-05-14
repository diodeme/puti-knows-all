package com.puti.code.repository.graph.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用子图查询结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQuerySubgraph {

    @Builder.Default
    private List<GraphQueryNode> nodes = new ArrayList<>();
    @Builder.Default
    private List<GraphQueryEdge> edges = new ArrayList<>();
}
