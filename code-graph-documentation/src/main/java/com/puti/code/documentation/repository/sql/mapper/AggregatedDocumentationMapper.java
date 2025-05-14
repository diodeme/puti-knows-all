package com.puti.code.documentation.repository.sql.mapper;

import com.puti.code.base.entity.rdb.AggregatedDocumentation;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聚合说明书数据访问Mapper
 *
 * @author diodehe
 */
@Mapper
@Repository
public interface AggregatedDocumentationMapper {

    /**
     * 插入聚合说明书
     */
    @Insert("""
                INSERT INTO aggregated_documentation (
                    id, title, summary, content, project_id, branch_name,
                    aggregation_type, status, created_at, updated_at
                ) VALUES (
                    #{id}, #{title}, #{summary}, #{content}, #{projectId}, #{branchName},
                    #{aggregationType}, #{status}, #{createdAt}, #{updatedAt}
                )
            """)
    int insert(AggregatedDocumentation aggregatedDocumentation);

    /**
     * 根据ID查询聚合说明书
     */
    @Select("""
                SELECT id, title, summary, content, project_id, branch_name,
                       aggregation_type, status, created_at, updated_at
                FROM aggregated_documentation
                WHERE id = #{id}
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "aggregationType", column = "aggregation_type"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    AggregatedDocumentation findById(String id);

    /**
     * 根据ID列表批量查询聚合说明书
     */
    @Select("<script>" +
            "SELECT id, title, summary, content, project_id, branch_name, " +
            "aggregation_type, status, created_at, updated_at " +
            "FROM aggregated_documentation WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "aggregationType", column = "aggregation_type"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<AggregatedDocumentation> findByIds(@Param("ids") List<String> ids);

    /**
     * 根据项目ID和分支名称查询聚合说明书
     */
    @Select("""
                SELECT id, title, summary, content, project_id, branch_name,
                       aggregation_type, status, created_at, updated_at
                FROM aggregated_documentation
                WHERE project_id = #{projectId} AND branch_name = #{branchName}
                ORDER BY created_at DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "aggregationType", column = "aggregation_type"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<AggregatedDocumentation> findByProjectIdAndBranchName(@Param("projectId") String projectId, 
                                                               @Param("branchName") String branchName);

    /**
     * 根据聚合类型查询聚合说明书
     */
    @Select("""
                SELECT id, title, summary, content, project_id, branch_name,
                       aggregation_type, status, created_at, updated_at
                FROM aggregated_documentation
                WHERE aggregation_type = #{aggregationType}
                ORDER BY created_at DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "aggregationType", column = "aggregation_type"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<AggregatedDocumentation> findByAggregationType(String aggregationType);

    /**
     * 根据状态查询聚合说明书
     */
    @Select("""
                SELECT id, title, summary, content, project_id, branch_name,
                       aggregation_type, status, created_at, updated_at
                FROM aggregated_documentation
                WHERE status = #{status}
                ORDER BY created_at DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "content", column = "content"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "aggregationType", column = "aggregation_type"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<AggregatedDocumentation> findByStatus(String status);

    /**
     * 更新聚合说明书
     */
    @Update("""
                UPDATE aggregated_documentation SET
                    title = #{title}, summary = #{summary}, content = #{content},
                    aggregation_type = #{aggregationType}, status = #{status}, updated_at = #{updatedAt}
                WHERE id = #{id}
            """)
    int update(AggregatedDocumentation aggregatedDocumentation);

    /**
     * 删除聚合说明书
     */
    @Delete("DELETE FROM aggregated_documentation WHERE id = #{id}")
    int deleteById(String id);

    /**
     * 统计聚合说明书总数
     */
    @Select("SELECT COUNT(*) FROM aggregated_documentation")
    long count();

    /**
     * 根据项目ID统计聚合说明书数量
     */
    @Select("SELECT COUNT(*) FROM aggregated_documentation WHERE project_id = #{projectId}")
    long countByProjectId(String projectId);

    /**
     * 检查指定项目和分支是否已有聚合说明书
     */
    @Select("SELECT COUNT(*) > 0 FROM aggregated_documentation WHERE project_id = #{projectId} AND branch_name = #{branchName}")
    boolean existsByProjectIdAndBranchName(@Param("projectId") String projectId, @Param("branchName") String branchName);

    /**
     * 查询所有已接入的项目ID
     */
    @Select("""
                SELECT DISTINCT project_id, COUNT(*) as documentation_count
                FROM aggregated_documentation
                GROUP BY project_id
                ORDER BY project_id
            """)
    @Results({
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "documentationCount", column = "documentation_count")
    })
    List<com.puti.code.documentation.controller.response.ProjectInfo> findAllProjects();

    /**
     * 根据项目ID查询聚合说明书摘要信息
     */
    @Select("""
                SELECT id, title, summary, project_id, branch_name, aggregation_type
                FROM aggregated_documentation
                WHERE project_id = #{projectId}
                ORDER BY created_at DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name"),
            @Result(property = "aggregationType", column = "aggregation_type")
    })
    List<com.puti.code.documentation.controller.response.AggregatedDocumentationSummary> findSummariesByProjectId(@Param("projectId") String projectId);

    /**
     * 根据ID查询聚合说明书内容
     */
    @Select("SELECT content FROM aggregated_documentation WHERE id = #{id}")
    String findContentById(@Param("id") String id);
}
