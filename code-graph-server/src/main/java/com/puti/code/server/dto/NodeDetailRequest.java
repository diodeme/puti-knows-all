package com.puti.code.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NodeDetailRequest {
    @JsonProperty("node_id")
    private String nodeId;
}
