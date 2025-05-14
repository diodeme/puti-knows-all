package com.puti.code.documentation.repository.sql;

import com.puti.code.base.entity.rdb.Documentation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 说明书数据访问接口
 * 
 * @author diodehe
 */
public interface DocumentationRepository {
    
    /**
     * 保存说明书
     * 
     * @param documentation 说明书实体
     * @return 保存后的实体（包含生成的ID）
     */
    Documentation save(Documentation documentation);
    
    /**
     * 根据ID查找说明书
     *
     * @param id 说明书ID
     * @return 说明书实体
     */
    Optional<Documentation> findById(String id);

    /**
     * 根据ID列表批量查找说明书
     *
     * @param ids 说明书ID列表
     * @return 说明书列表
     */
    List<Documentation> findByIds(List<String> ids);
    
    /**
     * 根据入口点ID查找说明书
     * 
     * @param entryPointId 入口点ID
     * @return 说明书列表
     */
    List<Documentation> findByEntryPointId(String entryPointId);
    
    /**
     * 根据入口点ID和层级查找说明书
     * 
     * @param entryPointId 入口点ID
     * @param level 层级
     * @return 说明书实体
     */
    Optional<Documentation> findByEntryPointIdAndLevel(String entryPointId, Integer level);
    
    /**
     * 查找入口点的最终版本说明书
     * 
     * @param entryPointId 入口点ID
     * @return 最终版本说明书
     */
    Optional<Documentation> findFinalVersionByEntryPointId(String entryPointId);
    
    /**
     * 查找入口点的中间态版本说明书
     * 
     * @param entryPointId 入口点ID
     * @return 中间态版本列表
     */
    List<Documentation> findIntermediateVersionsByEntryPointId(String entryPointId);
    
    /**
     * 根据项目ID查找说明书
     *
     * @param projectId 项目ID
     * @return 说明书列表
     */
    List<Documentation> findByProjectId(String projectId);

    /**
     * 根据项目ID和分支名称查找说明书
     *
     * @param projectId 项目ID
     * @param branchName 分支名称
     * @return 说明书列表
     */
    List<Documentation> findByProjectIdAndBranchName(String projectId, String branchName, Integer level);
    
    /**
     * 根据状态查找说明书
     * 
     * @param status 状态
     * @return 说明书列表
     */
    List<Documentation> findByStatus(Documentation.DocumentationStatus status);
    
    /**
     * 查找指定时间之前创建的说明书
     * 
     * @param createdBefore 时间点
     * @return 说明书列表
     */
    List<Documentation> findByCreatedAtBefore(LocalDateTime createdBefore);
    
    /**
     * 更新说明书
     * 
     * @param documentation 说明书实体
     * @return 更新后的实体
     */
    Documentation update(Documentation documentation);
    
    /**
     * 删除说明书
     * 
     * @param id 说明书ID
     * @return 是否删除成功
     */
    boolean deleteById(String id);
    
    /**
     * 批量删除说明书
     *
     * @param ids 说明书ID列表
     * @return 删除的数量
     */
    int deleteByIds(List<String> ids);
    
    /**
     * 统计说明书总数
     * 
     * @return 总数
     */
    long count();
    
    /**
     * 根据项目ID统计说明书数量
     * 
     * @param projectId 项目ID
     * @return 数量
     */
    long countByProjectId(String projectId);
    
    /**
     * 根据状态统计说明书数量
     * 
     * @param status 状态
     * @return 数量
     */
    long countByStatus(Documentation.DocumentationStatus status);
    
    /**
     * 检查入口点是否已有说明书
     * 
     * @param entryPointId 入口点ID
     * @return 是否存在
     */
    boolean existsByEntryPointId(String entryPointId);
    
    /**
     * 检查入口点的指定层级是否已有说明书
     * 
     * @param entryPointId 入口点ID
     * @param level 层级
     * @return 是否存在
     */
    boolean existsByEntryPointIdAndLevel(String entryPointId, Integer level);
}
