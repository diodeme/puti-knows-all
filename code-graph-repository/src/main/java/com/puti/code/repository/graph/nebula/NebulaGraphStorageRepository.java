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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nebula 图存储写入实现。
 */
public class NebulaGraphStorageRepository implements GraphStorageRepository {

    private final NebulaGraphClient nebulaGraphClient;
    private final GraphSchemaManager graphSchemaManager;

    public NebulaGraphStorageRepository() {
        NebulaGraphDialect graphDialect = new NebulaGraphDialect();
        this.nebulaGraphClient = new NebulaGraphClient(graphDialect);
        this.graphSchemaManager = new NebulaSchemaManager(
                nebulaGraphClient::execute,
                EdgeSchemaRegistry.getInstance(),
                graphDialect);
        this.graphSchemaManager.ensureRegisteredSchemas();
    }

    public NebulaGraphStorageRepository(NebulaGraphClient nebulaGraphClient, GraphSchemaManager graphSchemaManager) {
        this.nebulaGraphClient = nebulaGraphClient;
        this.graphSchemaManager = graphSchemaManager;
        this.graphSchemaManager.ensureRegisteredSchemas();
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
