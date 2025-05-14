package com.puti.code.documentation.repository.sql.impl;

import com.puti.code.base.entity.rdb.Documentation;
import com.puti.code.documentation.repository.sql.mapper.DocumentationMapper;
import com.puti.code.documentation.repository.sql.DocumentationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于MyBatis的说明书数据访问实现
 * 
 * @author diodehe
 */
@Slf4j
@Repository
public class DocumentationRepositoryImpl implements DocumentationRepository {
    
    @Autowired
    private DocumentationMapper documentationMapper;
    
    @Override
    public Documentation save(Documentation documentation) {
        try {
            if (documentation.getId() == null) {
                // 新增
                if (documentation.getCreatedAt() == null) {
                    documentation.setCreatedAt(LocalDateTime.now());
                }
                documentation.setUpdatedAt(LocalDateTime.now());
                
                int result = documentationMapper.insert(documentation);
                if (result > 0) {
                    log.debug("成功插入说明书，ID: {}", documentation.getId());
                    return documentation;
                } else {
                    log.error("插入说明书失败");
                    return null;
                }
            } else {
                // 更新
                documentation.setUpdatedAt(LocalDateTime.now());
                int result = documentationMapper.update(documentation);
                if (result > 0) {
                    log.debug("成功更新说明书，ID: {}", documentation.getId());
                    return documentation;
                } else {
                    log.error("更新说明书失败，ID: {}", documentation.getId());
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("保存说明书时发生错误", e);
            throw new RuntimeException("保存说明书失败", e);
        }
    }
    
    @Override
    public Optional<Documentation> findById(String id) {
        try {
            Documentation documentation = documentationMapper.findById(id);
            return Optional.ofNullable(documentation);
        } catch (Exception e) {
            log.error("根据ID查找说明书时发生错误: {}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public List<Documentation> findByIds(List<String> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return List.of();
            }
            return documentationMapper.findByIds(ids);
        } catch (Exception e) {
            log.error("根据ID列表批量查找说明书时发生错误: {}", ids, e);
            return List.of();
        }
    }
    
    @Override
    public List<Documentation> findByEntryPointId(String entryPointId) {
        try {
            return documentationMapper.findByEntryPointId(entryPointId);
        } catch (Exception e) {
            log.error("根据入口点ID查找说明书时发生错误: {}", entryPointId, e);
            return List.of();
        }
    }
    
    @Override
    public Optional<Documentation> findByEntryPointIdAndLevel(String entryPointId, Integer level) {
        try {
            Documentation documentation = documentationMapper.findByEntryPointIdAndLevel(entryPointId, level);
            return Optional.ofNullable(documentation);
        } catch (Exception e) {
            log.error("根据入口点ID和层级查找说明书时发生错误: {}, {}", entryPointId, level, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<Documentation> findFinalVersionByEntryPointId(String entryPointId) {
        try {
            Documentation documentation = documentationMapper.findFinalVersionByEntryPointId(entryPointId);
            return Optional.ofNullable(documentation);
        } catch (Exception e) {
            log.error("查找入口点最终版本说明书时发生错误: {}", entryPointId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<Documentation> findIntermediateVersionsByEntryPointId(String entryPointId) {
        try {
            return documentationMapper.findIntermediateVersionsByEntryPointId(entryPointId);
        } catch (Exception e) {
            log.error("查找入口点中间态版本说明书时发生错误: {}", entryPointId, e);
            return List.of();
        }
    }
    
    @Override
    public List<Documentation> findByProjectId(String projectId) {
        try {
            return documentationMapper.findByProjectId(projectId);
        } catch (Exception e) {
            log.error("根据项目ID查找说明书时发生错误: {}", projectId, e);
            return List.of();
        }
    }

    @Override
    public List<Documentation> findByProjectIdAndBranchName(String projectId, String branchName, Integer level) {
        try {
            return documentationMapper.findByProjectIdAndBranchName(projectId, branchName, level);
        } catch (Exception e) {
            log.error("根据项目ID和分支名称查找说明书时发生错误: {}, {}", projectId, branchName, e);
            return List.of();
        }
    }
    
    @Override
    public List<Documentation> findByStatus(Documentation.DocumentationStatus status) {
        try {
            return documentationMapper.findByStatus(status.name());
        } catch (Exception e) {
            log.error("根据状态查找说明书时发生错误: {}", status, e);
            return List.of();
        }
    }
    
    @Override
    public List<Documentation> findByCreatedAtBefore(LocalDateTime createdBefore) {
        try {
            return documentationMapper.findByCreatedAtBefore(createdBefore);
        } catch (Exception e) {
            log.error("查找指定时间之前创建的说明书时发生错误: {}", createdBefore, e);
            return List.of();
        }
    }
    
    @Override
    public Documentation update(Documentation documentation) {
        if (documentation.getId() == null) {
            throw new IllegalArgumentException("更新的实体必须有ID");
        }
        return save(documentation);
    }
    
    @Override
    public boolean deleteById(String id) {
        try {
            int result = documentationMapper.deleteById(id);
            boolean deleted = result > 0;
            if (deleted) {
                log.debug("成功删除说明书，ID: {}", id);
            } else {
                log.warn("删除说明书失败，可能不存在，ID: {}", id);
            }
            return deleted;
        } catch (Exception e) {
            log.error("删除说明书时发生错误，ID: {}", id, e);
            return false;
        }
    }
    
    @Override
    public int deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        try {
            int result = documentationMapper.deleteByIds(ids);
            log.debug("批量删除说明书，删除数量: {}", result);
            return result;
        } catch (Exception e) {
            log.error("批量删除说明书时发生错误", e);
            return 0;
        }
    }
    
    @Override
    public long count() {
        try {
            return documentationMapper.count();
        } catch (Exception e) {
            log.error("统计说明书总数时发生错误", e);
            return 0;
        }
    }
    
    @Override
    public long countByProjectId(String projectId) {
        try {
            return documentationMapper.countByProjectId(projectId);
        } catch (Exception e) {
            log.error("根据项目ID统计说明书数量时发生错误: {}", projectId, e);
            return 0;
        }
    }
    
    @Override
    public long countByStatus(Documentation.DocumentationStatus status) {
        try {
            return documentationMapper.countByStatus(status.name());
        } catch (Exception e) {
            log.error("根据状态统计说明书数量时发生错误: {}", status, e);
            return 0;
        }
    }
    
    @Override
    public boolean existsByEntryPointId(String entryPointId) {
        try {
            return documentationMapper.existsByEntryPointId(entryPointId);
        } catch (Exception e) {
            log.error("检查入口点是否已有说明书时发生错误: {}", entryPointId, e);
            return false;
        }
    }
    
    @Override
    public boolean existsByEntryPointIdAndLevel(String entryPointId, Integer level) {
        try {
            return documentationMapper.existsByEntryPointIdAndLevel(entryPointId, level);
        } catch (Exception e) {
            log.error("检查入口点指定层级是否已有说明书时发生错误: {}, {}", entryPointId, level, e);
            return false;
        }
    }
}
