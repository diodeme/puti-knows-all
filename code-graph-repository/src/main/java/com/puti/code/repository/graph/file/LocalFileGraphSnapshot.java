package com.puti.code.repository.graph.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地图快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalFileGraphSnapshot {

    @Builder.Default
    private Map<String, LocalFileGraphNodeRecord> nodesById = new LinkedHashMap<>();
    @Builder.Default
    private List<LocalFileGraphEdgeRecord> edges = new ArrayList<>();
    @Builder.Default
    private Map<String, List<LocalFileGraphEdgeRecord>> outgoingEdges = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, List<LocalFileGraphEdgeRecord>> incomingEdges = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, LocalFileGraphNodeRecord> functionsByFullName = new LinkedHashMap<>();
    @Builder.Default
    private List<LocalFileGraphNodeRecord> functionNodes = new ArrayList<>();
}
