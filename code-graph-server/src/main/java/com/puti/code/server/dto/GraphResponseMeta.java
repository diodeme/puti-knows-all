package com.puti.code.server.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class GraphResponseMeta {
    private Map<String, Integer> nodeTypeStats = new LinkedHashMap<>();
    private Map<String, Integer> edgeTypeStats = new LinkedHashMap<>();
    private Map<String, String> legend = new LinkedHashMap<>();
    private Map<String, String> edgeTypeLabels = new LinkedHashMap<>();
    private Map<String, String> edgeCategoryLabels = new LinkedHashMap<>();
    private Map<String, Object> queryInfo = new LinkedHashMap<>();
}
