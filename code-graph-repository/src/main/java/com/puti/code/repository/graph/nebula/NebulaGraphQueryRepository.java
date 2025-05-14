package com.puti.code.repository.graph.nebula;

import com.puti.code.base.model.EdgeCategory;
import com.puti.code.base.model.EdgeDefinition;
import com.puti.code.base.model.EdgeDefinitionResolver;
import com.puti.code.base.model.EdgeSchema;
import com.puti.code.base.model.EdgeSchemaRegistry;
import com.puti.code.repository.graph.query.GraphDirection;
import com.puti.code.repository.graph.query.GraphQueryEdge;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import com.puti.code.repository.graph.query.GraphQuerySubgraph;
import com.puti.code.repository.nebula.NebulaGraphClient;
import com.vesoft.nebula.client.graph.data.Node;
import com.vesoft.nebula.client.graph.data.Relationship;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Nebula 图查询实现。
 */
@Slf4j
public class NebulaGraphQueryRepository implements GraphQueryRepository {

    private final NebulaGraphClient nebulaGraphClient;
    private final EdgeSchemaRegistry edgeSchemaRegistry = EdgeSchemaRegistry.getInstance();
    private final EdgeDefinitionResolver edgeDefinitionResolver = new EdgeDefinitionResolver(edgeSchemaRegistry);
    private volatile List<String> cachedSubgraphEdgeTypes = List.of();

    public NebulaGraphQueryRepository() {
        this(new NebulaGraphClient());
    }

    public NebulaGraphQueryRepository(NebulaGraphClient nebulaGraphClient) {
        this.nebulaGraphClient = nebulaGraphClient;
    }

    @Override
    public List<GraphQueryNode> searchMethodByName(String methodName) {
        String escapedMethodName = escapeNebulaString(methodName);
        String query = String.format(
                "MATCH (v:function) WHERE v.function.full_name STARTS WITH \"%s\" OR v.function.name == \"%s\" "
                        + "RETURN v LIMIT 100",
                escapedMethodName, escapedMethodName);
        ResultSet result = nebulaGraphClient.execute(query);
        List<GraphQueryNode> nodes = new ArrayList<>();
        if (result == null || !result.isSucceeded() || result.getRows() == null) {
            return nodes;
        }
        for (int i = 0; i < result.getRows().size(); i++) {
            try {
                GraphQueryNode node = toQueryNode(result.rowValues(i).values().get(0).asNode());
                if (node != null) {
                    nodes.add(node);
                }
            } catch (Exception e) {
                log.debug("Failed to parse method search row {}", i, e);
            }
        }
        return nodes;
    }

    @Override
    public Optional<GraphQueryNode> findFunctionByFullName(String methodFullName) {
        String escapedMethodFullName = escapeNebulaString(methodFullName);
        String query = String.format("MATCH (v:function) WHERE v.function.full_name == \"%s\" RETURN v LIMIT 1",
                escapedMethodFullName);
        ResultSet result = nebulaGraphClient.execute(query);
        return readFirstNode(result);
    }

    @Override
    public Optional<String> findFunctionIdByFullName(String methodFullName) {
        String escapedMethodFullName = escapeNebulaString(methodFullName);
        String query = String.format("MATCH (v:function) WHERE v.function.full_name == \"%s\" RETURN id(v) as vid LIMIT 1",
                escapedMethodFullName);
        ResultSet result = nebulaGraphClient.execute(query);
        if (result == null || !result.isSucceeded() || result.getRows() == null || result.getRows().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(result.rowValues(0).values().get(0).asString());
        } catch (Exception e) {
            log.debug("Failed to parse function vid for {}", methodFullName, e);
            return Optional.empty();
        }
    }

    @Override
    public GraphQuerySubgraph getSubgraph(String startNodeId, int pathDepth, GraphDirection direction, List<String> edgeTypes) {
        String traversalClause = buildTraversalClause(direction, edgeTypes == null || edgeTypes.isEmpty()
                ? resolveSubgraphEdgeTypes()
                : edgeTypes);
        String query = String.format(
                "GET SUBGRAPH WITH PROP %d STEPS FROM \"%s\"%s YIELD VERTICES AS nodes, EDGES AS relationships",
                pathDepth,
                escapeNebulaString(startNodeId),
                traversalClause);
        ResultSet result = nebulaGraphClient.execute(query);
        if (result == null || !result.isSucceeded() || result.getRows() == null) {
            return GraphQuerySubgraph.builder().build();
        }

        Map<String, GraphQueryNode> nodes = new LinkedHashMap<>();
        List<GraphQueryEdge> edges = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < result.getRows().size(); rowIndex++) {
            ResultSet.Record row = result.rowValues(rowIndex);
            try {
                for (ValueWrapper vertexWrapper : row.values().get(0).asList()) {
                    GraphQueryNode node = toQueryNode(vertexWrapper.asNode());
                    if (node != null) {
                        nodes.put(node.getId(), node);
                    }
                }
                for (ValueWrapper edgeWrapper : row.values().get(1).asList()) {
                    GraphQueryEdge edge = toQueryEdge(edgeWrapper.asRelationship());
                    if (edge != null) {
                        edges.add(edge);
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse subgraph row {}", rowIndex, e);
            }
        }
        return GraphQuerySubgraph.builder()
                .nodes(new ArrayList<>(nodes.values()))
                .edges(edges)
                .build();
    }

    @Override
    public Optional<GraphQueryNode> findNodeById(String nodeId) {
        String query = String.format("MATCH (v) WHERE id(v) == \"%s\" RETURN v AS node LIMIT 1",
                escapeNebulaString(nodeId));
        ResultSet result = nebulaGraphClient.execute(query);
        return readFirstNode(result);
    }

    @Override
    public List<GraphQueryNode> getEntryPoints(String projectId, String branchName) {
        String query = String.format("""
                MATCH (v:function{is_entry_point:TRUE})
                WHERE v.function.repo_id == \"%s\" AND v.function.branch_name == \"%s\"
                RETURN v
                """, escapeNebulaString(projectId), escapeNebulaString(branchName));
        ResultSet result = nebulaGraphClient.execute(query);
        List<GraphQueryNode> nodes = new ArrayList<>();
        if (result == null || !result.isSucceeded() || result.getRows() == null) {
            return nodes;
        }
        for (int i = 0; i < result.getRows().size(); i++) {
            try {
                GraphQueryNode node = toQueryNode(result.rowValues(i).values().get(0).asNode());
                if (node != null) {
                    nodes.add(node);
                }
            } catch (Exception e) {
                log.debug("Failed to parse entry point row {}", i, e);
            }
        }
        return nodes;
    }

    @Override
    public long countEntryPoints() {
        ResultSet result = nebulaGraphClient.execute("MATCH (v:function{is_entry_point:TRUE}) RETURN count(v) as count");
        if (result == null || !result.isSucceeded() || result.getRows() == null || result.getRows().isEmpty()) {
            return 0L;
        }
        try {
            return result.rowValues(0).values().get(0).asLong();
        } catch (Exception e) {
            log.debug("Failed to parse entry point count", e);
            return 0L;
        }
    }

    private Optional<GraphQueryNode> readFirstNode(ResultSet result) {
        if (result == null || !result.isSucceeded() || result.getRows() == null || result.getRows().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(toQueryNode(result.rowValues(0).values().get(0).asNode()));
        } catch (Exception e) {
            log.debug("Failed to parse first node", e);
            return Optional.empty();
        }
    }

    private GraphQueryNode toQueryNode(Node vertex) {
        if (vertex == null || vertex.getId() == null) {
            return null;
        }
        String vertexId;
        try {
            vertexId = vertex.getId().asString();
        } catch (Exception e) {
            return null;
        }
        String primaryTag = resolvePrimaryTag(vertex);
        if (primaryTag == null) {
            return null;
        }
        return GraphQueryNode.builder()
                .id(vertexId)
                .tag(primaryTag)
                .properties(extractVertexProperties(vertex, primaryTag))
                .build();
    }

    private GraphQueryEdge toQueryEdge(Relationship relationship) {
        if (relationship == null) {
            return null;
        }
        String sourceId;
        String targetId;
        try {
            sourceId = relationship.srcId().asString();
            targetId = relationship.dstId().asString();
        } catch (UnsupportedEncodingException e) {
            log.debug("Failed to parse relationship endpoints for {}", relationship.edgeName(), e);
            return null;
        }
        Map<String, Object> properties = extractRelationshipProperties(relationship);
        String edgeType = relationship.edgeName();
        String category = resolveEdgeCategory(edgeType, properties);
        properties.putIfAbsent("type", edgeType);
        properties.putIfAbsent("category", category);
        return GraphQueryEdge.builder()
                .source(sourceId)
                .target(targetId)
                .type(edgeType)
                .category(category)
                .properties(properties)
                .build();
    }

    private Map<String, Object> extractVertexProperties(Node vertex, String tagName) {
        try {
            Map<String, ValueWrapper> tagProperties = vertex.properties(tagName);
            Map<String, Object> props = new LinkedHashMap<>();
            for (Map.Entry<String, ValueWrapper> entry : tagProperties.entrySet()) {
                props.put(entry.getKey(), unwrapValue(entry.getValue()));
            }
            return props;
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return Map.of();
        }
    }

    private String resolvePrimaryTag(Node vertex) {
        if (vertex == null || vertex.tagNames() == null || vertex.tagNames().isEmpty()) {
            return null;
        }
        return vertex.tagNames().stream()
                .filter(Objects::nonNull)
                .filter(tag -> !tag.isBlank())
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> extractRelationshipProperties(Relationship relationship) {
        Map<String, Object> properties = new LinkedHashMap<>();
        try {
            relationship.properties().forEach((key, value) -> properties.put(key, unwrapValue(value)));
        } catch (Exception e) {
            log.debug("Failed to extract relationship properties for {}", relationship.edgeName(), e);
        }
        return properties;
    }

    private Object unwrapValue(ValueWrapper valueWrapper) {
        if (valueWrapper == null || valueWrapper.isNull()) {
            return null;
        }
        try {
            if (valueWrapper.isString()) {
                return valueWrapper.asString();
            }
            if (valueWrapper.isLong()) {
                return valueWrapper.asLong();
            }
            if (valueWrapper.isBoolean()) {
                return valueWrapper.asBoolean();
            }
            if (valueWrapper.isDouble()) {
                return valueWrapper.asDouble();
            }
        } catch (Exception ignored) {
        }
        return valueWrapper.toString();
    }

    private String resolveEdgeCategory(String edgeType, Map<String, Object> edgeProperties) {
        Object category = edgeProperties.get("category");
        if (category != null && !String.valueOf(category).isBlank()) {
            return String.valueOf(category);
        }
        if (edgeType == null || edgeType.isBlank()) {
            return EdgeCategory.SEMANTIC.name();
        }
        EdgeDefinition edgeDefinition = edgeDefinitionResolver.resolve(edgeType, EdgeCategory.SEMANTIC);
        return edgeDefinition.getCategory().name();
    }

    private String escapeNebulaString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String buildTraversalClause(GraphDirection direction, List<String> edgeTypes) {
        if (edgeTypes == null || edgeTypes.isEmpty()) {
            return "";
        }
        return " " + direction.name() + " " + String.join(", ", edgeTypes);
    }

    private List<String> resolveSubgraphEdgeTypes() {
        if (!cachedSubgraphEdgeTypes.isEmpty()) {
            return cachedSubgraphEdgeTypes;
        }
        List<String> edgeTypes = loadEdgeTypesFromNebula();
        if (edgeTypes.isEmpty()) {
            edgeTypes = edgeSchemaRegistry.getAll().stream()
                    .map(EdgeSchema::getValue)
                    .toList();
            log.warn("Falling back to in-memory edge registry for subgraph query; SHOW EDGES returned no usable edge types");
        }
        cachedSubgraphEdgeTypes = edgeTypes;
        return cachedSubgraphEdgeTypes;
    }

    private List<String> loadEdgeTypesFromNebula() {
        ResultSet resultSet = nebulaGraphClient.execute("SHOW EDGES");
        if (resultSet == null || !resultSet.isSucceeded() || resultSet.getRows() == null) {
            return List.of();
        }
        List<String> edgeTypes = new ArrayList<>();
        for (int i = 0; i < resultSet.getRows().size(); i++) {
            try {
                String edgeType = resultSet.rowValues(i).values().get(0).asString();
                if (edgeType != null && !edgeType.isBlank()) {
                    edgeTypes.add(edgeType);
                }
            } catch (Exception e) {
                log.debug("Failed to parse edge type from SHOW EDGES row {}", i, e);
            }
        }
        return edgeTypes.stream().distinct().toList();
    }
}
