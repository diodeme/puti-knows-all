package com.puti.code.server.dao;

import com.puti.code.repository.graph.query.GraphDirection;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import com.puti.code.repository.graph.query.GraphQuerySubgraph;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GraphQueryDao {

    private final GraphQueryRepository graphQueryRepository;

    public List<GraphQueryNode> searchMethodByName(String methodName) {
        return graphQueryRepository.searchMethodByName(methodName);
    }

    public Optional<GraphQueryNode> findFunctionByFullName(String methodFullName) {
        return graphQueryRepository.findFunctionByFullName(methodFullName);
    }

    public Optional<String> findFunctionIdByFullName(String methodFullName) {
        return graphQueryRepository.findFunctionIdByFullName(methodFullName);
    }

    public GraphQuerySubgraph getSubgraph(String startVid, int pathDepth, String direction) {
        return graphQueryRepository.getSubgraph(startVid, pathDepth, GraphDirection.fromValue(direction), null);
    }

    public Optional<GraphQueryNode> getNodeDetail(String nodeId) {
        return graphQueryRepository.findNodeById(nodeId);
    }

    public List<GraphQueryNode> getEntryPoints(String projectId, String branchName) {
        return graphQueryRepository.getEntryPoints(projectId, branchName);
    }

    public Optional<String> getContainingFilePath(String nodeId) {
        return graphQueryRepository.getContainingFilePath(nodeId);
    }
}
