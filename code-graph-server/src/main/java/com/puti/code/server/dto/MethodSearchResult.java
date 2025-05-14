package com.puti.code.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MethodSearchResult {

    @JsonProperty("node_id")
    private String nodeId;

    private String id;
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    private String type;
    private String visibility;

    @JsonProperty("branch_name")
    private String branchName;

    @JsonProperty("repo_id")
    private String repoId;
}
