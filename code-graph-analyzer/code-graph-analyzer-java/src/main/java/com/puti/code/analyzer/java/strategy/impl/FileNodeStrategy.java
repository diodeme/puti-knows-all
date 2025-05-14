package com.puti.code.analyzer.java.strategy.impl;

import com.puti.code.base.model.FileNode;
import com.puti.code.base.model.Node;
import com.puti.code.analyzer.java.strategy.NodeContentStrategy;

/**
 * 文件节点内容获取策略
 * 
 * @author AI Assistant
 */
public class FileNodeStrategy implements NodeContentStrategy {
    
    @Override
    public String getDigest(Node node, Object context) {
        if (!(node instanceof FileNode fileNode)) {
            throw new IllegalArgumentException("Node must be FileNode");
        }
        
        return "File: " + fileNode.getName() + ", Path: " + fileNode.getFilePath();
    }
}
