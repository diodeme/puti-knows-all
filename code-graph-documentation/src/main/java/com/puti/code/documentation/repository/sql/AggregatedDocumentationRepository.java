package com.puti.code.documentation.repository.sql;

import com.puti.code.base.entity.rdb.AggregatedDocumentation;

import java.util.List;
import java.util.Optional;

/**
 * 聚合说明书数据访问接口
 * 
 * @author diodehe
 */
public interface AggregatedDocumentationRepository {
    
    /**
     * 保存聚合说明书
     * 
     * @param aggregatedDocumentation 聚合说明书实体
     * @return 保存后的实体（包含生成的ID）
     */
    AggregatedDocumentation save(AggregatedDocumentation aggregatedDocumentation);
    
    /**
     * 根据ID查找聚合说明书
     *
     * @param id 聚合说明书ID
     * @return 聚合说明书实体
     */
    Optional<AggregatedDocumentation> findById(String id);

    /**
     * 根据ID列表批量查找聚合说明书
     *
     * @param ids 聚合说明书ID列表
     * @return 聚合说明书列表
     */
    List<AggregatedDocumentation> findByIds(List<String> ids);
    
    /**
     * 根据项目ID和分支名称查找聚合说明书
     * 
     * @param projectId 项目ID
     * @param branchName 分支名称
     * @return 聚合说明书列表
     */
    List<AggregatedDocumentation> findByProjectIdAndBranchName(String projectId, String branchName);
    
    /**
     * 根据聚合类型查找聚合说明书
     * 
     * @param aggregationType 聚合类型
     * @return 聚合说明书列表
     */
    List<AggregatedDocumentation> findByAggregationType(String aggregationType);
    
    /**
     * 根据状态查找聚合说明书
     * 
     * @param status 状态
     * @return 聚合说明书列表
     */
    List<AggregatedDocumentation> findByStatus(AggregatedDocumentation.AggregationStatus status);
    
    /**
     * 更新聚合说明书
     * 
     * @param aggregatedDocumentation 聚合说明书实体
     * @return 更新后的实体
     */
    AggregatedDocumentation update(AggregatedDocumentation aggregatedDocumentation);
    
    /**
     * 删除聚合说明书
     * 
     * @param id 聚合说明书ID
     * @return 是否删除成功
     */
    boolean deleteById(String id);
    
    /**
     * 统计聚合说明书总数
     * 
     * @return 总数
     */
    long count();
    
    /**
     * 根据项目ID统计聚合说明书数量
     * 
     * @param projectId 项目ID
     * @return 数量
     */
    long countByProjectId(String projectId);
    
    /**
     * 检查指定项目和分支是否已有聚合说明书
     * 
     * @param projectId 项目ID
     * @param branchName 分支名称
     * @return 是否存在
     */
    boolean existsByProjectIdAndBranchName(String projectId, String branchName);
}
