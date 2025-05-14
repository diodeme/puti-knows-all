package com.puti.code.ai.documentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聚合内容DTO
 * 表示AI生成的聚合说明书内容
 *
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedContentDto {

    /**
     * 聚合说明书标题
     */
    private String title;

    /**
     * 聚合说明书摘要
     */
    private String summary;

    /**
     * 聚合说明书详细内容（Markdown格式）
     */
    private String content;
}
