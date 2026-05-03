package com.puti.code.repository.graph.nebula;

import com.puti.code.base.model.EdgeCategory;
import com.puti.code.base.model.EdgeDefinition;
import com.puti.code.base.model.EdgeDefinitionResolver;
import com.puti.code.base.model.EdgeSchema;
import com.puti.code.base.model.EdgeSchemaRegistry;
import com.puti.code.repository.graph.dialect.GraphDialect;
import com.puti.code.repository.graph.query.GraphDirection;
import com.puti.code.repository.graph.query.GraphGlobalStats;
import com.puti.code.repository.graph.query.GraphQueryEdge;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import com.puti.code.repository.graph.query.GraphQuerySubgraph;
import com.puti.code.repository.nebula.NebulaGraphClient;
import com.puti.code.repository.nebula.NebulaGraphDialect;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Nebula 图查询实现。
 */
@Slf4j
public class NebulaGraphQueryRepository implements GraphQueryRepository {

    private final NebulaGraphClient nebulaGraphClient;
    private final GraphDialect graphDialect;
    private final EdgeSchemaRegistry edgeSchemaRegistry = EdgeSchemaRegistry.getInstance();
    private final EdgeDefinitionResolver edgeDefinitionResolver = new EdgeDefinitionResolver(edgeSchemaRegistry);
    private volatile List<String> cachedSubgraphEdgeTypes = List.of();

    public NebulaGraphQueryRepository() {
        this(new NebulaGraphClient(new NebulaGraphDialect()), new NebulaGraphDialect());
    }

    public NebulaGraphQueryRepository(NebulaGraphClient nebulaGraphClient) {
        this(nebulaGraphClient, new NebulaGraphDialect());
    }

    public NebulaGraphQueryRepository(NebulaGraphClient nebulaGraphClient, GraphDialect graphDialect) {
        this.nebulaGraphClient = nebulaGraphClient;
        this.graphDialect = graphDialect;
    }

    @Override
    public List<GraphQueryNode> searchMethodByName(String methodName) {
        ResultSet result = executeQuery(graphDialect.buildSearchMethodByNameQuery(methodName), "search method by name");
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
        return readFirstNode(executeQuery(
                graphDialect.buildFindFunctionByFullNameQuery(methodFullName),
                "find function by full name"));
    }

    @Override
    public Optional<String> findFunctionIdByFullName(String methodFullName) {
        ResultSet result = executeQuery(
                graphDialect.buildFindFunctionIdByFullNameQuery(methodFullName),
                "find function id by full name");
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
        ResultSet result = executeQuery(
                graphDialect.buildGetSubgraphQuery(
                        startNodeId,
                        pathDepth,
                        direction,
                        edgeTypes == null || edgeTypes.isEmpty() ? resolveSubgraphEdgeTypes() : edgeTypes),
                "load subgraph");
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
        return readFirstNode(executeQuery(graphDialect.buildFindNodeByIdQuery(nodeId), "find node by id"));
    }

    @Override
    public List<GraphQueryNode> getEntryPoints(String projectId, String branchName) {
        ResultSet result = executeQuery(
                graphDialect.buildGetEntryPointsQuery(projectId, branchName),
                "load entry points");
        List<GraphQueryNode> nodes = new ArrayList<>();
        if (result == null || !result.isSucceeded() || result.getRows() == null) {
            return nodes;
        }
        for (int i = 0; i < result.getRows().size(); i++) {
            try {
                GraphQueryNode node = toQueryNode(result.rowValues(i).values().get(0).asNode());
                if (node != null) {
                    nodes.add(node);
                } else {
                    log.warn("Entry point row {} resolved to null (VID or tag issue)", i);
                }
            } catch (Exception e) {
                log.warn("Failed to parse entry point row {}: {}", i, e.getMessage());
            }
        }
        return nodes;
    }

    @Override
    public long countEntryPoints() {
        ResultSet result = executeQuery(graphDialect.buildCountEntryPointsQuery(), "count entry points");
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

    @Override
    public GraphGlobalStats getGlobalStats() {
        // Try SHOW STATS first (fast, uses pre-computed stats)
        GraphGlobalStats stats = tryShowStats();
        if (stats != null) {
            return stats;
        }

        // Fallback: submit stats job then retry
        executeQuery("SUBMIT JOB STATS", "submit stats job");
        // Wait for job to process
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        stats = tryShowStats();
        if (stats != null) {
            return stats;
        }

        // Last resort: individual count queries
        return countByIndividualQueries();
    }

    private GraphGlobalStats tryShowStats() {
        ResultSet result = executeQuery("SHOW STATS", "show stats");
        if (result == null || !result.isSucceeded() || result.getRows() == null) {
            return null;
        }

        Map<String, Long> nodeTypeStats = new LinkedHashMap<>();
        Map<String, Long> edgeTypeStats = new LinkedHashMap<>();
        long totalNodes = 0;
        long totalEdges = 0;
        boolean hasData = false;

        for (int i = 0; i < result.getRows().size(); i++) {
            try {
                String type = result.rowValues(i).values().get(0).asString();
                String name = result.rowValues(i).values().get(1).asString();
                long count = result.rowValues(i).values().get(2).asLong();
                if ("Tag".equals(type) || "tag".equalsIgnoreCase(type)) {
                    nodeTypeStats.put(name, count);
                    totalNodes += count;
                    if (count > 0) hasData = true;
                } else if ("Edge".equals(type) || "edge".equalsIgnoreCase(type)) {
                    edgeTypeStats.put(name, count);
                    totalEdges += count;
                }
            } catch (Exception e) {
                log.debug("Failed to parse SHOW STATS row {}", i, e);
            }
        }

        if (!hasData) {
            return null;
        }

        return GraphGlobalStats.builder()
                .totalNodes(totalNodes)
                .totalEdges(totalEdges)
                .nodeTypeStats(nodeTypeStats)
                .edgeTypeStats(edgeTypeStats)
                .entryPointCount(countEntryPoints())
                .build();
    }

    private GraphGlobalStats countByIndividualQueries() {
        List<String> tagNames = loadTagNames();
        List<String> edgeTypeNames = resolveSubgraphEdgeTypes();

        Map<String, Long> nodeTypeStats = new LinkedHashMap<>();
        long totalNodes = 0;
        for (String tag : tagNames) {
            long count = countByTag(tag);
            nodeTypeStats.put(tag, count);
            totalNodes += count;
        }

        Map<String, Long> edgeTypeStats = new LinkedHashMap<>();
        long totalEdges = 0;
        for (String edgeType : edgeTypeNames) {
            long count = countByEdgeType(edgeType);
            edgeTypeStats.put(edgeType, count);
            totalEdges += count;
        }

        return GraphGlobalStats.builder()
                .totalNodes(totalNodes)
                .totalEdges(totalEdges)
                .nodeTypeStats(nodeTypeStats)
                .edgeTypeStats(edgeTypeStats)
                .entryPointCount(countEntryPoints())
                .build();
    }

    private List<String> loadTagNames() {
        ResultSet resultSet = executeQuery("SHOW TAGS", "load tag names");
        if (resultSet == null || !resultSet.isSucceeded() || resultSet.getRows() == null) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < resultSet.getRows().size(); i++) {
            try {
                String tagName = resultSet.rowValues(i).values().get(0).asString();
                if (tagName != null && !tagName.isBlank()) {
                    tags.add(tagName);
                }
            } catch (Exception e) {
                log.debug("Failed to parse tag name from SHOW TAGS row {}", i, e);
            }
        }
        return tags;
    }

    private long countByTag(String tagName) {
        String query = String.format("MATCH (v:`%s`) RETURN count(v) AS cnt", tagName);
        ResultSet result = executeQuery(query, "count nodes by tag " + tagName);
        if (result == null || !result.isSucceeded() || result.getRows() == null || result.getRows().isEmpty()) {
            return 0L;
        }
        try {
            return result.rowValues(0).values().get(0).asLong();
        } catch (Exception e) {
            log.debug("Failed to parse count for tag {}", tagName, e);
            return 0L;
        }
    }

    private long countByEdgeType(String edgeType) {
        String query = String.format("MATCH ()-[e:`%s`]->() RETURN count(e) AS cnt", edgeType);
        ResultSet result = executeQuery(query, "count edges by type " + edgeType);
        if (result == null || !result.isSucceeded() || result.getRows() == null || result.getRows().isEmpty()) {
            return 0L;
        }
        try {
            return result.rowValues(0).values().get(0).asLong();
        } catch (Exception e) {
            log.debug("Failed to parse count for edge type {}", edgeType, e);
            return 0L;
        }
    }

    @Override
    public List<GraphQueryNode> findNodesByRepoId(String repoId) {
        List<GraphQueryNode> nodes = new ArrayList<>();
        String escapedRepoId = escapeNebulaString(repoId);
        for (String tag : loadTagNames()) {
            String query = String.format("MATCH (v:`%s`) WHERE v.`%s`.repo_id == \"%s\" RETURN v", tag, tag, escapedRepoId);
            ResultSet result = executeQuery(query, "find nodes by repoId");
            if (result == null || !result.isSucceeded() || result.getRows() == null) continue;
            for (int i = 0; i < result.getRows().size(); i++) {
                try {
                    GraphQueryNode node = toQueryNode(result.rowValues(i).values().get(0).asNode());
                    if (node != null) nodes.add(node);
                } catch (Exception e) {
                    log.debug("Failed to parse node by repoId row {}", i, e);
                }
            }
        }
        return nodes;
    }

    @Override
    public Optional<String> getContainingFilePath(String nodeId) {
        String query = graphDialect.buildGetContainingFilePathQuery(nodeId);
        ResultSet result = executeQuery(query, "get containing file path");
        if (result == null || !result.isSucceeded()) {
            log.warn("getContainingFilePath query failed for node {}: {}", nodeId,
                    result != null ? result.getErrorMessage() : "null result");
            return Optional.empty();
        }
        if (result.getRows() == null || result.getRows().isEmpty()) {
            log.debug("getContainingFilePath no rows for node {}", nodeId);
            return Optional.empty();
        }
        try {
            var row = result.rowValues(0);
            var val = row.values().get(0);
            log.info("getContainingFilePath node={}, colCount={}, valType={}, val={}",
                    nodeId, row.values().size(), val.getClass().getSimpleName(), val);
            String filePath = val.asString();
            return Optional.ofNullable(filePath);
        } catch (Exception e) {
            log.warn("Failed to parse file path for node {}: {}", nodeId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        nebulaGraphClient.close();
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
            try {
                vertexId = String.valueOf(vertex.getId().asLong());
            } catch (Exception e2) {
                vertexId = vertex.getId().toString();
            }
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
        ResultSet resultSet = executeQuery(graphDialect.buildShowEdgesQuery(), "load edge types");
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

    private ResultSet executeQuery(String query, String operation) {
        ResultSet resultSet = nebulaGraphClient.execute(query);
        if (resultSet == null) {
            log.warn("Nebula {} returned null result, query={}", operation, query);
            return null;
        }
        if (!resultSet.isSucceeded()) {
            log.warn("Nebula {} failed, query={}, error={}", operation, query, resultSet.getErrorMessage());
        }
        return resultSet;
    }

    private static String escapeNebulaString(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
