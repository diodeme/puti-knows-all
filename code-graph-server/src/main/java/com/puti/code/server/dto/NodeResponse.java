package com.puti.code.server.dto;

import lombok.Data;
import java.util.List;

@Data
public class NodeResponse {
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
    private GraphResponseMeta meta;
}
