package com.puti.code.documentation.repository.sql.impl;

import com.puti.code.base.entity.rdb.DocumentationMethod;
import com.puti.code.documentation.repository.sql.DocumentationMethodRepository;
import com.puti.code.documentation.repository.sql.mapper.DocumentationMethodMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基于MyBatis的说明书方法信息数据访问实现
 * 
 * @author diodehe
 */
@Slf4j
@Repository
public class DocumentationMethodRepositoryImpl implements DocumentationMethodRepository {
    
    @Autowired
    private DocumentationMethodMapper documentationMethodMapper;
    
    @Override
    public DocumentationMethod save(DocumentationMethod method) {
        try {
            if (method.getId() == null) {
                // 新增
                if (method.getCreatedAt() == null) {
                    method.setCreatedAt(LocalDateTime.now());
                }
                
                int result = documentationMethodMapper.insert(method);
                if (result > 0) {
                    log.debug("成功插入方法信息，ID: {}", method.getId());
                    return method;
                } else {
                    log.error("插入方法信息失败");
                    return null;
                }
            } else {
                // 更新
                int result = documentationMethodMapper.update(method);
                if (result > 0) {
                    log.debug("成功更新方法信息，ID: {}", method.getId());
                    return method;
                } else {
                    log.error("更新方法信息失败，ID: {}", method.getId());
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("保存方法信息时发生错误", e);
            throw new RuntimeException("保存方法信息失败", e);
        }
    }

    //fixme 批量插入
    @Override
    @Transactional
    public List<DocumentationMethod> saveAll(List<DocumentationMethod> methods) {
        if (methods == null || methods.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<DocumentationMethod> savedMethods = new ArrayList<>();
        
        try {
            for (DocumentationMethod method : methods) {
                DocumentationMethod saved = save(method);
                if (saved != null) {
                    savedMethods.add(saved);
                } else {
                    log.warn("保存方法信息失败: {}", method.getMethodId());
                }
            }
            
            log.info("批量保存方法信息完成，成功: {}, 总数: {}", savedMethods.size(), methods.size());
            return savedMethods;
            
        } catch (Exception e) {
            log.error("批量保存方法信息时发生错误", e);
            throw new RuntimeException("批量保存方法信息失败", e);
        }
    }
    
    @Override
    public Optional<DocumentationMethod> findById(String id) {
        try {
            DocumentationMethod method = documentationMethodMapper.findById(id);
            return Optional.ofNullable(method);
        } catch (Exception e) {
            log.error("根据ID查找方法信息时发生错误: {}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<DocumentationMethod> findByDocumentationId(String documentationId) {
        try {
            return documentationMethodMapper.findByDocumentationId(documentationId);
        } catch (Exception e) {
            log.error("根据说明书ID查找方法信息时发生错误: {}", documentationId, e);
            return List.of();
        }
    }

    @Override
    public List<DocumentationMethod> findByDocumentationIds(List<String> documentationIds) {
        try {
            if (documentationIds == null || documentationIds.isEmpty()) {
                return List.of();
            }
            return documentationMethodMapper.findByDocumentationIds(documentationIds);
        } catch (Exception e) {
            log.error("根据说明书ID列表批量查找方法信息时发生错误: {}", documentationIds, e);
            return List.of();
        }
    }
    
    @Override
    public List<DocumentationMethod> findByMethodId(String methodId) {
        try {
            return documentationMethodMapper.findByMethodId(methodId);
        } catch (Exception e) {
            log.error("根据方法ID查找方法信息时发生错误: {}", methodId, e);
            return List.of();
        }
    }
    
    @Override
    public List<DocumentationMethod> findByDocumentationIdAndCallLevel(String documentationId, Integer callLevel) {
        try {
            return documentationMethodMapper.findByDocumentationIdAndCallLevel(documentationId, callLevel);
        } catch (Exception e) {
            log.error("根据说明书ID和调用层级查找方法信息时发生错误: {}, {}", documentationId, callLevel, e);
            return List.of();
        }
    }

    @Override
    public List<DocumentationMethod> findByDocumentationIdAndMethodType(String documentationId,
                                                                        DocumentationMethod.MethodType methodType) {
        try {
            return documentationMethodMapper.findByDocumentationIdAndMethodType(documentationId, methodType.name());
        } catch (Exception e) {
            log.error("根据说明书ID和方法类型查找方法信息时发生错误: {}, {}", documentationId, methodType, e);
            return List.of();
        }
    }
    
    @Override
    public List<DocumentationMethod> findByClassName(String className) {
        try {
            return documentationMethodMapper.findByClassName(className);
        } catch (Exception e) {
            log.error("根据类名查找方法信息时发生错误: {}", className, e);
            return List.of();
        }
    }
    
    @Override
    public DocumentationMethod update(DocumentationMethod method) {
        if (method.getId() == null) {
            throw new IllegalArgumentException("更新的实体必须有ID");
        }
        return save(method);
    }
    
    @Override
    public boolean deleteById(String id) {
        try {
            int result = documentationMethodMapper.deleteById(id);
            boolean deleted = result > 0;
            if (deleted) {
                log.debug("成功删除方法信息，ID: {}", id);
            } else {
                log.warn("删除方法信息失败，可能不存在，ID: {}", id);
            }
            return deleted;
        } catch (Exception e) {
            log.error("删除方法信息时发生错误，ID: {}", id, e);
            return false;
        }
    }
    
    @Override
    public int deleteByDocumentationId(String documentationId) {
        try {
            int result = documentationMethodMapper.deleteByDocumentationId(documentationId);
            log.debug("根据说明书ID删除方法信息，删除数量: {}", result);
            return result;
        } catch (Exception e) {
            log.error("根据说明书ID删除方法信息时发生错误: {}", documentationId, e);
            return 0;
        }
    }
    
    @Override
    public int deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        try {
            int result = documentationMethodMapper.deleteByIds(ids);
            log.debug("批量删除方法信息，删除数量: {}", result);
            return result;
        } catch (Exception e) {
            log.error("批量删除方法信息时发生错误", e);
            return 0;
        }
    }
    
    @Override
    public long count() {
        try {
            return documentationMethodMapper.count();
        } catch (Exception e) {
            log.error("统计方法信息总数时发生错误", e);
            return 0;
        }
    }
    
    @Override
    public long countByDocumentationId(String documentationId) {
        try {
            return documentationMethodMapper.countByDocumentationId(documentationId);
        } catch (Exception e) {
            log.error("根据说明书ID统计方法信息数量时发生错误: {}", documentationId, e);
            return 0;
        }
    }
    
    @Override
    public long countByMethodType(DocumentationMethod.MethodType methodType) {
        try {
            return documentationMethodMapper.countByMethodType(methodType.name());
        } catch (Exception e) {
            log.error("根据方法类型统计数量时发生错误: {}", methodType, e);
            return 0;
        }
    }
}
