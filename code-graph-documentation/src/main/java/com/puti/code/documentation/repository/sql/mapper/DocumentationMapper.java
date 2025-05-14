package com.puti.code.documentation.repository.sql.mapper;

import com.puti.code.base.entity.rdb.Documentation;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 说明书数据访问Mapper
 *
 * @author diodehe
 */
@Mapper
@Repository
public interface DocumentationMapper {

    /**
     * 插入说明书
     */
    @Insert("""
                INSERT INTO documentation (
                    id, entry_point_id, entry_point_name, title, summary, content, level, status,
                    version, is_final_version, parent_documentation_id, project_id,
                    branch_name, created_at, updated_at
                ) VALUES (
                    #{id}, #{entryPointId}, #{entryPointName}, #{title}, #{summary}, #{content}, #{level}, #{status},
                    #{version}, #{isFinalVersion}, #{parentDocumentationId}, #{projectId},
                    #{branchName}, #{createdAt}, #{updatedAt}
                )
            """)
    int insert(Documentation documentation);

    /**
     * 根据ID查询说明书
     */
    @Select("""
                SELECT id, entry_point_id, entry_point_name, title, summary, content, level, status,
                       version, is_final_version, parent_documentation_id, project_id,
                       branch_name, created_at, updated_at
                FROM documentation
                WHERE id = #{id}
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "level", column = "level"),
            @Result(property = "status", column = "status"),
            @Result(property = "version", column = "version"),
            @Result(property = "isFinalVersion", column = "is_final_version"),
            @Result(property = "parentDocumentationId", column = "parent_documentation_id"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Documentation findById(@Param("id") String id);

    /**
     * 根据ID列表批量查询说明书
     */
    @Select("<script>" +
            "SELECT id, entry_point_id, entry_point_name, title, summary, content, level, status, " +
            "version, is_final_version, parent_documentation_id, project_id, " +
            "branch_name, created_at, updated_at " +
            "FROM documentation WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "level", column = "level"),
            @Result(property = "status", column = "status"),
            @Result(property = "version", column = "version"),
            @Result(property = "isFinalVersion", column = "is_final_version"),
            @Result(property = "parentDocumentationId", column = "parent_documentation_id"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Documentation> findByIds(@Param("ids") List<String> ids);

    /**
     * 根据入口点ID查询说明书列表
     */
    @Select("""
                SELECT id, entry_point_id, entry_point_name, title, summary, content, level, status,
                       version, is_final_version, parent_documentation_id, project_id,
                       branch_name, created_at, updated_at
                FROM documentation
                WHERE entry_point_id = #{entryPointId}
                ORDER BY level, version DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "level", column = "level"),
            @Result(property = "status", column = "status"),
            @Result(property = "version", column = "version"),
            @Result(property = "isFinalVersion", column = "is_final_version"),
            @Result(property = "parentDocumentationId", column = "parent_documentation_id"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Documentation> findByEntryPointId(@Param("entryPointId") String entryPointId);

    /**
     * 根据入口点ID和层级查询说明书
     */
    @Select("""
                SELECT id, entry_point_id, entry_point_name, title, summary, content, level, status,
                       version, is_final_version, parent_documentation_id, project_id,
                       branch_name, created_at, updated_at
                FROM documentation
                WHERE entry_point_id = #{entryPointId} AND level = #{level}
                ORDER BY version DESC
                LIMIT 1
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "level", column = "level"),
            @Result(property = "status", column = "status"),
            @Result(property = "version", column = "version"),
            @Result(property = "isFinalVersion", column = "is_final_version"),
            @Result(property = "parentDocumentationId", column = "parent_documentation_id"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Documentation findByEntryPointIdAndLevel(@Param("entryPointId") String entryPointId,
                                             @Param("level") Integer level);

    /**
     * 查找入口点的最终版本说明书
     */
    @Select("""
                SELECT id, entry_point_id, entry_point_name, title, summary, content, level, status,
                       version, is_final_version, parent_documentation_id, project_id,
                       branch_name, created_at, updated_at
                FROM documentation
                WHERE entry_point_id = #{entryPointId} AND is_final_version = true
                ORDER BY level DESC, version DESC
                LIMIT 1
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "level", column = "level"),
            @Result(property = "status", column = "status"),
            @Result(property = "version", column = "version"),
            @Result(property = "isFinalVersion", column = "is_final_version"),
            @Result(property = "parentDocumentationId", column = "parent_documentation_id"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Documentation findFinalVersionByEntryPointId(@Param("entryPointId") String entryPointId);

    /**
     * 查找入口点的中间态版本说明书
     */
    @Select("""
                SELECT id, entry_point_id, entry_point_name, title, summary, content, level, status,
                       version, is_final_version, parent_documentation_id, project_id,
                       branch_name, created_at, updated_at
                FROM documentation
                WHERE entry_point_id = #{entryPointId} AND is_final_version = false
                ORDER BY level, version DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "level", column = "level"),
            @Result(property = "status", column = "status"),
            @Result(property = "version", column = "version"),
            @Result(property = "isFinalVersion", column = "is_final_version"),
            @Result(property = "parentDocumentationId", column = "parent_documentation_id"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Documentation> findIntermediateVersionsByEntryPointId(@Param("entryPointId") String entryPointId);

    /**
     * 根据项目ID查询说明书列表
     */
    @Select("""
                SELECT id, entry_point_id, entry_point_name, title, summary, content, level, status,
                       version, is_final_version, parent_documentation_id, project_id,
                       branch_name, created_at, updated_at
                FROM documentation
                WHERE project_id = #{projectId}
                ORDER BY created_at DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "level", column = "level"),
            @Result(property = "status", column = "status"),
            @Result(property = "version", column = "version"),
            @Result(property = "isFinalVersion", column = "is_final_version"),
            @Result(property = "parentDocumentationId", column = "parent_documentation_id"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Documentation> findByProjectId(@Param("projectId") String projectId);

    /**
     * 根据项目ID和分支名称查询说明书列表
     */
    @Select("""
                SELECT id, entry_point_id, entry_point_name, title, summary, content, level, status,
                       version, is_final_version, parent_documentation_id, project_id,
                       branch_name, created_at, updated_at
                FROM documentation
                WHERE project_id = #{projectId} AND branch_name = #{branchName} AND level = #{level}
                ORDER BY created_at DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "level", column = "level"),
            @Result(property = "status", column = "status"),
            @Result(property = "version", column = "version"),
            @Result(property = "isFinalVersion", column = "is_final_version"),
            @Result(property = "parentDocumentationId", column = "parent_documentation_id"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Documentation> findByProjectIdAndBranchName(@Param("projectId") String projectId, @Param("branchName") String branchName,
                                                     @Param("level") Integer level);

    /**
     * 根据状态查询说明书列表
     */
    @Select("""
                SELECT id, entry_point_id, entry_point_name, title, summary, content, level, status,
                       version, is_final_version, parent_documentation_id, project_id,
                       branch_name, created_at, updated_at
                FROM documentation
                WHERE status = #{status}
                ORDER BY created_at DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "level", column = "level"),
            @Result(property = "status", column = "status"),
            @Result(property = "version", column = "version"),
            @Result(property = "isFinalVersion", column = "is_final_version"),
            @Result(property = "parentDocumentationId", column = "parent_documentation_id"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Documentation> findByStatus(@Param("status") String status);

    /**
     * 更新说明书
     */
    @Update("""
                UPDATE documentation SET
                    entry_point_name = #{entryPointName},
                    title = #{title},
                    summary = #{summary},
                    content = #{content},
                    level = #{level},
                    status = #{status},
                    version = #{version},
                    is_final_version = #{isFinalVersion},
                    parent_documentation_id = #{parentDocumentationId},
                    project_id = #{projectId},
                    branch_name = #{branchName},
                    updated_at = #{updatedAt}
                WHERE id = #{id}
            """)
    int update(Documentation documentation);

    /**
     * 根据ID删除说明书
     */
    @Delete("DELETE FROM documentation WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    /**
     * 批量删除说明书
     */
    @Delete("""
                <script>
                DELETE FROM documentation WHERE id IN
                <foreach collection="ids" item="id" open="(" separator="," close=")">
                    #{id}
                </foreach>
                </script>
            """)
    int deleteByIds(@Param("ids") List<String> ids);

    /**
     * 统计说明书总数
     */
    @Select("SELECT COUNT(*) FROM documentation")
    long count();

    /**
     * 根据项目ID统计说明书数量
     */
    @Select("SELECT COUNT(*) FROM documentation WHERE project_id = #{projectId}")
    long countByProjectId(@Param("projectId") String projectId);

    /**
     * 根据状态统计说明书数量
     */
    @Select("SELECT COUNT(*) FROM documentation WHERE status = #{status}")
    long countByStatus(@Param("status") String status);

    /**
     * 检查入口点是否已有说明书
     */
    @Select("SELECT COUNT(*) > 0 FROM documentation WHERE entry_point_id = #{entryPointId}")
    boolean existsByEntryPointId(@Param("entryPointId") String entryPointId);

    /**
     * 检查入口点的指定层级是否已有说明书
     */
    @Select("SELECT COUNT(*) > 0 FROM documentation WHERE entry_point_id = #{entryPointId} AND level = #{level}")
    boolean existsByEntryPointIdAndLevel(@Param("entryPointId") String entryPointId,
                                         @Param("level") Integer level);

    /**
     * 查找指定时间之前创建的说明书
     */
    @Select("""
                SELECT id, entry_point_id, entry_point_name, title, summary, content, level, status, 
                       version, is_final_version, parent_documentation_id, project_id, 
                       branch_name, created_at, updated_at
                FROM documentation 
                WHERE created_at < #{createdBefore}
                ORDER BY created_at
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "level", column = "level"),
            @Result(property = "status", column = "status"),
            @Result(property = "version", column = "version"),
            @Result(property = "isFinalVersion", column = "is_final_version"),
            @Result(property = "parentDocumentationId", column = "parent_documentation_id"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Documentation> findByCreatedAtBefore(@Param("createdBefore") LocalDateTime createdBefore);

    /**
     * 根据ID查询流程说明书内容
     */
    @Select("SELECT content FROM documentation WHERE id = #{id}")
    String findContentById(@Param("id") String id);
}
