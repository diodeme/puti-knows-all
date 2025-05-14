package com.puti.code.documentation.repository.sql.mapper;

import com.puti.code.base.entity.rdb.DocumentationTask;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 说明书生成任务数据访问Mapper
 * 
 * @author diodehe
 */
@Mapper
@Repository
public interface DocumentationTaskMapper {
    
    /**
     * 插入任务
     */
    @Insert("""
        INSERT INTO documentation_task (
            id, entry_point_id, entry_point_name, target_level, current_level, status,
            progress, error_message, project_id, branch_name, created_at, updated_at
        ) VALUES (
            #{id}, #{entryPointId}, #{entryPointName}, #{targetLevel}, #{currentLevel}, #{status},
            #{progress}, #{errorMessage}, #{projectId}, #{branchName}, #{createdAt}, #{updatedAt}
        )
    """)
    int insert(DocumentationTask task);
    
    /**
     * 根据ID查询任务
     */
    @Select("""
        SELECT id, entry_point_id, entry_point_name, target_level, current_level, status,
               progress, error_message, project_id, branch_name, created_at, updated_at, completed_at
        FROM documentation_task 
        WHERE id = #{id}
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "entryPointId", column = "entry_point_id"),
        @Result(property = "entryPointName", column = "entry_point_name"),
        @Result(property = "targetLevel", column = "target_level"),
        @Result(property = "currentLevel", column = "current_level"),
        @Result(property = "status", column = "status"),
        @Result(property = "progress", column = "progress"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "branchName", column = "branch_name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "completedAt", column = "completed_at")
    })
    DocumentationTask findById(@Param("id") String id);
    
    /**
     * 根据入口点ID查询任务列表
     */
    @Select("""
        SELECT id, entry_point_id, entry_point_name, target_level, current_level, status,
               progress, error_message, project_id, branch_name, created_at, updated_at, completed_at
        FROM documentation_task 
        WHERE entry_point_id = #{entryPointId}
        ORDER BY created_at DESC
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "entryPointId", column = "entry_point_id"),
        @Result(property = "entryPointName", column = "entry_point_name"),
        @Result(property = "targetLevel", column = "target_level"),
        @Result(property = "currentLevel", column = "current_level"),
        @Result(property = "status", column = "status"),
        @Result(property = "progress", column = "progress"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "branchName", column = "branch_name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "completedAt", column = "completed_at")
    })
    List<DocumentationTask> findByEntryPointId(@Param("entryPointId") String entryPointId);
    
    /**
     * 根据入口点ID查询运行中的任务
     */
    @Select("""
        SELECT id, entry_point_id, entry_point_name, target_level, current_level, status,
               progress, error_message, project_id, branch_name, created_at, updated_at, completed_at
        FROM documentation_task 
        WHERE entry_point_id = #{entryPointId} AND status IN ('PENDING', 'RUNNING')
        ORDER BY created_at DESC
        LIMIT 1
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "entryPointId", column = "entry_point_id"),
        @Result(property = "entryPointName", column = "entry_point_name"),
        @Result(property = "targetLevel", column = "target_level"),
        @Result(property = "currentLevel", column = "current_level"),
        @Result(property = "status", column = "status"),
        @Result(property = "progress", column = "progress"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "branchName", column = "branch_name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "completedAt", column = "completed_at")
    })
    DocumentationTask findRunningTaskByEntryPointId(@Param("entryPointId") String entryPointId);
    
    /**
     * 根据状态查询任务列表
     */
    @Select("""
        SELECT id, entry_point_id, entry_point_name, target_level, current_level, status,
               progress, error_message, project_id, branch_name, created_at, updated_at, completed_at
        FROM documentation_task 
        WHERE status = #{status}
        ORDER BY created_at DESC
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "entryPointId", column = "entry_point_id"),
        @Result(property = "entryPointName", column = "entry_point_name"),
        @Result(property = "targetLevel", column = "target_level"),
        @Result(property = "currentLevel", column = "current_level"),
        @Result(property = "status", column = "status"),
        @Result(property = "progress", column = "progress"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "branchName", column = "branch_name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "completedAt", column = "completed_at")
    })
    List<DocumentationTask> findByStatus(@Param("status") String status);
    
    /**
     * 查找运行中的任务
     */
    @Select("""
        SELECT id, entry_point_id, entry_point_name, target_level, current_level, status,
               progress, error_message, project_id, branch_name, created_at, updated_at, completed_at
        FROM documentation_task 
        WHERE status IN ('PENDING', 'RUNNING')
        ORDER BY created_at
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "entryPointId", column = "entry_point_id"),
        @Result(property = "entryPointName", column = "entry_point_name"),
        @Result(property = "targetLevel", column = "target_level"),
        @Result(property = "currentLevel", column = "current_level"),
        @Result(property = "status", column = "status"),
        @Result(property = "progress", column = "progress"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "branchName", column = "branch_name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "completedAt", column = "completed_at")
    })
    List<DocumentationTask> findRunningTasks();
    
    /**
     * 根据项目ID查询任务列表
     */
    @Select("""
        SELECT id, entry_point_id, entry_point_name, target_level, current_level, status,
               progress, error_message, project_id, branch_name, created_at, updated_at, completed_at
        FROM documentation_task 
        WHERE project_id = #{projectId}
        ORDER BY created_at DESC
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "entryPointId", column = "entry_point_id"),
        @Result(property = "entryPointName", column = "entry_point_name"),
        @Result(property = "targetLevel", column = "target_level"),
        @Result(property = "currentLevel", column = "current_level"),
        @Result(property = "status", column = "status"),
        @Result(property = "progress", column = "progress"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "branchName", column = "branch_name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "completedAt", column = "completed_at")
    })
    List<DocumentationTask> findByProjectId(@Param("projectId") String projectId);
    
    /**
     * 更新任务
     */
    @Update("""
        UPDATE documentation_task SET 
            entry_point_name = #{entryPointName},
            target_level = #{targetLevel},
            current_level = #{currentLevel},
            status = #{status},
            progress = #{progress},
            error_message = #{errorMessage},
            project_id = #{projectId},
            branch_name = #{branchName},
            updated_at = #{updatedAt},
            completed_at = #{completedAt}
        WHERE id = #{id}
    """)
    int update(DocumentationTask task);
    
    /**
     * 根据ID删除任务
     */
    @Delete("DELETE FROM documentation_task WHERE id = #{id}")
    int deleteById(@Param("id") String id);
    
    /**
     * 批量删除任务
     */
    @Delete("""
        <script>
        DELETE FROM documentation_task WHERE id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
    """)
    int deleteByIds(@Param("ids") List<Long> ids);
    
    /**
     * 删除指定时间之前完成的任务
     */
    @Delete("DELETE FROM documentation_task WHERE completed_at < #{completedBefore}")
    int deleteCompletedTasksBefore(@Param("completedBefore") LocalDateTime completedBefore);
    
    /**
     * 统计任务总数
     */
    @Select("SELECT COUNT(*) FROM documentation_task")
    long count();
    
    /**
     * 根据状态统计任务数量
     */
    @Select("SELECT COUNT(*) FROM documentation_task WHERE status = #{status}")
    long countByStatus(@Param("status") String status);
    
    /**
     * 统计运行中的任务数量
     */
    @Select("SELECT COUNT(*) FROM documentation_task WHERE status IN ('PENDING', 'RUNNING')")
    long countRunningTasks();
    
    /**
     * 根据项目ID统计任务数量
     */
    @Select("SELECT COUNT(*) FROM documentation_task WHERE project_id = #{projectId}")
    long countByProjectId(@Param("projectId") String projectId);
    
    /**
     * 检查入口点是否有运行中的任务
     */
    @Select("SELECT COUNT(*) > 0 FROM documentation_task WHERE entry_point_id = #{entryPointId} AND status IN ('PENDING', 'RUNNING')")
    boolean hasRunningTaskForEntryPoint(@Param("entryPointId") String entryPointId);
    
    /**
     * 查找指定时间之前创建的任务
     */
    @Select("""
        SELECT id, entry_point_id, entry_point_name, target_level, current_level, status,
               progress, error_message, project_id, branch_name, created_at, updated_at, completed_at
        FROM documentation_task 
        WHERE created_at < #{createdBefore}
        ORDER BY created_at
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "entryPointId", column = "entry_point_id"),
        @Result(property = "entryPointName", column = "entry_point_name"),
        @Result(property = "targetLevel", column = "target_level"),
        @Result(property = "currentLevel", column = "current_level"),
        @Result(property = "status", column = "status"),
        @Result(property = "progress", column = "progress"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "branchName", column = "branch_name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "completedAt", column = "completed_at")
    })
    List<DocumentationTask> findByCreatedAtBefore(@Param("createdBefore") LocalDateTime createdBefore);
    
    /**
     * 查找指定时间之前完成的任务
     */
    @Select("""
        SELECT id, entry_point_id, entry_point_name, target_level, current_level, status,
               progress, error_message, project_id, branch_name, created_at, updated_at, completed_at
        FROM documentation_task 
        WHERE completed_at < #{completedBefore}
        ORDER BY completed_at
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "entryPointId", column = "entry_point_id"),
        @Result(property = "entryPointName", column = "entry_point_name"),
        @Result(property = "targetLevel", column = "target_level"),
        @Result(property = "currentLevel", column = "current_level"),
        @Result(property = "status", column = "status"),
        @Result(property = "progress", column = "progress"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "branchName", column = "branch_name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "completedAt", column = "completed_at")
    })
    List<DocumentationTask> findByCompletedAtBefore(@Param("completedBefore") LocalDateTime completedBefore);
}
