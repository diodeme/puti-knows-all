package com.puti.code.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EntryPointsRequest {

    @JsonProperty("repo_id")
    private String repoId;

    @JsonProperty("branch_name")
    private String branchName;
}
