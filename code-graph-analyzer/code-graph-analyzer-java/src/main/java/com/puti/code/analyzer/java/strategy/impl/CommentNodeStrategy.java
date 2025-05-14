package com.puti.code.analyzer.java.strategy.impl;

import com.puti.code.base.model.CommentNode;
import com.puti.code.base.model.Node;
import com.puti.code.analyzer.java.strategy.NodeContentStrategy;

/**
 * 注释节点内容获取策略
 * 通用策略，适用于所有处理器中的CommentNode
 * 
 * @author AI Assistant
 */
public class CommentNodeStrategy implements NodeContentStrategy {
    
    @Override
    public String getDigest(Node node, Object context) {
        if (!(node instanceof CommentNode commentNode)) {
            throw new IllegalArgumentException("Node must be CommentNode");
        }
        
        return commentNode.getOriContent();
    }
}
