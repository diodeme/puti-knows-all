package com.puti.code.ai.documentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 说明书摘要DTO
 * 用于AI模型分析和聚合
 *
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationSummaryDto {

    /**
     * 入口点ID
     */
    private String entryPointId;

    /**
     * 入口点名称
     */
    private String entryPointName;

    /**
     * 说明书标题
     */
    private String title;

    /**
     * 说明书摘要
     */
    private String summary;

    /**
     * 说明书ID
     */
    private String documentationId;
}
