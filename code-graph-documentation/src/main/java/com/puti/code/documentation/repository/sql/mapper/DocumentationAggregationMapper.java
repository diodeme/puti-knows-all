package com.puti.code.documentation.repository.sql.mapper;

import com.puti.code.base.entity.rdb.DocumentationAggregation;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 说明书聚合关系数据访问Mapper
 *
 * @author diodehe
 */
@Mapper
@Repository
public interface DocumentationAggregationMapper {

    /**
     * 插入聚合关系
     */
    @Insert("""
                INSERT INTO documentation_aggregation (
                    id, aggregated_documentation_id, documentation_id, entry_point_id,
                    entry_point_name, weight, created_at
                ) VALUES (
                    #{id}, #{aggregatedDocumentationId}, #{documentationId}, #{entryPointId},
                    #{entryPointName}, #{weight}, #{createdAt}
                )
            """)
    int insert(DocumentationAggregation aggregation);

    /**
     * 批量插入聚合关系
     */
    @Insert({
            "<script>",
            "INSERT INTO documentation_aggregation (",
            "id, aggregated_documentation_id, documentation_id, entry_point_id,",
            "entry_point_name, weight, created_at",
            ") VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.id}, #{item.aggregatedDocumentationId}, #{item.documentationId}, #{item.entryPointId},",
            "#{item.entryPointName}, #{item.weight}, #{item.createdAt})",
            "</foreach>",
            "</script>"
    })
    int batchInsert(@Param("list") List<DocumentationAggregation> aggregations);

    /**
     * 根据ID查询聚合关系
     */
    @Select("""
                SELECT id, aggregated_documentation_id, documentation_id, entry_point_id,
                       entry_point_name, weight, created_at
                FROM documentation_aggregation
                WHERE id = #{id}
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "aggregatedDocumentationId", column = "aggregated_documentation_id"),
            @Result(property = "documentationId", column = "documentation_id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "weight", column = "weight"),
            @Result(property = "createdAt", column = "created_at")
    })
    DocumentationAggregation findById(String id);

    /**
     * 根据聚合说明书ID查询所有关系
     */
    @Select("""
                SELECT id, aggregated_documentation_id, documentation_id, entry_point_id,
                       entry_point_name, weight, created_at
                FROM documentation_aggregation
                WHERE aggregated_documentation_id = #{aggregatedDocumentationId}
                ORDER BY weight DESC, created_at ASC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "aggregatedDocumentationId", column = "aggregated_documentation_id"),
            @Result(property = "documentationId", column = "documentation_id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "weight", column = "weight"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<DocumentationAggregation> findByAggregatedDocumentationId(String aggregatedDocumentationId);

    /**
     * 根据原始说明书ID查询聚合关系
     */
    @Select("""
                SELECT id, aggregated_documentation_id, documentation_id, entry_point_id,
                       entry_point_name, weight, created_at
                FROM documentation_aggregation
                WHERE documentation_id = #{documentationId}
                ORDER BY created_at DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "aggregatedDocumentationId", column = "aggregated_documentation_id"),
            @Result(property = "documentationId", column = "documentation_id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "weight", column = "weight"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<DocumentationAggregation> findByDocumentationId(String documentationId);

    /**
     * 根据入口点ID查询聚合关系
     */
    @Select("""
                SELECT id, aggregated_documentation_id, documentation_id, entry_point_id,
                       entry_point_name, weight, created_at
                FROM documentation_aggregation
                WHERE entry_point_id = #{entryPointId}
                ORDER BY created_at DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "aggregatedDocumentationId", column = "aggregated_documentation_id"),
            @Result(property = "documentationId", column = "documentation_id"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "weight", column = "weight"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<DocumentationAggregation> findByEntryPointId(String entryPointId);

    /**
     * 删除聚合关系
     */
    @Delete("DELETE FROM documentation_aggregation WHERE id = #{id}")
    int deleteById(String id);

    /**
     * 根据聚合说明书ID删除所有关系
     */
    @Delete("DELETE FROM documentation_aggregation WHERE aggregated_documentation_id = #{aggregatedDocumentationId}")
    int deleteByAggregatedDocumentationId(String aggregatedDocumentationId);

    /**
     * 统计聚合关系总数
     */
    @Select("SELECT COUNT(*) FROM documentation_aggregation")
    long count();

    /**
     * 根据聚合说明书ID查询关联的流程说明书摘要信息
     */
    @Select("""
                SELECT d.id, d.title, d.summary, d.entry_point_id, d.entry_point_name,
                       d.project_id, d.branch_name
                FROM documentation d
                INNER JOIN documentation_aggregation da ON d.id = da.documentation_id
                WHERE da.aggregated_documentation_id = #{aggregatedDocumentationId}
                ORDER BY da.weight DESC, da.created_at ASC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "entryPointId", column = "entry_point_id"),
            @Result(property = "entryPointName", column = "entry_point_name"),
            @Result(property = "projectId", column = "project_id"),
            @Result(property = "branchName", column = "branch_name")
    })
    List<com.puti.code.documentation.controller.response.ProcessDocumentationSummary> findProcessDocumentationsByAggregatedId(@Param("aggregatedDocumentationId") String aggregatedDocumentationId);
}
