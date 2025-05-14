package com.puti.code.server.controller;

import com.puti.code.server.dto.ApiResponse;
import com.puti.code.server.dto.MethodSearchRequest;
import com.puti.code.server.dto.MethodSearchResult;
import com.puti.code.server.dto.NodeDetail;
import com.puti.code.server.dto.NodeDetailRequest;
import com.puti.code.server.dto.NodeRequest;
import com.puti.code.server.dto.NodeResponse;
import com.puti.code.server.service.GraphQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class GraphQueryController {

    private final GraphQueryService graphQueryService;

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<MethodSearchResult>>> searchMethods(@RequestBody MethodSearchRequest request) {
        log.info("Search method: {}", request.getMethodName());
        try {
            List<MethodSearchResult> result = graphQueryService.searchMethodByName(request.getMethodName());
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Failed to search method", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Search failed: " + e.getMessage()));
        }
    }

    @PostMapping("/nodes")
    public ResponseEntity<?> getNodes(@RequestBody NodeRequest request) {
        log.info("Get nodes: method={}, queryType={}, depth={}",
                request.getMethodFullName(), request.getQueryType(), request.getPathDepth());
        try {
            NodeResponse response = graphQueryService.getNodes(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get nodes", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid query type or execution failed"));
        }
    }

    @PostMapping("/node_detail")
    public ResponseEntity<?> getNodeDetail(@RequestBody NodeDetailRequest request) {
        log.info("Get node detail: {}", request.getNodeId());
        try {
            NodeDetail detail = graphQueryService.getNodeDetail(request.getNodeId());
            if (detail == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("Failed to get node detail", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()));
        }
    }
}
