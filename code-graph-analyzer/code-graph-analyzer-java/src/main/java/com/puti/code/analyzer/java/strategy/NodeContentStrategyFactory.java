package com.puti.code.analyzer.java.strategy;

import com.puti.code.base.model.Node;
import com.puti.code.base.model.NodeType;
import com.puti.code.analyzer.java.strategy.impl.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点内容策略工厂
 * 根据节点类型返回对应的策略实现
 * 
 * @author AI Assistant
 */
public class NodeContentStrategyFactory {
    
    private static final Map<NodeType, NodeContentStrategy> STRATEGY_MAP = new HashMap<>();
    
    static {
        STRATEGY_MAP.put(NodeType.FILE, new FileNodeStrategy());
        STRATEGY_MAP.put(NodeType.CLASS, new ClassNodeStrategy());
        STRATEGY_MAP.put(NodeType.FUNCTION, new FunctionNodeStrategy());
        STRATEGY_MAP.put(NodeType.COMMENT, new CommentNodeStrategy());
        STRATEGY_MAP.put(NodeType.ANNOTATION, new AnnotationNodeStrategy());
        STRATEGY_MAP.put(NodeType.FIELD, new FieldNodeStrategy());
    }
    
    /**
     * 根据节点类型获取对应的策略
     * 
     * @param nodeType 节点类型
     * @return 对应的策略实现
     */
    public static NodeContentStrategy getStrategy(NodeType nodeType) {
        NodeContentStrategy strategy = STRATEGY_MAP.get(nodeType);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for node type: " + nodeType);
        }
        return strategy;
    }
    
    /**
     * 根据节点获取对应的策略
     * 
     * @param node 节点
     * @return 对应的策略实现
     */
    public static NodeContentStrategy getStrategy(Node node) {
        return getStrategy(node.getNodeType());
    }
}
