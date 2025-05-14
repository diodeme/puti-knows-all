package com.puti.code.server.service;

import com.puti.code.base.model.EdgeCategory;
import com.puti.code.base.model.EdgeDefinition;
import com.puti.code.base.model.EdgeDefinitionResolver;
import com.puti.code.base.util.ContentCompressor;
import com.puti.code.server.dao.GraphQueryDao;
import com.puti.code.server.dto.GraphEdge;
import com.puti.code.server.dto.GraphNode;
import com.puti.code.server.dto.GraphResponseMeta;
import com.puti.code.server.dto.MethodSearchResult;
import com.puti.code.server.dto.NodeDetail;
import com.puti.code.server.dto.NodeRequest;
import com.puti.code.server.dto.NodeResponse;
import com.puti.code.server.mapper.GraphQueryMapper;
import com.puti.code.repository.graph.query.GraphQueryEdge;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQuerySubgraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GraphQueryService {

    private final GraphQueryDao graphQueryDao;
    private final GraphQueryMapper graphQueryMapper;
    private final EdgeDefinitionResolver edgeDefinitionResolver = new EdgeDefinitionResolver();

    public List<MethodSearchResult> searchMethodByName(String methodName) {
        List<GraphQueryNode> result = graphQueryDao.searchMethodByName(methodName);
        List<MethodSearchResult> nodes = new ArrayList<>();
        for (GraphQueryNode node : result) {
            try {
                Map<String, Object> properties = node.getProperties();
                String fullName = getStringProperty(properties, "full_name", "");
                if (fullName == null || fullName.isBlank()) {
                    continue;
                }
                GraphQueryRecord.MethodSearchRecord searchRecord = new GraphQueryRecord.MethodSearchRecord(
                        node.getId(),
                        getStringProperty(properties, "name", ""),
                        fullName,
                        getStringProperty(properties, "type", node.getTag()),
                        getStringProperty(properties, "visibility", "public"),
                        getStringProperty(properties, "branch_name", ""),
                        getStringProperty(properties, "repo_id", ""));
                nodes.add(graphQueryMapper.toMethodSearchResult(searchRecord));
            } catch (Exception e) {
                log.error("Error parsing method search result", e);
            }
        }
        return nodes;
    }

    public NodeResponse getNodes(NodeRequest request) {
        String type = request.getQueryType();
        if ("self".equals(type)) {
            return getSelfNode(request.getMethodFullName());
        }
        if ("upstream".equals(type)) {
            return getSubgraph(request.getMethodFullName(), request.getPathDepth(), "IN");
        }
        if ("downstream".equals(type)) {
            return getSubgraph(request.getMethodFullName(), request.getPathDepth(), "OUT");
        }
        throw new IllegalArgumentException("Unknown query type: " + type);
    }

    private NodeResponse getSelfNode(String methodFullName) {
        Optional<GraphQueryNode> result = graphQueryDao.findFunctionByFullName(methodFullName);
        NodeResponse response = emptyNodeResponse();

        result.map(node -> toGraphNodeRecord(node, node.getId()))
                .ifPresent(graphNodeRecord -> {
                    response.getNodes().add(graphQueryMapper.toGraphNode(graphNodeRecord));
                    response.getEdges().add(graphQueryMapper.toGraphEdge(new GraphQueryRecord.GraphEdgeRecord(
                            graphNodeRecord.id(),
                            graphNodeRecord.id(),
                            "self_reference",
                            "STRUCTURAL",
                            Map.of("type", "self_reference"))));
                });
        enrichResponseMeta(response, Map.of("queryType", "self", "methodFullName", methodFullName));
        return response;
    }

    private NodeResponse getSubgraph(String methodFullName, Integer pathDepth, String direction) {
        int normalizedDepth = normalizePathDepth(pathDepth);
        Map<String, Object> queryInfo = new LinkedHashMap<>();
        queryInfo.put("queryType", direction);
        queryInfo.put("pathDepth", pathDepth != null ? pathDepth : 1);
        queryInfo.put("resolvedPathDepth", normalizedDepth);
        queryInfo.put("methodFullName", methodFullName);
        Optional<String> vidResult = graphQueryDao.findFunctionIdByFullName(methodFullName);
        if (vidResult.isEmpty()) {
            NodeResponse response = emptyNodeResponse();
            enrichResponseMeta(response, queryInfo);
            return response;
        }

        String startVid = vidResult.get();
        GraphQuerySubgraph subgraphResult = graphQueryDao.getSubgraph(startVid, normalizedDepth, direction);
        Map<String, GraphNode> allNodes = new LinkedHashMap<>();
        List<GraphEdge> allEdges = new ArrayList<>();

        for (GraphQueryNode node : subgraphResult.getNodes()) {
            GraphQueryRecord.GraphNodeRecord graphNodeRecord = toGraphNodeRecord(node, startVid);
            if (graphNodeRecord != null) {
                allNodes.put(graphNodeRecord.id(), graphQueryMapper.toGraphNode(graphNodeRecord));
            }
        }
        for (GraphQueryEdge edge : subgraphResult.getEdges()) {
            Map<String, Object> edgeProperties = new LinkedHashMap<>(edge.getProperties());
            String edgeType = edge.getType();
            String category = edge.getCategory() != null ? edge.getCategory() : resolveEdgeCategory(edgeType, edgeProperties);
            edgeProperties.putIfAbsent("type", edgeType);
            edgeProperties.putIfAbsent("category", category);
            GraphQueryRecord.GraphEdgeRecord edgeRecord = new GraphQueryRecord.GraphEdgeRecord(
                    edge.getSource(),
                    edge.getTarget(),
                    edgeType,
                    category,
                    edgeProperties);
            allEdges.add(graphQueryMapper.toGraphEdge(edgeRecord));
        }

        NodeResponse response = new NodeResponse();
        response.setNodes(new ArrayList<>(allNodes.values()));
        response.setEdges(allEdges);
        enrichResponseMeta(response, queryInfo);
        return response;
    }

    public NodeDetail getNodeDetail(String nodeId) {
        return graphQueryDao.getNodeDetail(nodeId)
                .map(this::toNodeDetail)
                .orElse(null);
    }

    private NodeResponse emptyNodeResponse() {
        NodeResponse response = new NodeResponse();
        response.setNodes(new ArrayList<>());
        response.setEdges(new ArrayList<>());
        response.setMeta(new GraphResponseMeta());
        return response;
    }

    private int normalizePathDepth(Integer pathDepth) {
        if (pathDepth == null || (pathDepth <= 0 && pathDepth != -1)) {
            return 1;
        }
        if (pathDepth == -1) {
            return 10;
        }
        return pathDepth;
    }

    GraphQueryRecord.GraphNodeRecord toGraphNodeRecord(GraphQueryNode node, String startVid) {
        if (node == null || node.getId() == null) {
            return null;
        }
        String vertexId = node.getId();
        String primaryTag = node.getTag();
        if (primaryTag == null || primaryTag.isBlank()) {
            return null;
        }
        Map<String, Object> props = node.getProperties() != null ? node.getProperties() : Collections.emptyMap();
        String nodeType = normalizeNodeType(primaryTag, props);
        String fullName = firstNonBlank(props, "full_name", "qualified_name", "fullName");
        String label = firstNonBlank(props, "name", "label", "simple_name", "full_name", "qualified_name");
        if (label == null || label.isBlank()) {
            label = vertexId;
        }
        boolean sourceNode = vertexId.equals(startVid);
        boolean daoNode = isDaoNode(fullName);

        return new GraphQueryRecord.GraphNodeRecord(
                vertexId,
                label,
                fullName != null ? fullName : label,
                nodeType,
                firstNonBlank(props, "visibility"),
                normalizeIsLibrary(props.get("is_library")),
                sourceNode,
                daoNode,
                buildNodeProperties(vertexId, nodeType, primaryTag, props, label, fullName, sourceNode, daoNode));
    }

    private String firstNonBlank(Map<String, Object> properties, String... keys) {
        for (String key : keys) {
            Object value = properties.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private boolean isDaoNode(String fullName) {
        return fullName != null && fullName.toLowerCase().contains("dao");
    }

    private Boolean normalizeIsLibrary(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String decompressContent(String compressedBase64) {
        if (compressedBase64 == null || compressedBase64.isEmpty() || "null".equals(compressedBase64)) {
            return null;
        }

        String decompressed = ContentCompressor.decompress(compressedBase64);
        if (decompressed == null || decompressed.isEmpty()) {
            log.warn("Decompression returned empty content, fallback to raw string");
            return compressedBase64;
        }
        return decompressed;
    }


    private Map<String, Object> buildNodeProperties(String nodeId, String nodeType, Map<String, Object> props,
                                                    String label, String fullName, boolean sourceNode,
                                                    boolean daoNode) {
        return buildNodeProperties(nodeId, nodeType, nodeType, props, label, fullName, sourceNode, daoNode);
    }

    private Map<String, Object> buildNodeProperties(String nodeId, String nodeType, String rawTag,
                                                    Map<String, Object> props, String label, String fullName,
                                                    boolean sourceNode, boolean daoNode) {
        Map<String, Object> normalized = new LinkedHashMap<>(props);
        normalized.put("id", nodeId);
        normalized.put("node_type", nodeType);
        normalized.put("tag", rawTag);
        normalized.put("label", label);
        normalized.put("full_name", fullName != null ? fullName : props.getOrDefault("full_name", ""));
        normalized.put("is_library", normalizeIsLibrary(props.get("is_library")));
        normalized.put("source_node", sourceNode);
        normalized.put("dao_node", daoNode);
        return normalized;
    }

    private String normalizeNodeType(String tagName, Map<String, Object> props) {
        if (tagName == null || tagName.isBlank()) {
            return "unknown";
        }
        if ("marker_annotations".equals(tagName) || "marker_annotation".equals(tagName)) {
            return "marker_annotation";
        }
        if ("annotations".equals(tagName) || "annotation".equals(tagName)) {
            Object annotationKind = props != null ? props.get("type") : null;
            String normalizedKind = annotationKind != null ? String.valueOf(annotationKind) : "";
            if ("MARKER_ANNOTATION".equalsIgnoreCase(normalizedKind)
                    || "marker_annotation".equalsIgnoreCase(normalizedKind)
                    || "marker_annotations".equalsIgnoreCase(normalizedKind)) {
                return "marker_annotation";
            }
            return "annotation";
        }
        return tagName;
    }

    String resolveEdgeCategory(String edgeType, Map<String, Object> edgeProperties) {
        if (edgeProperties != null) {
            Object category = edgeProperties.get("category");
            if (category != null && !String.valueOf(category).isBlank()) {
                return String.valueOf(category);
            }
        }
        if (edgeType == null || edgeType.isBlank()) {
            return EdgeCategory.SEMANTIC.name();
        }
        EdgeDefinition edgeDefinition = edgeDefinitionResolver.resolve(edgeType, EdgeCategory.SEMANTIC);
        return edgeDefinition.getCategory().name();
    }

    private String getStringProperty(Map<String, Object> properties, String key, String defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        Object value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }
        String stringValue = String.valueOf(value);
        return stringValue.isBlank() ? defaultValue : stringValue;
    }

    void enrichResponseMeta(NodeResponse response, Map<String, Object> queryInfo) {
        GraphResponseMeta meta = new GraphResponseMeta();
        Map<String, Integer> nodeTypeStats = new LinkedHashMap<>();
        for (GraphNode node : response.getNodes()) {
            String type = node.getType() != null ? node.getType() : String.valueOf(node.getProperties().getOrDefault("type", "unknown"));
            nodeTypeStats.merge(type, 1, Integer::sum);
        }
        Map<String, Integer> edgeTypeStats = new LinkedHashMap<>();
        Map<String, String> legend = new LinkedHashMap<>();
        Map<String, String> edgeTypeLabels = new LinkedHashMap<>();
        Map<String, String> edgeCategoryLabels = new LinkedHashMap<>();
        for (GraphEdge edge : response.getEdges()) {
            String type = edge.getType() != null ? edge.getType() : String.valueOf(edge.getProperties().getOrDefault("type", "unknown"));
            edgeTypeStats.merge(type, 1, Integer::sum);
            legend.putIfAbsent(type, edge.getCategory());
            edgeTypeLabels.putIfAbsent(type, resolveEdgeDisplayName(type));
            if (edge.getCategory() != null && !edge.getCategory().isBlank()) {
                edgeCategoryLabels.putIfAbsent(edge.getCategory(), resolveEdgeCategoryDisplayName(edge.getCategory()));
            }
        }
        meta.setNodeTypeStats(nodeTypeStats);
        meta.setEdgeTypeStats(edgeTypeStats);
        meta.setLegend(legend);
        meta.setEdgeTypeLabels(edgeTypeLabels);
        meta.setEdgeCategoryLabels(edgeCategoryLabels);
        meta.setQueryInfo(new LinkedHashMap<>(queryInfo));
        response.setMeta(meta);
    }

    private String resolveEdgeDisplayName(String edgeType) {
        if (edgeType == null || edgeType.isBlank()) {
            return "unknown";
        }
        EdgeDefinition edgeDefinition = edgeDefinitionResolver.resolve(edgeType, EdgeCategory.SEMANTIC);
        String displayName = edgeDefinition.getDisplayName();
        return displayName != null && !displayName.isBlank() ? displayName : edgeType;
    }

    private String resolveEdgeCategoryDisplayName(String category) {
        if (category == null || category.isBlank()) {
            return "unknown";
        }
        return switch (category) {
            case "STRUCTURAL" -> "结构";
            case "SEMANTIC" -> "语义";
            case "FRAMEWORK" -> "框架";
            case "DATA_FLOW" -> "数据流";
            default -> category;
        };
    }

    private NodeDetail toNodeDetail(GraphQueryNode node) {
        GraphQueryRecord.GraphNodeRecord record = toGraphNodeRecord(node, "");
        if (record == null) {
            return null;
        }

        NodeDetail detail = new NodeDetail();
        detail.setId(record.id());
        detail.setName(record.name());
        detail.setFullName(record.fullName());

        Map<String, Object> rawProperties = new LinkedHashMap<>(record.properties());
        Object content = rawProperties.remove("content");
        detail.setRawProperties(rawProperties);
        if (content instanceof String encodedContent) {
            detail.setContent(decompressContent(encodedContent));
        }
        return detail;
    }
}

