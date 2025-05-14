package com.puti.code.documentation.controller;

import com.puti.code.base.util.Json;
import com.puti.code.documentation.controller.request.DocumentationGenerationRequest;
import com.puti.code.documentation.dto.DocumentationGenerationDto;
import com.puti.code.base.entity.rdb.Documentation;
import com.puti.code.base.entity.rdb.DocumentationTask;
import com.puti.code.documentation.generator.DocumentationGenerator;
import com.puti.code.documentation.repository.sql.DocumentationRepository;
import com.puti.code.documentation.service.DocumentationTaskService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 说明书生成REST控制器
 * 
 * @author diodehe
 */
@Slf4j
@RestController
@RequestMapping("/api/documentation")
public class DocumentationController {
    
    @Autowired
    private DocumentationGenerator documentationGenerator;
    
    @Autowired
    private DocumentationTaskService taskService;
    
    @Autowired
    private DocumentationRepository documentationRepository;
    
    /**
     * 为所有入口点生成说明书
     */
    @PostMapping("/generate/all")
    public ResponseEntity<?> generateForAllEntryPoints(@Valid @RequestBody DocumentationGenerationRequest request) {
        try {
            // 确保这是全量生成请求（entryPointId应该为空）
            if (!request.isGenerateForAll()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "此接口用于全量生成，请不要指定entryPointId", null));
            }

            log.info("开始为所有入口点生成说明书 - {}", Json.toJson(request));
            DocumentationGenerationDto documentationGenerationDto = DocumentationGenerationDto.builder().projectId(request.getProjectId())
                    .branchName(request.getBranchName())
                    .level(request.getLevel())
                    .forceRegenerate(request.getForceRegenerate())
                    .description(request.getDescription()).build();
            CompletableFuture<List<String>> future = documentationGenerator.generateDocumentationForAllEntryPoints(documentationGenerationDto);
            List<String> taskIds = future.join();

            log.info("成功启动 {} 个生成任务，项目: {}, 分支: {}", taskIds.size(), request.getProjectId(), request.getBranchName());

            return ResponseEntity.ok().body(new ApiResponse<>(true, "成功启动 " + taskIds.size() + " 个生成任务", taskIds));

        } catch (Exception e) {
            log.error("为所有入口点生成说明书时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "生成失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 为指定入口点生成说明书
     */
    @PostMapping("/generate/single")
    public ResponseEntity<?> generateForEntryPoint(@Valid @RequestBody DocumentationGenerationRequest request) {
        try {
            // 确保这是单个入口点生成请求（entryPointId不能为空）
            if (request.isGenerateForAll()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "此接口用于单个入口点生成，请指定entryPointId", null));
            }

            log.info("开始为指定入口点生成说明书 - {}", Json.toJson(request));

            DocumentationGenerationDto documentationGenerationDto = DocumentationGenerationDto.builder().projectId(request.getProjectId())
                    .branchName(request.getBranchName())
                    .level(request.getLevel())
                    .forceRegenerate(request.getForceRegenerate())
                    .description(request.getDescription()).build();
            CompletableFuture<String> future = documentationGenerator.generateDocumentationForEntryPoint(documentationGenerationDto, request.getEntryPointId());
            String taskId = future.join();

            log.info("成功启动生成任务 {}, 项目: {}, 分支: {}, 入口点: {}",
                    taskId, request.getProjectId(), request.getBranchName(), request.getEntryPointId());

            return ResponseEntity.ok().body(new ApiResponse<>(true, "成功启动生成任务", taskId));

        } catch (Exception e) {
            log.error("为指定入口点生成说明书时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "生成失败: " + e.getMessage(), null));
        }
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getTaskStatus(@PathVariable String taskId) {
        try {
            DocumentationTask task = taskService.getTaskById(taskId);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok().body(new ApiResponse<>(true, "获取任务状态成功", task));
            
        } catch (Exception e) {
            log.error("获取任务状态时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 取消任务
     */
    @PostMapping("/task/{taskId}/cancel")
    public ResponseEntity<?> cancelTask(@PathVariable String taskId) {
        try {
            boolean cancelled = taskService.cancelTask(taskId);
            if (cancelled) {
                return ResponseEntity.ok().body(new ApiResponse<>(true, "任务已取消", null));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "任务取消失败，可能已完成或不存在", null));
            }
            
        } catch (Exception e) {
            log.error("取消任务时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "取消失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取入口点的说明书
     */
    @GetMapping("/entry-point/{entryPointId}")
    public ResponseEntity<?> getDocumentation(@PathVariable String entryPointId,
                                            @RequestParam(required = false) Integer level) {
        try {
            List<Documentation> documentations;
            
            if (level != null) {
                // 获取指定层级的说明书
                documentations = documentationRepository.findByEntryPointIdAndLevel(entryPointId, level)
                        .map(List::of)
                        .orElse(List.of());
            } else {
                // 获取所有层级的说明书
                documentations = documentationRepository.findByEntryPointId(entryPointId);
            }
            
            if (documentations.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok().body(new ApiResponse<>(true, "获取说明书成功", documentations));
            
        } catch (Exception e) {
            log.error("获取说明书时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取入口点的最终版本说明书
     */
    @GetMapping("/entry-point/{entryPointId}/final")
    public ResponseEntity<?> getFinalDocumentation(@PathVariable String entryPointId) {
        try {
            Documentation documentation = documentationRepository.findFinalVersionByEntryPointId(entryPointId)
                    .orElse(null);
            
            if (documentation == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok().body(new ApiResponse<>(true, "获取最终版本说明书成功", documentation));
            
        } catch (Exception e) {
            log.error("获取最终版本说明书时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取系统统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            DocumentationGenerator.DocumentationStatistics stats = documentationGenerator.getStatistics();
            return ResponseEntity.ok().body(new ApiResponse<>(true, "获取统计信息成功", stats));
            
        } catch (Exception e) {
            log.error("获取统计信息时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取所有活跃任务
     */
    @GetMapping("/tasks")
    public ResponseEntity<?> getAllActiveTasks() {
        try {
            List<DocumentationTask> tasks = taskService.getAllActiveTasks();
            return ResponseEntity.ok().body(new ApiResponse<>(true, "获取活跃任务成功", tasks));
            
        } catch (Exception e) {
            log.error("获取活跃任务时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 清理过期数据
     */
    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanupExpiredData() {
        try {
            documentationGenerator.cleanupExpiredData();
            return ResponseEntity.ok().body(new ApiResponse<>(true, "清理任务已启动", null));
            
        } catch (Exception e) {
            log.error("清理过期数据时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "清理失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * API响应包装类
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        
        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public T getData() { return data; }
        
        // Setters
        public void setSuccess(boolean success) { this.success = success; }
        public void setMessage(String message) { this.message = message; }
        public void setData(T data) { this.data = data; }
    }
}
