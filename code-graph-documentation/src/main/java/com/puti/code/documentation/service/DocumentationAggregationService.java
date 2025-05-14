package com.puti.code.documentation.service;

import com.google.common.reflect.TypeToken;
import com.puti.code.ai.documentation.AIDocumentationAggregationService;
import com.puti.code.ai.documentation.BatchAggregationService;
import com.puti.code.ai.documentation.dto.AggregatedContentDto;
import com.puti.code.ai.documentation.dto.AggregationGroupDto;
import com.puti.code.ai.documentation.dto.DocumentationSummaryDto;
import com.puti.code.ai.vector.SpringVectorGenerator;
import com.puti.code.base.entity.rdb.AggregatedDocumentation;
import com.puti.code.base.entity.rdb.Documentation;
import com.puti.code.base.entity.rdb.DocumentationAggregation;
import com.puti.code.base.entity.vector.DocumentationVector;
import com.puti.code.base.enums.DocumentType;
import com.puti.code.base.util.Json;
import com.puti.code.documentation.repository.sql.AggregatedDocumentationRepository;
import com.puti.code.documentation.repository.sql.DocumentationAggregationRepository;
import com.puti.code.documentation.repository.sql.DocumentationRepository;
import com.puti.code.repository.milvus.DocumentationVectorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 说明书聚合服务
 * 负责整合已生成的流程说明书
 *
 * @author diodehe
 */
@Slf4j
@Service
public class DocumentationAggregationService {

    @Autowired
    private DocumentationRepository documentationRepository;

    @Autowired
    private AggregatedDocumentationRepository aggregatedDocumentationRepository;

    @Autowired
    private DocumentationAggregationRepository documentationAggregationRepository;

    @Autowired
    private AIDocumentationAggregationService aggregationAIService;

    @Autowired
    private BatchAggregationService batchAggregationService;

    @Autowired
    private SpringVectorGenerator vectorGenerator;

    @Autowired
    private DocumentationVectorClient vectorClient;

    /**
     * 聚合指定项目和分支的所有说明书
     *
     * @param projectId       项目ID
     * @param branchName      分支名称
     * @param forceRegenerate 是否强制重新生成
     * @return 聚合任务的Future
     */
    public CompletableFuture<List<String>> aggregateDocumentations(String projectId, String branchName, boolean forceRegenerate) {
        try {
            log.info("开始聚合说明书，项目: {}, 分支: {}", projectId, branchName);

            // 1. 检查是否已存在聚合结果
            if (!forceRegenerate && aggregatedDocumentationRepository.existsByProjectIdAndBranchName(projectId, branchName)) {
                log.info("项目 {} 分支 {} 已存在聚合说明书，跳过聚合", projectId, branchName);
                return CompletableFuture.completedFuture(aggregatedDocumentationRepository.findByProjectIdAndBranchName(projectId, branchName)
                        .stream().map(AggregatedDocumentation::getId).collect(Collectors.toList()));
            }

            // 2. 查询所有说明书
            List<Documentation> documentations = findDocumentationsByProjectAndBranch(projectId, branchName, 3);
            if (CollectionUtils.isEmpty(documentations)) {
                log.warn("项目 {} 分支 {} 没有找到说明书", projectId, branchName);
                return CompletableFuture.completedFuture(List.of());
            }

            // 3. 构建说明书摘要列表
            List<DocumentationSummaryDto> summaries = buildDocumentationSummaries(documentations);

            // 4. 调用AI模型进行聚合分析
            List<AggregationGroupDto> aggregationGroups = analyzeDocumentationGroups(summaries);
            if (CollectionUtils.isEmpty(aggregationGroups)) {
                log.warn("AI模型未返回有效的聚合分组");
                return CompletableFuture.completedFuture(List.of());
            }

            // 5. 为每个聚合分组生成聚合说明书
            List<CompletableFuture<String>> futures = new ArrayList<>();
            for (AggregationGroupDto group : aggregationGroups) {
                CompletableFuture<String> aggregatedId = generateAggregatedDocumentation(projectId, branchName, group, documentations);
                if (aggregatedId != null) {
                    futures.add(aggregatedId);
                }
            }

            log.info("完成说明书聚合，项目: {}, 分支: {}, 生成聚合说明书数量: {}",
                    projectId, branchName, futures.size());

            CompletableFuture<Void> allFuturesDone = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            return allFuturesDone.thenApply(v ->
                    futures.stream()
                            .map(CompletableFuture::join) // join() 在这里不会阻塞
                            .collect(Collectors.toList())
            );
        } catch (Exception e) {
            log.error("聚合说明书时发生错误，项目: {}, 分支: {}", projectId, branchName, e);
            throw new RuntimeException("聚合说明书失败", e);
        }
    }

    /**
     * 查询指定项目和分支的所有说明书
     */
    private List<Documentation> findDocumentationsByProjectAndBranch(String projectId, String branchName, Integer level) {
        return documentationRepository.findByProjectIdAndBranchName(projectId, branchName, level).stream()
                .filter(doc -> Documentation.DocumentationStatus.COMPLETED.equals(doc.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 构建说明书摘要列表
     */
    private List<DocumentationSummaryDto> buildDocumentationSummaries(List<Documentation> documentations) {
        return documentations.stream()
                .map(doc -> DocumentationSummaryDto.builder()
                        .entryPointId(doc.getEntryPointId())
                        .entryPointName(doc.getEntryPointName())
                        .title(doc.getTitle())
                        .summary(doc.getSummary())
                        .documentationId(doc.getId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 调用AI模型分析文档分组
     */
    private List<AggregationGroupDto> analyzeDocumentationGroups(List<DocumentationSummaryDto> summaries) {
        try {
            // 检查是否需要分批处理
            if (batchAggregationService.needsBatchProcessing(summaries)) {
                log.info("使用分批处理模式分析文档聚合");
                return batchAggregationService.analyzeBatchDocumentationGroups(summaries);
            }

            // 常规单次处理
            log.info("使用常规模式分析文档聚合");
            String summariesJson = Json.toJson(summaries);

            return aggregationAIService.analyzeDocumentationGroups(
                    summariesJson, new TypeToken<List<AggregationGroupDto>>() {
                    }.getType());

        } catch (Exception e) {
            log.error("调用AI模型分析文档分组时发生错误", e);
            return List.of();
        }
    }


    /**
     * 为聚合分组生成聚合说明书
     */
    @Async("codeDocumentTaskExecutor")
    public CompletableFuture<String> generateAggregatedDocumentation(String projectId, String branchName,
                                                                     AggregationGroupDto group, List<Documentation> allDocumentations) {
        try {
            log.info("开始生成聚合说明书，类型: {}", group.getAggregationType());

            // 1. 获取分组中的完整说明书内容
            List<Documentation> groupDocumentations = getGroupDocumentations(group, allDocumentations);
            if (CollectionUtils.isEmpty(groupDocumentations)) {
                log.warn("聚合分组 {} 中没有找到有效的说明书", group.getAggregationType());
                return null;
            }

            // 2. 调用AI模型生成聚合内容
            String aggregatedContent = generateAggregatedContent(group, groupDocumentations);
            if (aggregatedContent == null || aggregatedContent.trim().isEmpty()) {
                log.error("AI模型未能生成有效的聚合内容");
                return null;
            }

            // 3. 解析聚合内容
            AggregatedContentDto content = parseAggregatedContent(aggregatedContent);
            if (content == null) {
                log.error("无法解析聚合内容");
                return null;
            }

            // 4. 保存聚合说明书
            AggregatedDocumentation aggregatedDoc = AggregatedDocumentation.builder()
                    .title(content.getTitle())
                    .summary(content.getSummary())
                    .content(content.getContent())
                    .projectId(projectId)
                    .branchName(branchName)
                    .aggregationType(group.getAggregationType())
                    .status(AggregatedDocumentation.AggregationStatus.COMPLETED)
                    .build();

            aggregatedDoc = aggregatedDocumentationRepository.save(aggregatedDoc);
            if (aggregatedDoc == null || aggregatedDoc.getId() == null) {
                log.error("保存聚合说明书失败");
                return null;
            }

            // 5. 保存聚合关系
            saveAggregationRelations(aggregatedDoc.getId(), groupDocumentations);

            // 6. 生成并保存向量
            saveAggregatedDocumentationVector(aggregatedDoc);

            log.info("成功生成聚合说明书，ID: {}, 类型: {}", aggregatedDoc.getId(), group.getAggregationType());
            return CompletableFuture.completedFuture(aggregatedDoc.getId());

        } catch (Exception e) {
            log.error("生成聚合说明书时发生错误，类型: {}", group.getAggregationType(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 获取分组中的完整说明书
     */
    private List<Documentation> getGroupDocumentations(AggregationGroupDto group, List<Documentation> allDocumentations) {
        List<String> documentationIds = group.getDocumentationSummaries().stream()
                .map(DocumentationSummaryDto::getDocumentationId)
                .toList();

        return allDocumentations.stream()
                .filter(doc -> documentationIds.contains(doc.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 调用AI模型生成聚合内容
     */
    private String generateAggregatedContent(AggregationGroupDto group, List<Documentation> documentations) {
        try {
            String docsContent = buildDocumentationsContent(documentations);

            AggregatedContentDto content = aggregationAIService.generateAggregatedContent(
                    group.getAggregationType(),
                    group.getDescription(),
                    docsContent,
                    AggregatedContentDto.class
            );

            return content != null ? Json.toJson(content) : null;

        } catch (Exception e) {
            log.error("调用AI模型生成聚合内容时发生错误", e);
            return null;
        }
    }

    /**
     * 构建文档内容字符串
     */
    private String buildDocumentationsContent(List<Documentation> documentations) {
        StringBuilder docsContent = new StringBuilder();
        for (int i = 0; i < documentations.size(); i++) {
            Documentation doc = documentations.get(i);
            docsContent.append(String.format("""
                    ## 说明书 %d: %s
                    **文档ID**: %s
                    **入口点**: %s
                    **摘要**: %s
                    **内容**:
                    %s
                    
                    """, i + 1, doc.getTitle(), doc.getId(), doc.getEntryPointName(), doc.getSummary(), doc.getContent()));
        }
        return docsContent.toString();
    }

    /**
     * 解析聚合内容
     */
    private AggregatedContentDto parseAggregatedContent(String content) {
        try {
            return Json.fromJson(content, AggregatedContentDto.class);
        } catch (Exception e) {
            log.error("解析聚合内容时发生错误: {}", content, e);
            return null;
        }
    }

    /**
     * 保存聚合关系
     */
    private void saveAggregationRelations(String aggregatedDocumentationId, List<Documentation> documentations) {
        List<DocumentationAggregation> relations = new ArrayList<>();

        for (int i = 0; i < documentations.size(); i++) {
            Documentation doc = documentations.get(i);
            DocumentationAggregation relation = DocumentationAggregation.builder()
                    .aggregatedDocumentationId(aggregatedDocumentationId)
                    .documentationId(doc.getId())
                    .entryPointId(doc.getEntryPointId())
                    .entryPointName(doc.getEntryPointName())
                    .weight(documentations.size() - i) // 权重递减
                    .build();
            relations.add(relation);
        }

        documentationAggregationRepository.batchSave(relations);
        log.debug("保存了 {} 个聚合关系", relations.size());
    }

    /**
     * 生成并保存聚合说明书向量
     */
    private void saveAggregatedDocumentationVector(AggregatedDocumentation aggregatedDoc) {
        try {
            // 生成向量内容（标题 + 摘要）
            String vectorContent = aggregatedDoc.getTitle() + " " + aggregatedDoc.getSummary();
            float[] vector = vectorGenerator.generateVector(vectorContent);

            // 创建向量实体
            DocumentationVector documentationVector = new DocumentationVector();
            documentationVector.setDocumentId(aggregatedDoc.getId());
            documentationVector.setContent(vectorContent);
            documentationVector.setProjectId(aggregatedDoc.getProjectId());
            documentationVector.setBranchName(aggregatedDoc.getBranchName());
            documentationVector.setTextDense(vector);
            documentationVector.setDocumentType(DocumentType.DOC_TYPE_FLOW);

            // 保存到Milvus
//            vectorClient.batchUpsertDocumentationVectors(List.of(documentationVector));

            log.debug("成功生成聚合说明书向量，ID: {}", aggregatedDoc.getId());

        } catch (Exception e) {
            log.error("生成聚合说明书向量时发生错误，ID: {}", aggregatedDoc.getId(), e);
        }
    }

}
