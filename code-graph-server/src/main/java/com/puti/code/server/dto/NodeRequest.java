package com.puti.code.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class NodeRequest {
    @JsonProperty("method_full_name")
    private String methodFullName;

    @JsonProperty("query_type")
    private String queryType;

    @JsonProperty("path_depth")
    private Integer pathDepth = 1;

    @JsonProperty("edge_types")
    private List<String> edgeTypes;
}
