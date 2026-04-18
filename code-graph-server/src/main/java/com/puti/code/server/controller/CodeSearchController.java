package com.puti.code.server.controller;

import com.puti.code.server.dto.CodeSearchData;
import com.puti.code.server.dto.CodeSearchRequest;
import com.puti.code.server.dto.EntryPointsData;
import com.puti.code.server.dto.EntryPointsRequest;
import com.puti.code.server.service.CodeSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CodeSearchController {

    private final CodeSearchService codeSearchService;

    @PostMapping("/code_search")
    public ResponseEntity<CodeSearchData> codeSearch(@RequestBody CodeSearchRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Code search: query={}, topK={}, strategy={}",
                request.getQuery(), request.getTopK(), request.getStrategy());

        long startTime = System.currentTimeMillis();
        CodeSearchData data = codeSearchService.search(request);
        data.getMeta().setElapsedMs(System.currentTimeMillis() - startTime);

        return ResponseEntity.ok(data);
    }

    @PostMapping("/get_entry_points")
    public ResponseEntity<EntryPointsData> getEntryPoints(@RequestBody EntryPointsRequest request) {
        log.info("Get entry points: repoId={}, branchName={}", request.getRepoId(), request.getBranchName());

        long startTime = System.currentTimeMillis();
        EntryPointsData data = codeSearchService.getEntryPoints(request.getRepoId(), request.getBranchName());
        data.getMeta().setElapsedMs(System.currentTimeMillis() - startTime);

        return ResponseEntity.ok(data);
    }
}
