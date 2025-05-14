package com.puti.code.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NodeRequest {
    @JsonProperty("method_full_name")
    private String methodFullName;

    @JsonProperty("query_type")
    private String queryType;

    @JsonProperty("path_depth")
    private Integer pathDepth = 1;
}
