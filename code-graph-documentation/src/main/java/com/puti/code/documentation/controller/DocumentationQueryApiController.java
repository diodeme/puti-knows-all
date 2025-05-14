package com.puti.code.documentation.controller;

import com.puti.code.documentation.controller.request.ProjectQueryRequest;
import com.puti.code.documentation.controller.response.*;
import com.puti.code.documentation.dto.PageRequest;
import com.puti.code.documentation.dto.PageResult;
import com.puti.code.documentation.service.DocumentationQueryApiService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 说明书查询API控制器
 * 提供7个查询接口
 * 
 * @author diodehe
 */
@Slf4j
@RestController
@RequestMapping("/api/documentation/query")
public class DocumentationQueryApiController {
    
    @Autowired
    private DocumentationQueryApiService documentationQueryApiService;
    
    /**
     * 1. 查询所有已接入的子系统
     */
    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<ProjectInfo>>> getAllProjects() {
        try {
            log.info("接收到查询所有子系统的请求");
            List<ProjectInfo> projects = documentationQueryApiService.getAllProjects();
            return ResponseEntity.ok(ApiResponse.success("查询成功", projects));
        } catch (Exception e) {
            log.error("查询所有子系统时发生错误", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
    
    /**
     * 2. 查询子系统下所有聚合说明书
     */
    @PostMapping("/projects/aggregated-documentations")
    public ResponseEntity<ApiResponse<List<AggregatedDocumentationSummary>>> getAggregatedDocumentationsByProject(
            @RequestBody ProjectQueryRequest request) {
        try {
            log.info("接收到查询项目 {} 聚合说明书的请求", request.getProjectId());
            List<AggregatedDocumentationSummary> summaries =
                    documentationQueryApiService.getAggregatedDocumentationsByProject(request.getProjectId());
            return ResponseEntity.ok(ApiResponse.success("查询成功", summaries));
        } catch (Exception e) {
            log.error("查询项目 {} 聚合说明书时发生错误", request.getProjectId(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
    
    /**
     * 3. 查询指定聚合说明书下所有流程说明书
     */
    @GetMapping("/aggregated-documentations/{aggregatedDocumentationId}/process-documentations")
    public ResponseEntity<ApiResponse<List<ProcessDocumentationSummary>>> getProcessDocumentationsByAggregated(
            @PathVariable String aggregatedDocumentationId) {
        try {
            log.info("接收到查询聚合说明书 {} 下流程说明书的请求", aggregatedDocumentationId);
            List<ProcessDocumentationSummary> summaries = 
                    documentationQueryApiService.getProcessDocumentationsByAggregated(aggregatedDocumentationId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", summaries));
        } catch (Exception e) {
            log.error("查询聚合说明书 {} 下流程说明书时发生错误", aggregatedDocumentationId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
    
    /**
     * 4. 根据聚合说明书id查询说明书内容
     */
    @GetMapping("/aggregated-documentations/{aggregatedDocumentationId}/content")
    public ResponseEntity<ApiResponse<String>> getAggregatedDocumentationContent(
            @PathVariable String aggregatedDocumentationId) {
        try {
            log.info("接收到查询聚合说明书 {} 内容的请求", aggregatedDocumentationId);
            String content = documentationQueryApiService.getAggregatedDocumentationContent(aggregatedDocumentationId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", content));
        } catch (Exception e) {
            log.error("查询聚合说明书 {} 内容时发生错误", aggregatedDocumentationId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
    
    /**
     * 5. 根据流程说明书id查询说明书内容
     */
    @GetMapping("/process-documentations/{documentationId}/content")
    public ResponseEntity<ApiResponse<String>> getProcessDocumentationContent(
            @PathVariable String documentationId) {
        try {
            log.info("接收到查询流程说明书 {} 内容的请求", documentationId);
            String content = documentationQueryApiService.getProcessDocumentationContent(documentationId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", content));
        } catch (Exception e) {
            log.error("查询流程说明书 {} 内容时发生错误", documentationId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
    
    /**
     * 6. 根据流程说明书id查询关联的所有方法id，支持分页
     */
    @GetMapping("/process-documentations/{documentationId}/methods")
    public ResponseEntity<ApiResponse<PageResult<String>>> getMethodIdsByDocumentation(
            @PathVariable String documentationId,
            @Valid @ModelAttribute PageRequest pageRequest) {
        try {
            log.info("接收到分页查询流程说明书 {} 方法ID的请求，页码: {}, 每页大小: {}", 
                    documentationId, pageRequest.getPage(), pageRequest.getSize());
            PageResult<String> result = documentationQueryApiService.getMethodIdsByDocumentation(documentationId, pageRequest);
            return ResponseEntity.ok(ApiResponse.success("查询成功", result));
        } catch (Exception e) {
            log.error("分页查询流程说明书 {} 方法ID时发生错误", documentationId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
    
    /**
     * 7. 根据方法id查询方法内容
     */
    @GetMapping("/methods/{methodId}/content")
    public ResponseEntity<ApiResponse<MethodContent>> getMethodContent(@PathVariable String methodId) {
        try {
            log.info("接收到查询方法 {} 内容的请求", methodId);
            MethodContent methodContent = documentationQueryApiService.getMethodContent(methodId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", methodContent));
        } catch (Exception e) {
            log.error("查询方法 {} 内容时发生错误", methodId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
}
