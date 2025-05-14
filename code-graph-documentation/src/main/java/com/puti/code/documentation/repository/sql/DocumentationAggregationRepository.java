package com.puti.code.documentation.repository.sql;

import com.puti.code.base.entity.rdb.DocumentationAggregation;

import java.util.List;
import java.util.Optional;

/**
 * 说明书聚合关系数据访问接口
 * 
 * @author diodehe
 */
public interface DocumentationAggregationRepository {
    
    /**
     * 保存聚合关系
     * 
     * @param aggregation 聚合关系实体
     * @return 保存后的实体（包含生成的ID）
     */
    DocumentationAggregation save(DocumentationAggregation aggregation);
    
    /**
     * 批量保存聚合关系
     * 
     * @param aggregations 聚合关系列表
     * @return 保存的数量
     */
    int batchSave(List<DocumentationAggregation> aggregations);
    
    /**
     * 根据ID查找聚合关系
     * 
     * @param id 聚合关系ID
     * @return 聚合关系实体
     */
    Optional<DocumentationAggregation> findById(String id);
    
    /**
     * 根据聚合说明书ID查找所有关系
     * 
     * @param aggregatedDocumentationId 聚合说明书ID
     * @return 聚合关系列表
     */
    List<DocumentationAggregation> findByAggregatedDocumentationId(String aggregatedDocumentationId);
    
    /**
     * 根据原始说明书ID查找聚合关系
     * 
     * @param documentationId 原始说明书ID
     * @return 聚合关系列表
     */
    List<DocumentationAggregation> findByDocumentationId(String documentationId);
    
    /**
     * 根据入口点ID查找聚合关系
     * 
     * @param entryPointId 入口点ID
     * @return 聚合关系列表
     */
    List<DocumentationAggregation> findByEntryPointId(String entryPointId);
    
    /**
     * 删除聚合关系
     * 
     * @param id 聚合关系ID
     * @return 是否删除成功
     */
    boolean deleteById(String id);
    
    /**
     * 根据聚合说明书ID删除所有关系
     * 
     * @param aggregatedDocumentationId 聚合说明书ID
     * @return 删除的数量
     */
    int deleteByAggregatedDocumentationId(String aggregatedDocumentationId);
    
    /**
     * 统计聚合关系总数
     * 
     * @return 总数
     */
    long count();
}
