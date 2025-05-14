package com.puti.code.documentation.repository.sql.impl;

import com.puti.code.base.entity.rdb.DocumentationAggregation;
import com.puti.code.documentation.repository.sql.DocumentationAggregationRepository;
import com.puti.code.documentation.repository.sql.mapper.DocumentationAggregationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于MyBatis的说明书聚合关系数据访问实现
 * 
 * @author diodehe
 */
@Slf4j
@Repository
public class DocumentationAggregationRepositoryImpl implements DocumentationAggregationRepository {
    
    @Autowired
    private DocumentationAggregationMapper aggregationMapper;
    
    @Override
    public DocumentationAggregation save(DocumentationAggregation aggregation) {
        try {
            if (aggregation.getCreatedAt() == null) {
                aggregation.setCreatedAt(LocalDateTime.now());
            }
            
            int result = aggregationMapper.insert(aggregation);
            if (result > 0) {
                log.debug("成功插入聚合关系，ID: {}", aggregation.getId());
                return aggregation;
            } else {
                log.error("插入聚合关系失败");
                return null;
            }
        } catch (Exception e) {
            log.error("保存聚合关系时发生错误", e);
            throw new RuntimeException("保存聚合关系失败", e);
        }
    }
    
    @Override
    public int batchSave(List<DocumentationAggregation> aggregations) {
        if (aggregations == null || aggregations.isEmpty()) {
            return 0;
        }
        
        try {
            // 设置创建时间
            LocalDateTime now = LocalDateTime.now();
            aggregations.forEach(aggregation -> {
                if (aggregation.getCreatedAt() == null) {
                    aggregation.setCreatedAt(now);
                }
            });
            
            int result = aggregationMapper.batchInsert(aggregations);
            log.debug("成功批量插入 {} 个聚合关系", result);
            return result;
        } catch (Exception e) {
            log.error("批量保存聚合关系时发生错误", e);
            throw new RuntimeException("批量保存聚合关系失败", e);
        }
    }
    
    @Override
    public Optional<DocumentationAggregation> findById(String id) {
        try {
            DocumentationAggregation aggregation = aggregationMapper.findById(id);
            return Optional.ofNullable(aggregation);
        } catch (Exception e) {
            log.error("根据ID查找聚合关系时发生错误: {}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<DocumentationAggregation> findByAggregatedDocumentationId(String aggregatedDocumentationId) {
        try {
            return aggregationMapper.findByAggregatedDocumentationId(aggregatedDocumentationId);
        } catch (Exception e) {
            log.error("根据聚合说明书ID查找关系时发生错误: {}", aggregatedDocumentationId, e);
            return List.of();
        }
    }
    
    @Override
    public List<DocumentationAggregation> findByDocumentationId(String documentationId) {
        try {
            return aggregationMapper.findByDocumentationId(documentationId);
        } catch (Exception e) {
            log.error("根据原始说明书ID查找聚合关系时发生错误: {}", documentationId, e);
            return List.of();
        }
    }
    
    @Override
    public List<DocumentationAggregation> findByEntryPointId(String entryPointId) {
        try {
            return aggregationMapper.findByEntryPointId(entryPointId);
        } catch (Exception e) {
            log.error("根据入口点ID查找聚合关系时发生错误: {}", entryPointId, e);
            return List.of();
        }
    }
    
    @Override
    public boolean deleteById(String id) {
        try {
            int result = aggregationMapper.deleteById(id);
            boolean success = result > 0;
            if (success) {
                log.debug("成功删除聚合关系，ID: {}", id);
            } else {
                log.warn("删除聚合关系失败，ID: {}", id);
            }
            return success;
        } catch (Exception e) {
            log.error("删除聚合关系时发生错误: {}", id, e);
            return false;
        }
    }
    
    @Override
    public int deleteByAggregatedDocumentationId(String aggregatedDocumentationId) {
        try {
            int result = aggregationMapper.deleteByAggregatedDocumentationId(aggregatedDocumentationId);
            log.debug("成功删除 {} 个聚合关系，聚合说明书ID: {}", result, aggregatedDocumentationId);
            return result;
        } catch (Exception e) {
            log.error("根据聚合说明书ID删除关系时发生错误: {}", aggregatedDocumentationId, e);
            return 0;
        }
    }
    
    @Override
    public long count() {
        try {
            return aggregationMapper.count();
        } catch (Exception e) {
            log.error("统计聚合关系总数时发生错误", e);
            return 0;
        }
    }
}
