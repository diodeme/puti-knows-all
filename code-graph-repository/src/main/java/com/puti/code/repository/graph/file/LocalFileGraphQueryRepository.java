package com.puti.code.repository.graph.file;

import com.puti.code.repository.graph.query.GraphDirection;
import com.puti.code.repository.graph.query.GraphQueryEdge;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import com.puti.code.repository.graph.query.GraphQuerySubgraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * 本地图查询实现。
 */
public class LocalFileGraphQueryRepository implements GraphQueryRepository {

    private final LocalFileGraphStore graphStore;

    public LocalFileGraphQueryRepository() {
        this(new LocalFileGraphStore());
    }

    public LocalFileGraphQueryRepository(LocalFileGraphStore graphStore) {
        this.graphStore = graphStore;
    }

    @Override
    public List<GraphQueryNode> searchMethodByName(String methodName) {
        String normalized = methodName == null ? "" : methodName;
        return graphStore.snapshot().getFunctionNodes().stream()
                .filter(node -> {
                    Object fullName = node.getProperties().get("full_name");
                    Object name = node.getProperties().get("name");
                    return fullName != null && String.valueOf(fullName).startsWith(normalized)
                            || name != null && normalized.equals(String.valueOf(name));
                })
                .limit(100)
                .map(this::toQueryNode)
                .toList();
    }

    @Override
    public Optional<GraphQueryNode> findFunctionByFullName(String methodFullName) {
        return Optional.ofNullable(graphStore.snapshot().getFunctionsByFullName().get(methodFullName))
                .map(this::toQueryNode);
    }

    @Override
    public Optional<String> findFunctionIdByFullName(String methodFullName) {
        return Optional.ofNullable(graphStore.snapshot().getFunctionsByFullName().get(methodFullName))
                .map(LocalFileGraphNodeRecord::getId);
    }

    @Override
    public GraphQuerySubgraph getSubgraph(String startNodeId, int pathDepth, GraphDirection direction, List<String> edgeTypes) {
        LocalFileGraphSnapshot snapshot = graphStore.snapshot();
        if (!snapshot.getNodesById().containsKey(startNodeId)) {
            return GraphQuerySubgraph.builder().build();
        }

        Set<String> allowedEdgeTypes = edgeTypes == null || edgeTypes.isEmpty() ? null : new LinkedHashSet<>(edgeTypes);
        Map<String, GraphQueryNode> nodes = new LinkedHashMap<>();
        List<GraphQueryEdge> edges = new ArrayList<>();
        Set<String> edgeIds = new LinkedHashSet<>();
        Queue<NodeDepth> queue = new ArrayDeque<>();
        Set<String> visitedDepthKeys = new LinkedHashSet<>();

        queue.offer(new NodeDepth(startNodeId, 0));
        visitedDepthKeys.add(startNodeId + "@0");
        nodes.put(startNodeId, toQueryNode(snapshot.getNodesById().get(startNodeId)));

        while (!queue.isEmpty()) {
            NodeDepth current = queue.poll();
            if (current.depth() >= pathDepth) {
                continue;
            }

            for (LocalFileGraphEdgeRecord edge : resolveEdges(snapshot, current.nodeId(), direction)) {
                if (allowedEdgeTypes != null && !allowedEdgeTypes.contains(edge.getType())) {
                    continue;
                }

                String nextNodeId = resolveNextNodeId(edge, current.nodeId(), direction);
                if (nextNodeId == null) {
                    continue;
                }
                LocalFileGraphNodeRecord nextNode = snapshot.getNodesById().get(nextNodeId);
                if (nextNode == null) {
                    continue;
                }

                nodes.putIfAbsent(nextNodeId, toQueryNode(nextNode));
                String edgeId = edge.getSource() + "->" + edge.getTarget() + ":" + edge.getType();
                if (edgeIds.add(edgeId)) {
                    edges.add(toQueryEdge(edge));
                }

                String visitedKey = nextNodeId + "@" + (current.depth() + 1);
                if (visitedDepthKeys.add(visitedKey)) {
                    queue.offer(new NodeDepth(nextNodeId, current.depth() + 1));
                }
            }
        }

        return GraphQuerySubgraph.builder()
                .nodes(new ArrayList<>(nodes.values()))
                .edges(edges)
                .build();
    }

    @Override
    public Optional<GraphQueryNode> findNodeById(String nodeId) {
        return Optional.ofNullable(graphStore.snapshot().getNodesById().get(nodeId))
                .map(this::toQueryNode);
    }

    @Override
    public List<GraphQueryNode> getEntryPoints(String projectId, String branchName) {
        return graphStore.snapshot().getFunctionNodes().stream()
                .filter(node -> isTrue(node.getProperties().get("is_entry_point")))
                .filter(node -> projectId == null || projectId.equals(stringValue(node.getProperties().get("repo_id"))))
                .filter(node -> branchName == null || branchName.equals(stringValue(node.getProperties().get("branch_name"))))
                .map(this::toQueryNode)
                .toList();
    }

    @Override
    public long countEntryPoints() {
        return graphStore.snapshot().getFunctionNodes().stream()
                .filter(node -> isTrue(node.getProperties().get("is_entry_point")))
                .count();
    }

    @Override
    public Optional<String> getContainingFilePath(String nodeId) {
        return Optional.empty();
    }

    private List<LocalFileGraphEdgeRecord> resolveEdges(LocalFileGraphSnapshot snapshot, String nodeId, GraphDirection direction) {
        return switch (direction) {
            case IN -> snapshot.getIncomingEdges().getOrDefault(nodeId, List.of());
            case OUT -> snapshot.getOutgoingEdges().getOrDefault(nodeId, List.of());
            case BOTH -> {
                List<LocalFileGraphEdgeRecord> edges = new ArrayList<>(snapshot.getOutgoingEdges().getOrDefault(nodeId, List.of()));
                edges.addAll(snapshot.getIncomingEdges().getOrDefault(nodeId, List.of()));
                yield edges;
            }
        };
    }

    private String resolveNextNodeId(LocalFileGraphEdgeRecord edge, String currentNodeId, GraphDirection direction) {
        return switch (direction) {
            case IN -> edge.getSource();
            case OUT -> edge.getTarget();
            case BOTH -> currentNodeId.equals(edge.getSource()) ? edge.getTarget() : edge.getSource();
        };
    }

    private GraphQueryNode toQueryNode(LocalFileGraphNodeRecord node) {
        return GraphQueryNode.builder()
                .id(node.getId())
                .tag(node.getTag())
                .properties(new LinkedHashMap<>(node.getProperties()))
                .build();
    }

    private GraphQueryEdge toQueryEdge(LocalFileGraphEdgeRecord edge) {
        return GraphQueryEdge.builder()
                .source(edge.getSource())
                .target(edge.getTarget())
                .type(edge.getType())
                .category(edge.getCategory())
                .properties(new LinkedHashMap<>(edge.getProperties()))
                .build();
    }

    private boolean isTrue(Object value) {
        return value instanceof Boolean booleanValue ? booleanValue : Boolean.parseBoolean(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record NodeDepth(String nodeId, int depth) {
    }
}
