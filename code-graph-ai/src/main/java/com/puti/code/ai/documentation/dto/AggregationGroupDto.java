package com.puti.code.ai.documentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聚合分组DTO
 * 表示AI模型分析后的聚合结果
 *
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationGroupDto {

    /**
     * 分组ID（用于分批处理时的分组标识）
     */
    private String groupId;

    /**
     * 历史分组ID（如果当前批次要合并到历史分组，则返回历史分组的ID）
     */
    private String historicalGroupId;

    /**
     * 聚合类型（如：订单流程、用户管理流程等）
     */
    private String aggregationType;

    /**
     * 聚合描述
     */
    private String description;

    /**
     * 包含的说明书摘要列表
     */
    private List<DocumentationSummaryDto> documentationSummaries;

    /**
     * 相关性评分（0-100）
     */
    private Integer relevanceScore;
}
