package com.puti.code.documentation.repository.sql;

import com.puti.code.base.entity.rdb.DocumentationMethod;

import java.util.List;
import java.util.Optional;

/**
 * 说明书方法信息数据访问接口
 * 
 * @author diodehe
 */
public interface DocumentationMethodRepository {
    
    /**
     * 保存方法信息
     * 
     * @param method 方法信息实体
     * @return 保存后的实体
     */
    DocumentationMethod save(DocumentationMethod method);
    
    /**
     * 批量保存方法信息
     * 
     * @param methods 方法信息列表
     * @return 保存后的实体列表
     */
    List<DocumentationMethod> saveAll(List<DocumentationMethod> methods);
    
    /**
     * 根据ID查找方法信息
     * 
     * @param id 方法信息ID
     * @return 方法信息实体
     */
    Optional<DocumentationMethod> findById(String id);
    
    /**
     * 根据说明书ID查找方法信息
     *
     * @param documentationId 说明书ID
     * @return 方法信息列表
     */
    List<DocumentationMethod> findByDocumentationId(String documentationId);

    /**
     * 根据说明书ID列表批量查找方法信息
     *
     * @param documentationIds 说明书ID列表
     * @return 方法信息列表
     */
    List<DocumentationMethod> findByDocumentationIds(List<String> documentationIds);

    /**
     * 根据方法ID查找方法信息
     *
     * @param methodId 方法ID
     * @return 方法信息列表
     */
    List<DocumentationMethod> findByMethodId(String methodId);

    /**
     * 根据说明书ID和调用层级查找方法信息
     *
     * @param documentationId 说明书ID
     * @param callLevel 调用层级
     * @return 方法信息列表
     */
    List<DocumentationMethod> findByDocumentationIdAndCallLevel(String documentationId, Integer callLevel);

    /**
     * 根据说明书ID和方法类型查找方法信息
     *
     * @param documentationId 说明书ID
     * @param methodType 方法类型
     * @return 方法信息列表
     */
    List<DocumentationMethod> findByDocumentationIdAndMethodType(String documentationId,
                                                                DocumentationMethod.MethodType methodType);
    
    /**
     * 根据类名查找方法信息
     * 
     * @param className 类名
     * @return 方法信息列表
     */
    List<DocumentationMethod> findByClassName(String className);
    
    /**
     * 更新方法信息
     * 
     * @param method 方法信息实体
     * @return 更新后的实体
     */
    DocumentationMethod update(DocumentationMethod method);
    
    /**
     * 删除方法信息
     * 
     * @param id 方法信息ID
     * @return 是否删除成功
     */
    boolean deleteById(String id);
    
    /**
     * 根据说明书ID删除方法信息
     * 
     * @param documentationId 说明书ID
     * @return 删除的数量
     */
    int deleteByDocumentationId(String documentationId);
    
    /**
     * 批量删除方法信息
     * 
     * @param ids 方法信息ID列表
     * @return 删除的数量
     */
    int deleteByIds(List<String> ids);
    
    /**
     * 统计方法信息总数
     * 
     * @return 总数
     */
    long count();
    
    /**
     * 根据说明书ID统计方法信息数量
     * 
     * @param documentationId 说明书ID
     * @return 数量
     */
    long countByDocumentationId(String documentationId);
    
    /**
     * 根据方法类型统计数量
     * 
     * @param methodType 方法类型
     * @return 数量
     */
    long countByMethodType(DocumentationMethod.MethodType methodType);
}
