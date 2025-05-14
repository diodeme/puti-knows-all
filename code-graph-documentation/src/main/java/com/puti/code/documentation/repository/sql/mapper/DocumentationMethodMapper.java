package com.puti.code.documentation.repository.sql.mapper;

import com.puti.code.base.entity.rdb.DocumentationMethod;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 说明书方法信息数据访问Mapper
 * 
 * @author diodehe
 */
@Mapper
@Repository
public interface DocumentationMethodMapper {
    
    /**
     * 插入方法信息
     */
    @Insert("""
        INSERT INTO documentation_method (
            id, documentation_id, method_id, method_name, method_type, call_level,
            description, signature, class_name, created_at
        ) VALUES (
            #{id}, #{documentationId}, #{methodId}, #{methodName}, #{methodType}, #{callLevel},
            #{description}, #{signature}, #{className}, #{createdAt}
        )
    """)
    int insert(DocumentationMethod method);
    
    /**
     * 批量插入方法信息
     */
    @Insert("""
        <script>
        INSERT INTO documentation_method (
            id, documentation_id, method_id, method_name, method_type, call_level,
            description, signature, class_name, created_at
        ) VALUES
        <foreach collection="methods" item="method" separator=",">
            (#{method.id}, #{method.documentationId}, #{method.methodId}, #{method.methodName}, 
             #{method.methodType}, #{method.callLevel}, #{method.description}, 
             #{method.signature}, #{method.className}, #{method.createdAt})
        </foreach>
        </script>
    """)
    int insertBatch(@Param("methods") List<DocumentationMethod> methods);
    
    /**
     * 根据ID查询方法信息
     */
    @Select("""
        SELECT id, documentation_id, method_id, method_name, method_type, call_level,
               description, signature, class_name, created_at
        FROM documentation_method 
        WHERE id = #{id}
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "documentationId", column = "documentation_id"),
        @Result(property = "methodId", column = "method_id"),
        @Result(property = "methodName", column = "method_name"),
        @Result(property = "methodType", column = "method_type"),
        @Result(property = "callLevel", column = "call_level"),
        @Result(property = "description", column = "description"),
        @Result(property = "signature", column = "signature"),
        @Result(property = "className", column = "class_name"),
        @Result(property = "createdAt", column = "created_at")
    })
    DocumentationMethod findById(@Param("id") String id);
    
    /**
     * 根据说明书ID查询方法信息列表
     */
    @Select("""
        SELECT id, documentation_id, method_id, method_name, method_type, call_level,
               description, signature, class_name, created_at
        FROM documentation_method 
        WHERE documentation_id = #{documentationId}
        ORDER BY call_level, method_name
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "documentationId", column = "documentation_id"),
        @Result(property = "methodId", column = "method_id"),
        @Result(property = "methodName", column = "method_name"),
        @Result(property = "methodType", column = "method_type"),
        @Result(property = "callLevel", column = "call_level"),
        @Result(property = "description", column = "description"),
        @Result(property = "signature", column = "signature"),
        @Result(property = "className", column = "class_name"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<DocumentationMethod> findByDocumentationId(@Param("documentationId") String documentationId);

    /**
     * 根据说明书ID列表批量查询方法信息
     */
    @Select("<script>" +
            "SELECT id, documentation_id, method_id, method_name, method_type, call_level, " +
            "description, signature, class_name, created_at " +
            "FROM documentation_method WHERE documentation_id IN " +
            "<foreach collection='documentationIds' item='documentationId' open='(' separator=',' close=')'>" +
            "#{documentationId}" +
            "</foreach> " +
            "ORDER BY documentation_id, call_level, created_at" +
            "</script>")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "documentationId", column = "documentation_id"),
        @Result(property = "methodId", column = "method_id"),
        @Result(property = "methodName", column = "method_name"),
        @Result(property = "methodType", column = "method_type"),
        @Result(property = "callLevel", column = "call_level"),
        @Result(property = "description", column = "description"),
        @Result(property = "signature", column = "signature"),
        @Result(property = "className", column = "class_name"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<DocumentationMethod> findByDocumentationIds(@Param("documentationIds") List<String> documentationIds);

    /**
     * 根据方法ID查询方法信息列表
     */
    @Select("""
        SELECT id, documentation_id, method_id, method_name, method_type, call_level,
               description, signature, class_name, created_at
        FROM documentation_method 
        WHERE method_id = #{methodId}
        ORDER BY created_at DESC
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "documentationId", column = "documentation_id"),
        @Result(property = "methodId", column = "method_id"),
        @Result(property = "methodName", column = "method_name"),
        @Result(property = "methodType", column = "method_type"),
        @Result(property = "callLevel", column = "call_level"),
        @Result(property = "description", column = "description"),
        @Result(property = "signature", column = "signature"),
        @Result(property = "className", column = "class_name"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<DocumentationMethod> findByMethodId(@Param("methodId") String methodId);
    
    /**
     * 根据说明书ID和调用层级查询方法信息
     */
    @Select("""
        SELECT id, documentation_id, method_id, method_name, method_type, call_level,
               description, signature, class_name, created_at
        FROM documentation_method 
        WHERE documentation_id = #{documentationId} AND call_level = #{callLevel}
        ORDER BY method_name
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "documentationId", column = "documentation_id"),
        @Result(property = "methodId", column = "method_id"),
        @Result(property = "methodName", column = "method_name"),
        @Result(property = "methodType", column = "method_type"),
        @Result(property = "callLevel", column = "call_level"),
        @Result(property = "description", column = "description"),
        @Result(property = "signature", column = "signature"),
        @Result(property = "className", column = "class_name"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<DocumentationMethod> findByDocumentationIdAndCallLevel(@Param("documentationId") String documentationId,
                                                               @Param("callLevel") Integer callLevel);
    
    /**
     * 根据说明书ID和方法类型查询方法信息
     */
    @Select("""
        SELECT id, documentation_id, method_id, method_name, method_type, call_level,
               description, signature, class_name, created_at
        FROM documentation_method 
        WHERE documentation_id = #{documentationId} AND method_type = #{methodType}
        ORDER BY call_level, method_name
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "documentationId", column = "documentation_id"),
        @Result(property = "methodId", column = "method_id"),
        @Result(property = "methodName", column = "method_name"),
        @Result(property = "methodType", column = "method_type"),
        @Result(property = "callLevel", column = "call_level"),
        @Result(property = "description", column = "description"),
        @Result(property = "signature", column = "signature"),
        @Result(property = "className", column = "class_name"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<DocumentationMethod> findByDocumentationIdAndMethodType(@Param("documentationId") String documentationId,
                                                                @Param("methodType") String methodType);
    
    /**
     * 根据类名查询方法信息列表
     */
    @Select("""
        SELECT id, documentation_id, method_id, method_name, method_type, call_level,
               description, signature, class_name, created_at
        FROM documentation_method 
        WHERE class_name = #{className}
        ORDER BY created_at DESC
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "documentationId", column = "documentation_id"),
        @Result(property = "methodId", column = "method_id"),
        @Result(property = "methodName", column = "method_name"),
        @Result(property = "methodType", column = "method_type"),
        @Result(property = "callLevel", column = "call_level"),
        @Result(property = "description", column = "description"),
        @Result(property = "signature", column = "signature"),
        @Result(property = "className", column = "class_name"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<DocumentationMethod> findByClassName(@Param("className") String className);
    
    /**
     * 更新方法信息
     */
    @Update("""
        UPDATE documentation_method SET 
            method_name = #{methodName},
            method_type = #{methodType},
            call_level = #{callLevel},
            description = #{description},
            signature = #{signature},
            class_name = #{className}
        WHERE id = #{id}
    """)
    int update(DocumentationMethod method);
    
    /**
     * 根据ID删除方法信息
     */
    @Delete("DELETE FROM documentation_method WHERE id = #{id}")
    int deleteById(@Param("id") String id);
    
    /**
     * 根据说明书ID删除方法信息
     */
    @Delete("DELETE FROM documentation_method WHERE documentation_id = #{documentationId}")
    int deleteByDocumentationId(@Param("documentationId") String documentationId);
    
    /**
     * 批量删除方法信息
     */
    @Delete("""
        <script>
        DELETE FROM documentation_method WHERE id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
    """)
    int deleteByIds(@Param("ids") List<String> ids);
    
    /**
     * 统计方法信息总数
     */
    @Select("SELECT COUNT(*) FROM documentation_method")
    long count();
    
    /**
     * 根据说明书ID统计方法信息数量
     */
    @Select("SELECT COUNT(*) FROM documentation_method WHERE documentation_id = #{documentationId}")
    long countByDocumentationId(@Param("documentationId") String documentationId);
    
    /**
     * 根据方法类型统计数量
     */
    @Select("SELECT COUNT(*) FROM documentation_method WHERE method_type = #{methodType}")
    long countByMethodType(@Param("methodType") String methodType);

    /**
     * 根据流程说明书ID分页查询关联的方法ID
     */
    @Select("""
        SELECT method_id
        FROM documentation_method
        WHERE documentation_id = #{documentationId}
        ORDER BY call_level, created_at
        LIMIT #{limit} OFFSET #{offset}
    """)
    List<String> findMethodIdsByDocumentationIdWithPage(@Param("documentationId") String documentationId,
                                                        @Param("limit") int limit,
                                                        @Param("offset") int offset);
}
