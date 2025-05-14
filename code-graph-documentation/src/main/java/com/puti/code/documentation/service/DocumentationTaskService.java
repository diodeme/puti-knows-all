package com.puti.code.documentation.service;

import com.puti.code.base.entity.rdb.DocumentationTask;
import com.puti.code.base.model.MethodInfo;
import com.puti.code.documentation.config.DocumentationConfig;
import com.puti.code.documentation.dto.DocumentationGenerationDto;
import com.puti.code.documentation.repository.sql.mapper.DocumentationTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档生成任务管理服务
 * 负责任务的创建、状态管理、进度跟踪等
 * 
 * @author diodehe
 */
@Slf4j
@Service
public class DocumentationTaskService {

    private final DocumentationTaskMapper taskMapper;
    private final Map<String, DocumentationTask> activeTasks;

    @Autowired
    public DocumentationTaskService(DocumentationTaskMapper taskMapper, DocumentationConfig config) {
        this.taskMapper = taskMapper;
        this.activeTasks = new ConcurrentHashMap<>();
    }
    
    /**
     * 创建新的文档生成任务
     * 
     * @param dto 请求体
     * @param entryPoint 入口点信息
     * @return 创建的任务
     */
    public DocumentationTask createTask(DocumentationGenerationDto dto, MethodInfo entryPoint) {
        try {
            String entryPointId = entryPoint.getMethodId();
            Integer level = dto.getLevel();
            log.info("创建文档生成任务，入口点: {}, 目标层级: {}", entryPointId, level);
            
            // 验证参数
            if (entryPointId == null || entryPointId.trim().isEmpty()) {
                throw new IllegalArgumentException("入口点ID不能为空");
            }
            
            if (level < 1 || level > 3) {
                throw new IllegalArgumentException("目标层级必须在1-3之间");
            }
            
            // 检查是否已有运行中的任务
            DocumentationTask existingTask = getRunningTaskForEntryPoint(entryPointId);
            if (existingTask != null) {
                log.warn("入口点 {} 已有运行中的任务: {}", entryPointId, existingTask.getId());
                return existingTask;
            }
            
            // 创建新任务
            DocumentationTask task = DocumentationTask.builder()
                    .entryPointId(entryPointId)
                    .entryPointName(entryPoint.getFullName())
                    .targetLevel(level)
                    .currentLevel(0)
                    .status(DocumentationTask.TaskStatus.PENDING)
                    .progress(0)
                    .projectId(dto.getProjectId())
                    .branchName(dto.getBranchName())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            // 保存任务
            taskMapper.insert(task);
            activeTasks.put(task.getId(), task);
            
            log.info("成功创建任务: {}", task.getId());
            return task;
            
        } catch (Exception e) {
            log.error("创建任务时发生错误", e);
            throw new RuntimeException("创建任务失败", e);
        }
    }
    
    /**
     * 更新任务状态
     * 
     * @param taskId 任务ID
     * @param status 新状态
     */
    public void updateTaskStatus(String taskId, DocumentationTask.TaskStatus status) {
        try {
            DocumentationTask task = getTaskById(taskId);
            if (task == null) {
                log.warn("任务不存在: {}", taskId);
                return;
            }
            
            log.debug("更新任务 {} 状态: {} -> {}", taskId, task.getStatus(), status);
            
            task.setStatus(status);
            task.setUpdatedAt(LocalDateTime.now());
            
            if (status == DocumentationTask.TaskStatus.COMPLETED) {
                task.setProgress(100);
                task.setCompletedAt(LocalDateTime.now());
            } else if (status == DocumentationTask.TaskStatus.FAILED || 
                      status == DocumentationTask.TaskStatus.CANCELLED) {
                task.setCompletedAt(LocalDateTime.now());
            }
            
            // 更新缓存和持久化
            activeTasks.put(taskId, task);
            taskMapper.update(task);
            
        } catch (Exception e) {
            log.error("更新任务状态时发生错误", e);
        }
    }
    
    /**
     * 更新任务进度
     * 
     * @param taskId 任务ID
     * @param currentLevel 当前处理层级
     * @param progress 进度百分比
     */
    public void updateTaskProgress(String taskId, int currentLevel, int progress) {
        try {
            DocumentationTask task = getTaskById(taskId);
            if (task == null) {
                log.warn("任务不存在: {}", taskId);
                return;
            }
            
            log.debug("更新任务 {} 进度: 层级 {}, 进度 {}%", taskId, currentLevel, progress);
            
            task.setCurrentLevel(currentLevel);
            task.setProgress(Math.min(100, Math.max(0, progress)));
            task.setUpdatedAt(LocalDateTime.now());
            
            // 更新缓存和持久化
            activeTasks.put(taskId, task);
            taskMapper.update(task);
            
        } catch (Exception e) {
            log.error("更新任务进度时发生错误", e);
        }
    }
    
    /**
     * 完成任务
     * 
     * @param taskId 任务ID
     */
    public void completeTask(String taskId) {
        updateTaskStatus(taskId, DocumentationTask.TaskStatus.COMPLETED);
        log.info("任务 {} 已完成", taskId);
    }
    
    /**
     * 任务失败
     * 
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    public void failTask(String taskId, String errorMessage) {
        try {
            DocumentationTask task = getTaskById(taskId);
            if (task == null) {
                log.warn("任务不存在: {}", taskId);
                return;
            }
            
            task.setStatus(DocumentationTask.TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            task.setUpdatedAt(LocalDateTime.now());
            task.setCompletedAt(LocalDateTime.now());
            
            activeTasks.put(taskId, task);
            taskMapper.update(task);
            
            log.error("任务 {} 失败: {}", taskId, errorMessage);
            
        } catch (Exception e) {
            log.error("标记任务失败时发生错误", e);
        }
    }
    
    /**
     * 取消任务
     * 
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public boolean cancelTask(String taskId) {
        try {
            DocumentationTask task = getTaskById(taskId);
            if (task == null) {
                log.warn("任务不存在: {}", taskId);
                return false;
            }
            
            if (task.getStatus() == DocumentationTask.TaskStatus.COMPLETED ||
                task.getStatus() == DocumentationTask.TaskStatus.FAILED ||
                task.getStatus() == DocumentationTask.TaskStatus.CANCELLED) {
                log.warn("任务 {} 已完成，无法取消", taskId);
                return false;
            }
            
            updateTaskStatus(taskId, DocumentationTask.TaskStatus.CANCELLED);
            log.info("任务 {} 已取消", taskId);
            return true;
            
        } catch (Exception e) {
            log.error("取消任务时发生错误", e);
            return false;
        }
    }
    
    /**
     * 根据ID获取任务
     * 
     * @param taskId 任务ID
     * @return 任务信息
     */
    public DocumentationTask getTaskById(String taskId) {
        // 先从缓存中查找
        DocumentationTask task = activeTasks.get(taskId);
        if (task != null) {
            return task;
        }
        
        // 从持久化存储中查找
        task = taskMapper.findById(taskId);
        if (task != null) {
            activeTasks.put(taskId, task);
        }
        
        return task;
    }
    
    /**
     * 获取入口点的运行中任务
     * 
     * @param entryPointId 入口点ID
     * @return 运行中的任务，如果没有返回null
     */
    public DocumentationTask getRunningTaskForEntryPoint(String entryPointId) {
        // 先从缓存查找
        DocumentationTask cachedTask = activeTasks.values().stream()
                .filter(task -> entryPointId.equals(task.getEntryPointId()))
                .filter(task -> task.getStatus() == DocumentationTask.TaskStatus.PENDING ||
                               task.getStatus() == DocumentationTask.TaskStatus.RUNNING)
                .findFirst()
                .orElse(null);

        if (cachedTask != null) {
            return cachedTask;
        }

        // 从持久化存储查找
        return taskMapper.findRunningTaskByEntryPointId(entryPointId);
    }
    
    /**
     * 获取运行中任务数量
     */
    public int getRunningTasksCount() {
        return (int) activeTasks.values().stream()
                .filter(task -> task.getStatus() == DocumentationTask.TaskStatus.PENDING ||
                               task.getStatus() == DocumentationTask.TaskStatus.RUNNING)
                .count();
    }
    
    /**
     * 获取已完成任务数量
     */
    public int getCompletedTasksCount() {
        return (int) activeTasks.values().stream()
                .filter(task -> task.getStatus() == DocumentationTask.TaskStatus.COMPLETED)
                .count();
    }
    
    /**
     * 获取失败任务数量
     */
    public int getFailedTasksCount() {
        return (int) activeTasks.values().stream()
                .filter(task -> task.getStatus() == DocumentationTask.TaskStatus.FAILED)
                .count();
    }
    
    /**
     * 清理过期任务
     */
    public CompletableFuture<Void> cleanupExpiredTasks() {
        return CompletableFuture.runAsync(() -> {
            try {
                LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
                
                List<String> expiredTaskIds = activeTasks.values().stream()
                        .filter(task -> task.getCompletedAt() != null && 
                                       task.getCompletedAt().isBefore(cutoffTime))
                        .map(DocumentationTask::getId)
                        .toList();
                
                for (String taskId : expiredTaskIds) {
                    activeTasks.remove(taskId);
                    //todo 待实现
//                    deleteTask(taskId);
                }
                
                log.info("清理了 {} 个过期任务", expiredTaskIds.size());
                
            } catch (Exception e) {
                log.error("清理过期任务时发生错误", e);
            }
        });
    }
    
    /**
     * 获取所有活跃任务
     */
    public List<DocumentationTask> getAllActiveTasks() {
        return List.copyOf(activeTasks.values());
    }
    

}
