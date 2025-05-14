package com.puti.code.repository.graph.nebula;

import com.puti.code.base.model.Edge;
import com.puti.code.base.model.Node;
import com.puti.code.repository.graph.GraphStorageRepository;
import com.puti.code.repository.nebula.NebulaGraphClient;

import java.util.List;

/**
 * Nebula 图存储写入实现。
 */
public class NebulaGraphStorageRepository implements GraphStorageRepository {

    private final NebulaGraphClient nebulaGraphClient;

    public NebulaGraphStorageRepository() {
        this(new NebulaGraphClient());
    }

    public NebulaGraphStorageRepository(NebulaGraphClient nebulaGraphClient) {
        this.nebulaGraphClient = nebulaGraphClient;
    }

    @Override
    public void insertNode(Node node) {
        nebulaGraphClient.insertNode(node);
    }

    @Override
    public void insertEdge(Edge edge) {
        nebulaGraphClient.insertEdge(edge);
    }

    @Override
    public void batchInsertNodes(List<Node> nodes) {
        nebulaGraphClient.batchInsertNodes(nodes);
    }

    @Override
    public void batchInsertEdges(List<Edge> edges) {
        nebulaGraphClient.batchInsertEdges(edges);
    }

    @Override
    public void close() {
        nebulaGraphClient.close();
    }
}
