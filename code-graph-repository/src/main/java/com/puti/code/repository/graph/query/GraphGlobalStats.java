package com.puti.code.repository.graph.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphGlobalStats {
    private long totalNodes;
    private long totalEdges;
    private Map<String, Long> nodeTypeStats;
    private Map<String, Long> edgeTypeStats;
    private long entryPointCount;
}
