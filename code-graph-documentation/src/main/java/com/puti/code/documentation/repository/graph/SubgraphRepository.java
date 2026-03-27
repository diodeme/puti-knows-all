package com.puti.code.documentation.repository.graph;

import com.puti.code.base.model.MethodInfo;
import com.puti.code.base.model.SubgraphData;
import com.puti.code.documentation.repository.graph.support.GraphSupport;
import com.puti.code.repository.graph.query.GraphDirection;
import com.puti.code.repository.graph.query.GraphQueryEdge;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import com.puti.code.repository.graph.query.GraphQuerySubgraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 子图查询服务
 * 负责从图查询仓储中查询和解析子图数据
 *
 * @author diodehe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubgraphRepository {

    private static final List<String> DEFAULT_SUBGRAPH_EDGE_TYPES = List.of(
            "calls", "out_calls", "implemented_by", "overridden_by",
            "super_calls", "interface_calls", "subtype_calls", "injection_calls");

    private final GraphQueryRepository graphQueryRepository;
    
    /**
     * 根据层级获取子图数据
     * 
     * @param entryPointId 入口点ID
     * @param maxSteps 最大步数
     * @return 子图数据
     */
    public SubgraphData getSubgraphByLevel(String entryPointId, int maxSteps) {
        try {
            log.info("开始查询入口点 {} 的 {} 步子图", entryPointId, maxSteps);

            GraphQuerySubgraph resultSet = graphQueryRepository.getSubgraph(
                    entryPointId, maxSteps, GraphDirection.OUT, DEFAULT_SUBGRAPH_EDGE_TYPES);
            SubgraphData subgraph = parseSubgraphResult(resultSet, entryPointId);
            
            log.info("成功查询到子图，节点数: {}, 边数: {}, 最大层级: {}", 
                    subgraph.getTotalNodes(), subgraph.getTotalEdges(), subgraph.getMaxLevel());
            
            return subgraph;
            
        } catch (Exception e) {
            log.error("查询子图时发生错误", e);
            return createEmptySubgraph();
        }
    }
    
    /**
     * 获取指定步数的子图
     * 
     * @param entryPointId 入口点ID
     * @param steps 步数
     * @return 子图数据
     */
    public SubgraphData getSubgraph(String entryPointId, int steps) {
        return getSubgraphByLevel(entryPointId, steps);
    }
    
    /**
     * 解析子图查询结果
     */
    public SubgraphData parseSubgraphResult(GraphQuerySubgraph resultSet, String entryPointId) {
        try {
            if (resultSet == null || (resultSet.getNodes().isEmpty() && resultSet.getEdges().isEmpty())) {
                log.warn("子图查询结果为空");
                return createEmptySubgraph();
            }

            Map<String, MethodInfo> allMethods = new HashMap<>();
            List<SubgraphData.CallRelation> allRelations = new ArrayList<>();

            for (GraphQueryNode node : resultSet.getNodes()) {
                MethodInfo methodInfo = GraphSupport.parseGraphQueryNodeToMethodInfo(node);
                if (methodInfo != null) {
                    allMethods.put(methodInfo.getMethodId(), methodInfo);
                }
            }

            for (GraphQueryEdge edge : resultSet.getEdges()) {
                SubgraphData.CallRelation relation = GraphSupport.parseGraphQueryEdgeToCallRelation(edge);
                if (relation != null) {
                    allRelations.add(relation);
                }
            }

            log.info("合并后共解析到 {} 个节点，{} 条边", allMethods.size(), allRelations.size());

            // 检查是否有有效数据
            if (allMethods.isEmpty()) {
                log.warn("所有行处理完成后未获取到任何节点数据");
                return createEmptySubgraph();
            }

            // 计算每个节点的层级
            Map<Integer, List<MethodInfo>> methodsByLevel = calculateNodeLevels(allMethods, allRelations, entryPointId);

            // 获取入口点信息
            MethodInfo entryPoint = allMethods.get(entryPointId);
            if (entryPoint == null) {
                log.warn("在子图中未找到入口点: {}", entryPointId);
                return createEmptySubgraph();
            }

            // 将所有方法转换为列表
            List<MethodInfo> allMethodsList = methodsByLevel.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            // 构建子图数据
            return SubgraphData.builder()
                    .entryPoint(entryPoint)
                    .methods(allMethodsList)
                    .relations(allRelations)
                    .maxLevel(methodsByLevel.keySet().stream().max(Integer::compareTo).orElse(0))
                    .timestamp(System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("解析子图结果时发生错误", e);
            return createEmptySubgraph();
        }
    }
    
    /**
     * 计算节点层级（基于BFS算法）
     */
    private Map<Integer, List<MethodInfo>> calculateNodeLevels(Map<String, MethodInfo> allMethods, 
                                                              List<SubgraphData.CallRelation> relations, 
                                                              String entryPointId) {
        
        Map<Integer, List<MethodInfo>> methodsByLevel = new HashMap<>();
        Map<String, Integer> nodeLevels = new HashMap<>();
        
        // 构建邻接表
        Map<String, List<String>> adjacencyList = new HashMap<>();
        for (SubgraphData.CallRelation relation : relations) {
            adjacencyList.computeIfAbsent(relation.getSourceMethodId(), k -> new ArrayList<>())
                    .add(relation.getTargetMethodId());
        }
        
        // BFS计算层级
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        // 入口点为第0层
        queue.offer(entryPointId);
        visited.add(entryPointId);
        nodeLevels.put(entryPointId, 0);
        
        while (!queue.isEmpty()) {
            String currentNode = queue.poll();
            int currentLevel = nodeLevels.get(currentNode);
            
            List<String> neighbors = adjacencyList.getOrDefault(currentNode, List.of());
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor) && allMethods.containsKey(neighbor)) {
                    visited.add(neighbor);
                    int neighborLevel = currentLevel + 1;
                    nodeLevels.put(neighbor, neighborLevel);
                    queue.offer(neighbor);
                }
            }
        }
        
        // 按层级分组
        for (Map.Entry<String, Integer> entry : nodeLevels.entrySet()) {
            String nodeId = entry.getKey();
            Integer level = entry.getValue();
            
            MethodInfo method = allMethods.get(nodeId);
            if (method != null) {
                method.setLevel(level);
                methodsByLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(method);
            }
        }
        
        return methodsByLevel;
    }
    
    
    /**
     * 创建空子图
     */
    private SubgraphData createEmptySubgraph() {
        return SubgraphData.builder()
                .methods(new ArrayList<>())
                .relations(new ArrayList<>())
                .maxLevel(0)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
