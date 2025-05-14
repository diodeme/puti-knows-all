package com.puti.code.analyzer.java.strategy;

import com.puti.code.base.model.Node;

/**
 * 节点内容获取策略接口
 * 
 * @author AI Assistant
 */
public interface NodeContentStrategy {
    
    /**
     * 获取节点内容
     * 
     * @param node 节点
     * @param context 处理器上下文，可能包含Factory等信息
     * @return 节点内容
     */
    String getDigest(Node node, Object context);
}
