package com.puti.code.documentation.controller;

import com.puti.code.documentation.controller.request.BatchQueryRequest;
import com.puti.code.documentation.controller.response.AggregatedDocumentationResponse;
import com.puti.code.documentation.controller.response.ProcessDocumentationResponse;
import com.puti.code.documentation.service.DocumentationQueryService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 说明书查询控制器
 * 提供聚合说明书和流程说明书的查询接口
 * 
 * @author diodehe
 */
@Slf4j
@RestController
@RequestMapping("/api/documentation/query")
public class DocumentationQueryController {
    
    @Autowired
    private DocumentationQueryService documentationQueryService;
    
    /**
     * 批量查询聚合说明书
     *
     * @param request 批量查询请求
     * @return 聚合说明书内容列表
     */
    @PostMapping("/aggregated/batch")
    public ResponseEntity<?> getAggregatedDocumentationBatch(@RequestBody BatchQueryRequest request) {
        try {
            log.info("接收到批量聚合说明书查询请求，documentIds: {}", request.getDocumentIds());

            if (request == null || request.getDocumentIds() == null || request.getDocumentIds().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "说明书ID列表不能为空", null));
            }

            // 验证ID列表中是否有空值
            if (request.getDocumentIds().stream().anyMatch(id -> id == null || id.trim().isEmpty())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "说明书ID列表中不能包含空值", null));
            }

            List<AggregatedDocumentationResponse> result =
                    documentationQueryService.getAggregatedDocumentationBatch(request.getDocumentIds());

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(true, "查询成功", result));

        } catch (Exception e) {
            log.error("批量查询聚合说明书时发生错误，documentIds: {}",
                     request != null ? request.getDocumentIds() : null, e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "查询失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 批量查询流程说明书
     *
     * @param request 批量查询请求
     * @return 流程说明书内容及关联方法信息列表
     */
    @PostMapping("/process/batch")
    public ResponseEntity<?> getProcessDocumentationBatch(@RequestBody BatchQueryRequest request) {
        try {
            log.info("接收到批量流程说明书查询请求，documentIds: {}", request.getDocumentIds());

            if (request == null || request.getDocumentIds() == null || request.getDocumentIds().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "说明书ID列表不能为空", null));
            }

            // 验证ID列表中是否有空值
            if (request.getDocumentIds().stream().anyMatch(id -> id == null || id.trim().isEmpty())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "说明书ID列表中不能包含空值", null));
            }

            List<ProcessDocumentationResponse> result =
                    documentationQueryService.getProcessDocumentationBatch(request.getDocumentIds());

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(true, "查询成功", result));

        } catch (Exception e) {
            log.error("批量查询流程说明书时发生错误，documentIds: {}",
                     request != null ? request.getDocumentIds() : null, e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "查询失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * API响应包装类
     */
    @Setter
    @Getter
    public static class ApiResponse<T> {
        // Setters
        // Getters
        private boolean success;
        private String message;
        private T data;
        
        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

    }
}
