package com.puti.code.server.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class GraphEdge {
    private String id;
    private String source;
    private String target;
    private String type;
    private String category;
    private Map<String, Object> properties = new LinkedHashMap<>();
}
