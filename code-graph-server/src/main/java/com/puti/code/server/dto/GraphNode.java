package com.puti.code.server.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class GraphNode {
    private String id;
    private String type;
    private String label;
    private Map<String, Object> properties = new LinkedHashMap<>();
}
