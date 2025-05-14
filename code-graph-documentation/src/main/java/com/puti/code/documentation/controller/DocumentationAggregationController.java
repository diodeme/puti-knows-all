package com.puti.code.documentation.controller;

import com.puti.code.base.entity.rdb.AggregatedDocumentation;
import com.puti.code.documentation.controller.request.DocumentationAggregationRequest;
import com.puti.code.documentation.repository.sql.AggregatedDocumentationRepository;
import com.puti.code.documentation.service.DocumentationAggregationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 说明书聚合REST控制器
 * 
 * @author diodehe
 */
@Slf4j
@RestController
@RequestMapping("/api/documentation/aggregation")
public class DocumentationAggregationController {
    
    @Autowired
    private DocumentationAggregationService aggregationService;
    
    @Autowired
    private AggregatedDocumentationRepository aggregatedDocumentationRepository;
    
    /**
     * 聚合指定项目和分支的所有说明书
     */
    @PostMapping("/aggregate")
    public ResponseEntity<?> aggregateDocumentations(@Valid @RequestBody DocumentationAggregationRequest request) {
        try {
            log.info("开始聚合说明书 - 项目: {}, 分支: {}", request.getProjectId(), request.getBranchName());
            
            CompletableFuture<List<String>> future = aggregationService.aggregateDocumentations(
                    request.getProjectId(), 
                    request.getBranchName(), 
                    request.getForceRegenerate()
            );
            
            List<String> aggregatedDocumentationIds = future.join();
            
            log.info("成功完成说明书聚合，项目: {}, 分支: {}, 生成聚合说明书数量: {}", 
                    request.getProjectId(), request.getBranchName(), aggregatedDocumentationIds.size());
            
            return ResponseEntity.ok().body(new ApiResponse<>(true, 
                    "成功聚合 " + aggregatedDocumentationIds.size() + " 个说明书", 
                    aggregatedDocumentationIds));
            
        } catch (Exception e) {
            log.error("聚合说明书时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, 
                    "聚合失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取指定项目和分支的聚合说明书列表
     */
    @GetMapping("/list")
    public ResponseEntity<?> getAggregatedDocumentations(@RequestParam String projectId,
                                                        @RequestParam String branchName) {
        try {
            List<AggregatedDocumentation> aggregatedDocs = aggregatedDocumentationRepository
                    .findByProjectIdAndBranchName(projectId, branchName);
            
            return ResponseEntity.ok().body(new ApiResponse<>(true, 
                    "获取聚合说明书列表成功", aggregatedDocs));
            
        } catch (Exception e) {
            log.error("获取聚合说明书列表时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, 
                    "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 根据ID获取聚合说明书详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAggregatedDocumentation(@PathVariable String id) {
        try {
            AggregatedDocumentation aggregatedDoc = aggregatedDocumentationRepository.findById(id)
                    .orElse(null);
            
            if (aggregatedDoc == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok().body(new ApiResponse<>(true, 
                    "获取聚合说明书详情成功", aggregatedDoc));
            
        } catch (Exception e) {
            log.error("获取聚合说明书详情时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, 
                    "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 根据聚合类型获取聚合说明书
     */
    @GetMapping("/type/{aggregationType}")
    public ResponseEntity<?> getAggregatedDocumentationsByType(@PathVariable String aggregationType) {
        try {
            List<AggregatedDocumentation> aggregatedDocs = aggregatedDocumentationRepository
                    .findByAggregationType(aggregationType);
            
            return ResponseEntity.ok().body(new ApiResponse<>(true, 
                    "获取指定类型聚合说明书成功", aggregatedDocs));
            
        } catch (Exception e) {
            log.error("获取指定类型聚合说明书时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, 
                    "获取失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 删除聚合说明书
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAggregatedDocumentation(@PathVariable String id) {
        try {
            boolean success = aggregatedDocumentationRepository.deleteById(id);
            
            if (success) {
                return ResponseEntity.ok().body(new ApiResponse<>(true, 
                        "删除聚合说明书成功", null));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("删除聚合说明书时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, 
                    "删除失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取聚合统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getAggregationStatistics() {
        try {
            long totalCount = aggregatedDocumentationRepository.count();
            
            AggregationStatistics stats = AggregationStatistics.builder()
                    .totalAggregatedDocumentations(totalCount)
                    .build();
            
            return ResponseEntity.ok().body(new ApiResponse<>(true, 
                    "获取聚合统计信息成功", stats));
            
        } catch (Exception e) {
            log.error("获取聚合统计信息时发生错误", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, 
                    "获取失败: " + e.getMessage(), null));
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
    
    /**
     * 聚合统计信息
     */
    public static class AggregationStatistics {
        private long totalAggregatedDocumentations;
        
        public static AggregationStatisticsBuilder builder() {
            return new AggregationStatisticsBuilder();
        }
        
        public long getTotalAggregatedDocumentations() { return totalAggregatedDocumentations; }
        public void setTotalAggregatedDocumentations(long totalAggregatedDocumentations) { 
            this.totalAggregatedDocumentations = totalAggregatedDocumentations; 
        }
        
        public static class AggregationStatisticsBuilder {
            private long totalAggregatedDocumentations;
            
            public AggregationStatisticsBuilder totalAggregatedDocumentations(long totalAggregatedDocumentations) {
                this.totalAggregatedDocumentations = totalAggregatedDocumentations;
                return this;
            }
            
            public AggregationStatistics build() {
                AggregationStatistics stats = new AggregationStatistics();
                stats.totalAggregatedDocumentations = this.totalAggregatedDocumentations;
                return stats;
            }
        }
    }
}
