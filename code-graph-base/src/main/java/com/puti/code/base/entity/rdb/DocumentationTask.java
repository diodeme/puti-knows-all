package com.puti.code.base.entity.rdb;

import com.puti.code.base.annotation.AutoKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 说明书生成任务实体
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationTask {
    
    /**
     * 任务ID
     */
    @AutoKey
    private String id;
    
    /**
     * 入口节点ID
     */
    private String entryPointId;
    
    /**
     * 入口节点名称
     */
    private String entryPointName;
    
    /**
     * 目标生成层级（1-3）
     */
    private Integer targetLevel;
    
    /**
     * 当前处理层级
     */
    private Integer currentLevel;
    
    /**
     * 任务状态
     */
    private TaskStatus status;
    
    /**
     * 进度百分比（0-100）
     */
    private Integer progress;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 项目ID
     */
    private String projectId;
    
    /**
     * 分支名称
     */
    private String branchName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
    
    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING("待处理"),
        RUNNING("运行中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        CANCELLED("已取消");
        
        private final String description;
        
        TaskStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 检查任务是否正在运行
     */
    public boolean isRunning() {
        return status == TaskStatus.PENDING || status == TaskStatus.RUNNING;
    }
    
    /**
     * 检查任务是否已完成
     */
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED || 
               status == TaskStatus.FAILED || 
               status == TaskStatus.CANCELLED;
    }
    
    /**
     * 获取任务进度描述
     */
    public String getProgressDescription() {
        if (status == TaskStatus.PENDING) {
            return "等待开始";
        } else if (status == TaskStatus.RUNNING) {
            return String.format("正在处理第%d层 (%d%%)", currentLevel, progress);
        } else if (status == TaskStatus.COMPLETED) {
            return "生成完成";
        } else if (status == TaskStatus.FAILED) {
            return "生成失败: " + (errorMessage != null ? errorMessage : "未知错误");
        } else if (status == TaskStatus.CANCELLED) {
            return "已取消";
        }
        return "未知状态";
    }
    
    /**
     * 计算任务执行时长（毫秒）
     */
    public Long getExecutionDuration() {
        if (createdAt == null) {
            return null;
        }
        
        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(createdAt, endTime).toMillis();
    }
}
