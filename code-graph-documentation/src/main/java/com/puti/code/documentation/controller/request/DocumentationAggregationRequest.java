package com.puti.code.documentation.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 说明书聚合请求DTO
 * 用于封装聚合说明书的请求参数
 *
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationAggregationRequest {

    /**
     * 项目ID（必填）
     * 标识要聚合说明书的项目
     */
    @NotBlank(message = "项目ID不能为空")
    private String projectId;

    /**
     * 分支名称（必填）
     * 标识要聚合的代码分支
     */
    @NotBlank(message = "分支名称不能为空")
    private String branchName;

    /**
     * 是否强制重新聚合（可选，默认false）
     * true: 即使已存在聚合说明书也重新聚合
     * false: 如果已存在聚合说明书则跳过
     */
    @Builder.Default
    private Boolean forceRegenerate = false;

    /**
     * 聚合描述（可选）
     * 用于记录本次聚合的目的或备注信息
     */
    private String description;
}
