package com.puti.code.documentation.controller;

import com.puti.code.base.entity.rdb.DocumentationAggregation;
import com.puti.code.base.model.MethodInfo;
import com.puti.code.documentation.dto.DocumentationGenerationDto;
import com.puti.code.documentation.repository.graph.EntryPointRepository;
import com.puti.code.documentation.repository.sql.mapper.DocumentationAggregationMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Resource
    private DocumentationAggregationMapper mapper;

    @Resource
    private EntryPointRepository entryPointRepository;

    @GetMapping("/batchInsert")
    public void test(){
        List<DocumentationAggregation> documentationAggregationList = new ArrayList<>();
        DocumentationAggregation documentationAggregation = new DocumentationAggregation();
        documentationAggregation.setAggregatedDocumentationId("111");
        documentationAggregation.setAggregatedDocumentationId("111");
        documentationAggregation.setDocumentationId("111");
        documentationAggregation.setEntryPointId("111");
        documentationAggregation.setEntryPointName("xxx");
        documentationAggregation.setWeight(1);
        documentationAggregation.setCreatedAt(LocalDateTime.now());
        documentationAggregationList.add(documentationAggregation);
        int result = mapper.batchInsert(documentationAggregationList);
    }

    /**
     * 获取所有入口点信息
     * 参考 DocumentationGenerator#generateDocumentationForAllEntryPoints
     *
     * @param projectId 项目ID
     * @param branchName 分支名称
     * @return 入口点列表
     */
    @GetMapping("/entryPoints")
    public List<MethodInfo> getAllEntryPoints(
            @RequestParam String projectId,
            @RequestParam String branchName) {

        DocumentationGenerationDto dto = DocumentationGenerationDto.builder()
                .projectId(projectId)
                .branchName(branchName)
                .build();

        return entryPointRepository.getAllEntryPoints(dto);
    }
}
