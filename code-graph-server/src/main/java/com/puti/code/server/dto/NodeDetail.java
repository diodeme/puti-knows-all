package com.puti.code.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
public class NodeDetail {
    private String id;
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    private String content;

    @JsonProperty("raw_properties")
    private Map<String, Object> rawProperties;
}
