package com.puti.code.analyzer.java.strategy.impl;

import com.puti.code.analyzer.java.strategy.NodeContentStrategy;
import com.puti.code.base.model.AnnotationNode;
import com.puti.code.base.model.Node;

/**
 * 注解节点内容获取策略
 *
 * @author AI Assistant
 */
public class AnnotationNodeStrategy implements NodeContentStrategy {
    
    @Override
    public String getDigest(Node node, Object context) {
        if (!(node instanceof AnnotationNode annotationNode)) {
            throw new IllegalArgumentException("Node must be AnnotationNode");
        }
        
        return "Annotation: " + annotationNode.getName() + "\n" + annotationNode.getOriContent();
    }
}
