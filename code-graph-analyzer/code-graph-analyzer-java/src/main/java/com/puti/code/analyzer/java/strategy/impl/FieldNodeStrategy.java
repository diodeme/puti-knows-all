package com.puti.code.analyzer.java.strategy.impl;

import com.puti.code.base.model.FieldNode;
import com.puti.code.base.model.Node;
import com.puti.code.analyzer.java.strategy.NodeContentStrategy;

/**
 * 字段节点内容获取策略
 * 
 * @author AI Assistant
 */
public class FieldNodeStrategy implements NodeContentStrategy {
    
    @Override
    public String getDigest(Node node, Object context) {
        if (!(node instanceof FieldNode fieldNode)) {
            throw new IllegalArgumentException("Node must be FieldNode");
        }

        return "Field: " + fieldNode.getName() + ", Type: " + fieldNode.getType();
    }
}
