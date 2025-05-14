package com.puti.code.documentation.repository.sql.impl;

import com.puti.code.base.entity.rdb.AggregatedDocumentation;
import com.puti.code.documentation.repository.sql.AggregatedDocumentationRepository;
import com.puti.code.documentation.repository.sql.mapper.AggregatedDocumentationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于MyBatis的聚合说明书数据访问实现
 * 
 * @author diodehe
 */
@Slf4j
@Repository
public class AggregatedDocumentationRepositoryImpl implements AggregatedDocumentationRepository {
    
    @Autowired
    private AggregatedDocumentationMapper aggregatedDocumentationMapper;
    
    @Override
    public AggregatedDocumentation save(AggregatedDocumentation aggregatedDocumentation) {
        try {
            if (aggregatedDocumentation.getId() == null) {
                // 新增
                if (aggregatedDocumentation.getCreatedAt() == null) {
                    aggregatedDocumentation.setCreatedAt(LocalDateTime.now());
                }
                aggregatedDocumentation.setUpdatedAt(LocalDateTime.now());
                
                int result = aggregatedDocumentationMapper.insert(aggregatedDocumentation);
                if (result > 0) {
                    log.debug("成功插入聚合说明书，ID: {}", aggregatedDocumentation.getId());
                    return aggregatedDocumentation;
                } else {
                    log.error("插入聚合说明书失败");
                    return null;
                }
            } else {
                // 更新
                aggregatedDocumentation.setUpdatedAt(LocalDateTime.now());
                int result = aggregatedDocumentationMapper.update(aggregatedDocumentation);
                if (result > 0) {
                    log.debug("成功更新聚合说明书，ID: {}", aggregatedDocumentation.getId());
                    return aggregatedDocumentation;
                } else {
                    log.error("更新聚合说明书失败，ID: {}", aggregatedDocumentation.getId());
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("保存聚合说明书时发生错误", e);
            throw new RuntimeException("保存聚合说明书失败", e);
        }
    }
    
    @Override
    public Optional<AggregatedDocumentation> findById(String id) {
        try {
            AggregatedDocumentation aggregatedDocumentation = aggregatedDocumentationMapper.findById(id);
            return Optional.ofNullable(aggregatedDocumentation);
        } catch (Exception e) {
            log.error("根据ID查找聚合说明书时发生错误: {}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public List<AggregatedDocumentation> findByIds(List<String> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return List.of();
            }
            return aggregatedDocumentationMapper.findByIds(ids);
        } catch (Exception e) {
            log.error("根据ID列表批量查找聚合说明书时发生错误: {}", ids, e);
            return List.of();
        }
    }
    
    @Override
    public List<AggregatedDocumentation> findByProjectIdAndBranchName(String projectId, String branchName) {
        try {
            return aggregatedDocumentationMapper.findByProjectIdAndBranchName(projectId, branchName);
        } catch (Exception e) {
            log.error("根据项目ID和分支名称查找聚合说明书时发生错误: {}, {}", projectId, branchName, e);
            return List.of();
        }
    }
    
    @Override
    public List<AggregatedDocumentation> findByAggregationType(String aggregationType) {
        try {
            return aggregatedDocumentationMapper.findByAggregationType(aggregationType);
        } catch (Exception e) {
            log.error("根据聚合类型查找聚合说明书时发生错误: {}", aggregationType, e);
            return List.of();
        }
    }
    
    @Override
    public List<AggregatedDocumentation> findByStatus(AggregatedDocumentation.AggregationStatus status) {
        try {
            return aggregatedDocumentationMapper.findByStatus(status.name());
        } catch (Exception e) {
            log.error("根据状态查找聚合说明书时发生错误: {}", status, e);
            return List.of();
        }
    }
    
    @Override
    public AggregatedDocumentation update(AggregatedDocumentation aggregatedDocumentation) {
        return save(aggregatedDocumentation);
    }
    
    @Override
    public boolean deleteById(String id) {
        try {
            int result = aggregatedDocumentationMapper.deleteById(id);
            boolean success = result > 0;
            if (success) {
                log.debug("成功删除聚合说明书，ID: {}", id);
            } else {
                log.warn("删除聚合说明书失败，ID: {}", id);
            }
            return success;
        } catch (Exception e) {
            log.error("删除聚合说明书时发生错误: {}", id, e);
            return false;
        }
    }
    
    @Override
    public long count() {
        try {
            return aggregatedDocumentationMapper.count();
        } catch (Exception e) {
            log.error("统计聚合说明书总数时发生错误", e);
            return 0;
        }
    }
    
    @Override
    public long countByProjectId(String projectId) {
        try {
            return aggregatedDocumentationMapper.countByProjectId(projectId);
        } catch (Exception e) {
            log.error("根据项目ID统计聚合说明书数量时发生错误: {}", projectId, e);
            return 0;
        }
    }
    
    @Override
    public boolean existsByProjectIdAndBranchName(String projectId, String branchName) {
        try {
            return aggregatedDocumentationMapper.existsByProjectIdAndBranchName(projectId, branchName);
        } catch (Exception e) {
            log.error("检查项目和分支是否已有聚合说明书时发生错误: {}, {}", projectId, branchName, e);
            return false;
        }
    }
}
