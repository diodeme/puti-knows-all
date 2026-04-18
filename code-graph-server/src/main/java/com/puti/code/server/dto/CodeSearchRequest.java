package com.puti.code.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class CodeSearchRequest {

    private String query;

    @JsonProperty("top_k")
    private Integer topK = 5;

    @JsonProperty("strategy")
    private String strategy = "weighted";

    @JsonProperty("type_weights")
    private Map<String, Float> typeWeights;

    @JsonProperty("include_context")
    private Boolean includeContext = true;

    @JsonProperty("context_depth")
    private Integer contextDepth = 1;

    @JsonProperty("repo_id")
    private String repoId;

    @JsonProperty("branch_name")
    private String branchName;
}
