package com.puti.code.base.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 子图数据DTO
 * 包含从图数据库查询得到的子图信息
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubgraphData {
    
    /**
     * 入口点信息
     */
    private MethodInfo entryPoint;
    
    /**
     * 子图中的所有方法节点
     */
    private List<MethodInfo> methods;
    
    /**
     * 方法间的调用关系
     */
    private List<CallRelation> relations;
    
    /**
     * 子图的最大层级深度
     */
    private Integer maxLevel;
    
    /**
     * 子图生成时间戳
     */
    private Long timestamp;
    
    /**
     * 调用关系实体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallRelation {
        
        /**
         * 源方法ID
         */
        private String sourceMethodId;
        
        /**
         * 目标方法ID
         */
        private String targetMethodId;
        
        /**
         * 关系类型（CALLS, INVOKES, IMPLEMENTS等）
         */
        private String relationType;
        
        /**
         * 依赖类型（DIRECT, INDIRECT, INTERFACE等）
         */
        private String dependencyType;
        
        /**
         * 调用发生的行号
         */
        private Integer lineNumber;
        
        /**
         * 调用权重（调用频率或重要性）
         */
        private Double weight;
        
        /**
         * 关系属性（额外的键值对信息）
         */
        private Map<String, Object> properties;
    }
    
    /**
     * 获取指定层级的方法列表
     */
    public List<MethodInfo> getMethodsAtLevel(int level) {
        if (methods == null) {
            return List.of();
        }
        
        return methods.stream()
                .filter(method -> method.getLevel() != null && method.getLevel() == level)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有方法
     */
    public List<MethodInfo> getAllMethods() {
        return methods != null ? methods : List.of();
    }
    
    /**
     * 获取所有调用关系
     */
    public List<CallRelation> getRelations() {
        return relations != null ? relations : List.of();
    }
    
    /**
     * 根据方法ID查找方法信息
     */
    public MethodInfo findMethodById(String methodId) {
        if (methods == null || methodId == null) {
            return null;
        }
        
        return methods.stream()
                .filter(method -> methodId.equals(method.getMethodId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取指定方法的调用关系（作为源方法）
     */
    public List<CallRelation> getOutgoingRelations(String methodId) {
        if (relations == null || methodId == null) {
            return List.of();
        }
        
        return relations.stream()
                .filter(relation -> methodId.equals(relation.getSourceMethodId()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定方法的被调用关系（作为目标方法）
     */
    public List<CallRelation> getIncomingRelations(String methodId) {
        if (relations == null || methodId == null) {
            return List.of();
        }
        
        return relations.stream()
                .filter(relation -> methodId.equals(relation.getTargetMethodId()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取总节点数
     */
    public int getTotalNodes() {
        return methods != null ? methods.size() : 0;
    }
    
    /**
     * 获取总边数
     */
    public int getTotalEdges() {
        return relations != null ? relations.size() : 0;
    }
    
    /**
     * 检查子图是否为空
     */
    public boolean isEmpty() {
        return (methods == null || methods.isEmpty()) && 
               (relations == null || relations.isEmpty());
    }
    
    /**
     * 获取子图统计信息
     */
    public SubgraphStatistics getStatistics() {
        return SubgraphStatistics.builder()
                .totalNodes(getTotalNodes())
                .totalEdges(getTotalEdges())
                .maxLevel(maxLevel != null ? maxLevel : 0)
                .entryPointId(entryPoint != null ? entryPoint.getMethodId() : null)
                .methodsByLevel(getMethodCountByLevel())
                .relationsByType(getRelationCountByType())
                .build();
    }
    
    /**
     * 获取各层级的方法数量统计
     */
    private Map<Integer, Long> getMethodCountByLevel() {
        if (methods == null) {
            return Map.of();
        }
        
        return methods.stream()
                .filter(method -> method.getLevel() != null)
                .collect(Collectors.groupingBy(
                    MethodInfo::getLevel,
                    Collectors.counting()
                ));
    }
    
    /**
     * 获取各类型关系的数量统计
     */
    private Map<String, Long> getRelationCountByType() {
        if (relations == null) {
            return Map.of();
        }
        
        return relations.stream()
                .filter(relation -> relation.getRelationType() != null)
                .collect(Collectors.groupingBy(
                    CallRelation::getRelationType,
                    Collectors.counting()
                ));
    }
    
    /**
     * 子图统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubgraphStatistics {
        private Integer totalNodes;
        private Integer totalEdges;
        private Integer maxLevel;
        private String entryPointId;
        private Map<Integer, Long> methodsByLevel;
        private Map<String, Long> relationsByType;
    }
    
    /**
     * 验证子图数据的完整性
     */
    public boolean isValid() {
        // 检查入口点
        if (entryPoint == null || entryPoint.getMethodId() == null) {
            return false;
        }
        
        // 检查方法列表
        if (methods == null || methods.isEmpty()) {
            return false;
        }
        
        // 检查入口点是否在方法列表中
        boolean entryPointExists = methods.stream()
                .anyMatch(method -> entryPoint.getMethodId().equals(method.getMethodId()));
        
        if (!entryPointExists) {
            return false;
        }
        
        // 检查关系的完整性
        if (relations != null) {
            for (CallRelation relation : relations) {
                if (relation.getSourceMethodId() == null || relation.getTargetMethodId() == null) {
                    return false;
                }
                
                // 检查关系中的方法是否都存在于方法列表中
                boolean sourceExists = methods.stream()
                        .anyMatch(method -> relation.getSourceMethodId().equals(method.getMethodId()));
                boolean targetExists = methods.stream()
                        .anyMatch(method -> relation.getTargetMethodId().equals(method.getMethodId()));
                
                if (!sourceExists || !targetExists) {
                    return false;
                }
            }
        }
        
        return true;
    }
}
