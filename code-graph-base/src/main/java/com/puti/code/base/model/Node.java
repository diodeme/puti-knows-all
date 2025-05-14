package com.puti.code.base.model;

import com.puti.code.base.util.ContentCompressor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * 基础节点类
 */
@Data
@SuperBuilder
public abstract class Node {
    /**
     * 节点ID，32位定长字符串
     */
    private String id;
    
    /**
     * 节点类型
     */
    private NodeType nodeType;
    
    /**
     * 节点全限定名
     */
    private String fullName;
    
    /**
     * 节点向量
     */
    private float[] vector;

    private String content;

    private String vectorOriContent;

    public String getOriContent(){
        return ContentCompressor.decompress(content);
    }

    public void setContent(String content){
        this.content = ContentCompressor.compress(content);
    }
    
    /**
     * 获取节点标签
     */
    public abstract String getTag();
    
    /**
     * 获取节点属性
     */
    public abstract Object[] getProperties();
    
    /**
     * 获取节点属性名
     */
    public abstract String[] getPropertyNames();

    public boolean needRecordOriContent(){
        return NodeType.ANNOTATION.equals(nodeType) || NodeType.COMMENT.equals(nodeType)
                || NodeType.FIELD.equals(nodeType) || NodeType.FUNCTION.equals(nodeType) || NodeType.MARKER_ANNOTATION.equals(nodeType);
    }
}
