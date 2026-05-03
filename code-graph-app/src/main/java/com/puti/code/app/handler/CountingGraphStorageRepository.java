package com.puti.code.app.handler;

import com.puti.code.base.model.Edge;
import com.puti.code.base.model.Node;
import com.puti.code.repository.graph.GraphStorageRepository;

import java.util.List;

/**
 * GraphStorageRepository 的计数装饰器。
 * 在委托所有操作的同时，统计插入的节点和边数量。
 * 用于 LibraryHandler 跟踪每个 JAR 处理后的节点/边计数。
 */
class CountingGraphStorageRepository implements GraphStorageRepository {

    private final GraphStorageRepository delegate;
    private int nodeCount;
    private int edgeCount;

    CountingGraphStorageRepository(GraphStorageRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public void insertNode(Node node) {
        delegate.insertNode(node);
        nodeCount++;
    }

    @Override
    public void insertEdge(Edge edge) {
        delegate.insertEdge(edge);
        edgeCount++;
    }

    @Override
    public void batchInsertNodes(List<Node> nodes) {
        delegate.batchInsertNodes(nodes);
        nodeCount += nodes.size();
    }

    @Override
    public void batchInsertEdges(List<Edge> edges) {
        delegate.batchInsertEdges(edges);
        edgeCount += edges.size();
    }

    @Override
    public void deleteNode(String nodeId) {
        delegate.deleteNode(nodeId);
    }

    @Override
    public void deleteNodes(List<String> nodeIds) {
        delegate.deleteNodes(nodeIds);
    }

    @Override
    public void deleteEdgesBySrcOrDst(String nodeId) {
        delegate.deleteEdgesBySrcOrDst(nodeId);
    }

    @Override
    public void deleteEdges(List<String> srcIds, List<String> dstIds, String edgeType) {
        delegate.deleteEdges(srcIds, dstIds, edgeType);
    }

    @Override
    public void close() {
        delegate.close();
    }

    int getNodeCount() {
        return nodeCount;
    }

    int getEdgeCount() {
        return edgeCount;
    }

    void reset() {
        nodeCount = 0;
        edgeCount = 0;
    }
}
