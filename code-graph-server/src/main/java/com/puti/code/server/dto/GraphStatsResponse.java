package com.puti.code.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class GraphStatsResponse {
    @JsonProperty("total_nodes")
    private long totalNodes;

    @JsonProperty("total_edges")
    private long totalEdges;

    @JsonProperty("node_type_stats")
    private Map<String, Long> nodeTypeStats;

    @JsonProperty("edge_type_stats")
    private Map<String, Long> edgeTypeStats;

    @JsonProperty("entry_point_count")
    private long entryPointCount;

    @JsonProperty("repo_count")
    private long repoCount;
}
