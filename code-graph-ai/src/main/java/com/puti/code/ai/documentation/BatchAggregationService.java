package com.puti.code.ai.documentation;

import com.google.common.reflect.TypeToken;
import com.puti.code.ai.documentation.dto.AggregationGroupDto;
import com.puti.code.ai.documentation.dto.DocumentationSummaryDto;
import com.puti.code.base.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分批聚合服务
 * 负责处理大量文档的分批聚合分析
 *
 * @author diodehe
 */
@Slf4j
@Service
public class BatchAggregationService {

    @Autowired
    private AIDocumentationAggregationService aggregationAIService;

    // 分批处理配置
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * 检查是否需要分批处理
     *
     * @param summaries 说明书摘要列表
     * @return 是否需要分批处理
     */
    public boolean needsBatchProcessing(List<DocumentationSummaryDto> summaries) {
        boolean needsBatch = summaries.size() > BATCH_SIZE;
        if (needsBatch) {
            log.info("说明书数量过多({} > {})，需要分批处理", summaries.size(), BATCH_SIZE);
        }
        return needsBatch;
    }

    /**
     * 分批分析文档聚合
     *
     * @param summaries 说明书摘要列表
     * @return 聚合分组列表
     */
    public List<AggregationGroupDto> analyzeBatchDocumentationGroups(List<DocumentationSummaryDto> summaries) {
        try {
            log.info("开始分批分析文档聚合，总数量: {}", summaries.size());

            // 1. 按entry_point_name排序（名字相似的关联性更强）
            List<DocumentationSummaryDto> sortedSummaries = summaries.stream()
                    .sorted(Comparator.comparing(DocumentationSummaryDto::getEntryPointName))
                    .collect(Collectors.toList());

            // 2. 分批处理
            List<List<DocumentationSummaryDto>> batches = splitIntoBatches(sortedSummaries, BATCH_SIZE);
            log.info("将文档分为 {} 批处理", batches.size());

            // 3. 逐批进行聚合分析
            List<AggregationGroupDto> allGroups = new ArrayList<>();

            for (int i = 0; i < batches.size(); i++) {
                log.info("开始处理第 {} 批 (共 {} 批)", i + 1, batches.size());

                List<AggregationGroupDto> batchGroups = analyzeBatch(
                        batches.get(i),
                        allGroups,
                        i + 1,
                        batches.size()
                );

                if (batchGroups != null && !batchGroups.isEmpty()) {
                    // 处理分组合并逻辑
                    processBatchGroups(batchGroups, allGroups);
                    log.info("第 {} 批处理完成，当前总分组数: {}", i + 1, allGroups.size());
                } else {
                    log.warn("第 {} 批处理未返回有效结果", i + 1);
                }
            }

            log.info("分批聚合分析完成，总共生成 {} 个聚合分组", allGroups.size());
            return allGroups;

        } catch (Exception e) {
            log.error("分批分析文档聚合时发生错误", e);
            return List.of();
        }
    }

    /**
     * 分析单个批次
     *
     * @param batchSummaries 当前批次的摘要
     * @param previousGroups 之前的聚合结果
     * @param batchIndex 当前批次索引
     * @param totalBatches 总批次数
     * @return 当前批次的聚合分组
     */
    private List<AggregationGroupDto> analyzeBatch(List<DocumentationSummaryDto> batchSummaries,
                                                   List<AggregationGroupDto> previousGroups,
                                                   int batchIndex,
                                                   int totalBatches) {
        try {
            // 准备数据
            String summariesJson = Json.toJson(batchSummaries);

            // 构建之前聚合结果的摘要（包含分组ID，供AI引用）
            String previousGroupsJson = buildPreviousGroupsJson(previousGroups);

            // 调用AI服务进行分批分析
            Type typeReference = new TypeToken<List<AggregationGroupDto>>() {}.getType();
            return aggregationAIService.analyzeBatchDocumentationGroups(
                    summariesJson,
                    previousGroupsJson,
                    batchIndex,
                    totalBatches,
                    typeReference
            );

        } catch (Exception e) {
            log.error("分析批次时发生错误，批次: {}", batchIndex, e);
            return List.of();
        }
    }



    /**
     * 处理批次分组结果，包括合并到历史分组的逻辑
     */
    private void processBatchGroups(List<AggregationGroupDto> batchGroups, List<AggregationGroupDto> allGroups) {
        for (AggregationGroupDto batchGroup : batchGroups) {
            if (StringUtils.hasText(batchGroup.getHistoricalGroupId())) {
                // 需要合并到历史分组
                mergeToHistoricalGroup(batchGroup, allGroups);
            } else {
                // 新分组，分配ID并添加到总列表
                batchGroup.setGroupId(generateGroupId());
                allGroups.add(batchGroup);
                log.info("创建新聚合分组: {} (ID: {})", batchGroup.getAggregationType(), batchGroup.getGroupId());
            }
        }
    }

    /**
     * 合并到历史分组
     */
    private void mergeToHistoricalGroup(AggregationGroupDto batchGroup, List<AggregationGroupDto> allGroups) {
        String historicalGroupId = batchGroup.getHistoricalGroupId();

        // 查找对应的历史分组
        AggregationGroupDto historicalGroup = allGroups.stream()
                .filter(group -> historicalGroupId.equals(group.getGroupId()))
                .findFirst()
                .orElse(null);

        if (historicalGroup != null) {
            // 合并说明书列表
            List<DocumentationSummaryDto> mergedSummaries = new ArrayList<>(historicalGroup.getDocumentationSummaries());
            mergedSummaries.addAll(batchGroup.getDocumentationSummaries());
            historicalGroup.setDocumentationSummaries(mergedSummaries);

            // 更新描述（如果有新的描述信息）
            if (StringUtils.hasText(batchGroup.getDescription()) &&
                !batchGroup.getDescription().equals(historicalGroup.getDescription())) {
                historicalGroup.setDescription(historicalGroup.getDescription() + "；" + batchGroup.getDescription());
            }

            log.info("合并到历史分组: {} (ID: {}), 新增 {} 个说明书",
                    historicalGroup.getAggregationType(),
                    historicalGroupId,
                    batchGroup.getDocumentationSummaries().size());
        } else {
            log.warn("未找到历史分组ID: {}，将作为新分组处理", historicalGroupId);
            batchGroup.setGroupId(generateGroupId());
            batchGroup.setHistoricalGroupId(null); // 清除无效的历史分组ID
            allGroups.add(batchGroup);
        }
    }

    /**
     * 构建之前聚合结果的JSON（包含分组ID供AI引用）
     */
    private String buildPreviousGroupsJson(List<AggregationGroupDto> previousGroups) {
        if (previousGroups == null || previousGroups.isEmpty()) {
            return "[]";
        }

        List<Map<String, Object>> previousGroupsSummary = previousGroups.stream()
                .map(group -> {
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("groupId", group.getGroupId());
                    summary.put("aggregationType", group.getAggregationType());
                    summary.put("description", group.getDescription());
                    summary.put("relevanceScore", group.getRelevanceScore());
                    summary.put("documentCount", group.getDocumentationSummaries().size());
                    return summary;
                })
                .collect(Collectors.toList());

        return Json.toJson(previousGroupsSummary);
    }

    /**
     * 生成分组ID
     */
    private String generateGroupId() {
        return "group_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    /**
     * 将列表分批
     */
    private <T> List<List<T>> splitIntoBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            batches.add(new ArrayList<>(list.subList(i, end)));
        }
        return batches;
    }
}
