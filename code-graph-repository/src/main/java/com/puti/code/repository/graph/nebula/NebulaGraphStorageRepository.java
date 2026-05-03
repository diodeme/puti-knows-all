package com.puti.code.repository.graph.nebula;

import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeCategory;
import com.puti.code.base.model.EdgeSchemaRegistry;
import com.puti.code.base.model.Node;
import com.puti.code.repository.graph.GraphStorageRepository;
import com.puti.code.repository.graph.schema.GraphSchemaManager;
import com.puti.code.repository.nebula.NebulaGraphClient;
import com.puti.code.repository.nebula.NebulaGraphDialect;
import com.puti.code.repository.nebula.NebulaSchemaManager;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nebula 图存储写入实现。
 */
@Slf4j
public class NebulaGraphStorageRepository implements GraphStorageRepository {

    private final NebulaGraphClient nebulaGraphClient;
    private final GraphSchemaManager graphSchemaManager;

    /**
     * DDL 创建后等待存储节点同步的秒数。
     * NebulaGraph DDL 是异步传播的，meta 节点确认 schema 后 storage 节点可能还未同步，
     * 导致首批 INSERT 出现 "No schema found" 错误。
     */
    private static final int DDL_PROPAGATION_WAIT_SECONDS = 5;

    public NebulaGraphStorageRepository() {
        NebulaGraphDialect graphDialect = new NebulaGraphDialect();
        this.nebulaGraphClient = new NebulaGraphClient(graphDialect);
        this.graphSchemaManager = new NebulaSchemaManager(
                nebulaGraphClient::execute,
                EdgeSchemaRegistry.getInstance(),
                graphDialect);
        this.graphSchemaManager.ensureRegisteredSchemas();
        waitForDdlPropagation();
    }

    public NebulaGraphStorageRepository(NebulaGraphClient nebulaGraphClient, GraphSchemaManager graphSchemaManager) {
        this.nebulaGraphClient = nebulaGraphClient;
        this.graphSchemaManager = graphSchemaManager;
        this.graphSchemaManager.ensureRegisteredSchemas();
        waitForDdlPropagation();
    }

    private void waitForDdlPropagation() {
        try {
            log.info("Waiting {} seconds for NebulaGraph DDL propagation to storage nodes...", DDL_PROPAGATION_WAIT_SECONDS);
            Thread.sleep(DDL_PROPAGATION_WAIT_SECONDS * 1000L);
            log.info("DDL propagation wait completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("DDL propagation wait interrupted");
        }
    }

    @Override
    public void insertNode(Node node) {
        nebulaGraphClient.insertNode(node);
    }

    @Override
    public void insertEdge(Edge edge) {
        if (edge != null) {
            graphSchemaManager.ensureEdgeSchema(edge.getTypeName(), edge.getCategory(), edge.resolvedProperties());
        }
        nebulaGraphClient.insertEdge(edge);
    }

    @Override
    public void batchInsertNodes(List<Node> nodes) {
        nebulaGraphClient.batchInsertNodes(nodes);
    }

    @Override
    public void batchInsertEdges(List<Edge> edges) {
        ensureEdgeSchemas(edges);
        nebulaGraphClient.batchInsertEdges(edges);
    }

    private static final int BATCH_DELETE_SIZE = 100;

    @Override
    public void deleteNode(String nodeId) {
        nebulaGraphClient.execute("DELETE VERTEX \"" + escapeNebulaString(nodeId) + "\" WITH EDGE");
    }

    @Override
    public void deleteNodes(List<String> nodeIds) {
        // Batch delete: NebulaGraph supports DELETE VERTEX "id1", "id2", "id3" WITH EDGE
        for (int i = 0; i < nodeIds.size(); i += BATCH_DELETE_SIZE) {
            List<String> batch = nodeIds.subList(i, Math.min(i + BATCH_DELETE_SIZE, nodeIds.size()));
            String ids = batch.stream()
                    .map(id -> "\"" + escapeNebulaString(id) + "\"")
                    .collect(java.util.stream.Collectors.joining(", "));
            nebulaGraphClient.execute("DELETE VERTEX " + ids + " WITH EDGE");
        }
    }

    @Override
    public void deleteEdges(List<String> srcIds, List<String> dstIds, String edgeType) {
        for (int i = 0; i < srcIds.size(); i++) {
            nebulaGraphClient.execute(String.format(
                    "DELETE EDGE `%s` ON (\"%s\") -> (\"%s\")",
                    edgeType, escapeNebulaString(srcIds.get(i)), escapeNebulaString(dstIds.get(i))));
        }
    }

    @Override
    public void deleteEdgesBySrcOrDst(String nodeId) {
        String escapedId = escapeNebulaString(nodeId);
        for (String edgeType : resolveEdgeTypes()) {
            nebulaGraphClient.execute(String.format(
                    "MATCH ()-[e:`%s`]->() WHERE e._src == hash(\"%s\") OR e._dst == hash(\"%s\") DELETE e",
                    edgeType, escapedId, escapedId));
        }
    }

    private List<String> resolveEdgeTypes() {
        List<String> edgeTypes = new java.util.ArrayList<>();
        var result = nebulaGraphClient.execute("SHOW EDGES");
        if (result != null && result.isSucceeded() && result.getRows() != null) {
            for (int i = 0; i < result.getRows().size(); i++) {
                try {
                    edgeTypes.add(result.rowValues(i).values().get(0).asString());
                } catch (Exception e) {
                    log.debug("Failed to parse edge type name from SHOW EDGES row {}", i, e);
                }
            }
        }
        return edgeTypes;
    }

    private static String escapeNebulaString(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public void close() {
        nebulaGraphClient.close();
    }

    private void ensureEdgeSchemas(List<Edge> edges) {
        if (edges == null || edges.isEmpty()) {
            return;
        }
        Map<String, EdgeSchemaSeed> schemaSeeds = new LinkedHashMap<>();
        for (Edge edge : edges) {
            if (edge == null || edge.getTypeName() == null || edge.getTypeName().isBlank()) {
                continue;
            }
            EdgeSchemaSeed seed = schemaSeeds.computeIfAbsent(
                    edge.getTypeName(),
                    key -> new EdgeSchemaSeed(edge.getCategory(), new LinkedHashMap<>()));
            edge.resolvedProperties().forEach((key, value) -> {
                if (value != null) {
                    seed.properties().putIfAbsent(key, value);
                }
            });
        }
        schemaSeeds.forEach((edgeType, seed) ->
                graphSchemaManager.ensureEdgeSchema(edgeType, seed.category(), seed.properties()));
    }

    private record EdgeSchemaSeed(EdgeCategory category, Map<String, Object> properties) {
    }
}
