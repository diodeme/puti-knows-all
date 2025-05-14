package com.puti.code.base.entity.rdb;

import com.puti.code.base.annotation.AutoKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 说明书聚合关系实体类
 * 用于记录流程说明书和聚合说明书的关系
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationAggregation {
    
    /**
     * 主键ID
     */
    @AutoKey
    private String id;

    /**
     * 聚合说明书ID
     */
    private String aggregatedDocumentationId;

    /**
     * 原始说明书ID
     */
    private String documentationId;
    
    /**
     * 入口点ID
     */
    private String entryPointId;
    
    /**
     * 入口点名称
     */
    private String entryPointName;
    
    /**
     * 在聚合中的权重（用于排序）
     */
    private Integer weight;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
