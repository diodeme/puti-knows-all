package com.puti.code.repository.graph.query;

import java.util.List;
import java.util.Optional;

/**
 * 图查询抽象接口。
 */
public interface GraphQueryRepository {

    List<GraphQueryNode> searchMethodByName(String methodName);

    Optional<GraphQueryNode> findFunctionByFullName(String methodFullName);

    Optional<String> findFunctionIdByFullName(String methodFullName);

    GraphQuerySubgraph getSubgraph(String startNodeId, int pathDepth, GraphDirection direction, List<String> edgeTypes);

    Optional<GraphQueryNode> findNodeById(String nodeId);

    List<GraphQueryNode> getEntryPoints(String projectId, String branchName);

    long countEntryPoints();

    default boolean isEntryPoint(String methodId) {
        return findNodeById(methodId)
                .map(GraphQueryNode::getProperties)
                .map(properties -> properties.get("is_entry_point"))
                .map(value -> value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value)))
                .orElse(false);
    }
}
