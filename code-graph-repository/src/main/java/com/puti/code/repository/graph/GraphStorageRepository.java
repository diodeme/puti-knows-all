package com.puti.code.repository.graph;

import com.puti.code.base.model.Edge;
import com.puti.code.base.model.Node;

import java.util.List;

/**
 * 图存储写入仓储接口。
 */
public interface GraphStorageRepository extends AutoCloseable {

    void insertNode(Node node);

    void insertEdge(Edge edge);

    void batchInsertNodes(List<Node> nodes);

    void batchInsertEdges(List<Edge> edges);

    void deleteNode(String nodeId);

    void deleteNodes(List<String> nodeIds);

    void deleteEdges(List<String> srcIds, List<String> dstIds, String edgeType);

    void deleteEdgesBySrcOrDst(String nodeId);

    @Override
    default void close() {
    }
}
