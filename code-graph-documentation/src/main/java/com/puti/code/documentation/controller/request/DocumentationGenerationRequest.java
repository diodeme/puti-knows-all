package com.puti.code.documentation.controller.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 说明书生成请求DTO
 * 用于封装创建说明书的所有请求参数
 *
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationGenerationRequest {

    /**
     * 项目ID（必填）
     * 标识要生成说明书的项目
     */
    @NotBlank(message = "项目ID不能为空")
    private String projectId;

    /**
     * 分支名称（必填）
     * 标识要分析的代码分支
     */
    @NotBlank(message = "分支名称不能为空")
    private String branchName;

    /**
     * 生成层级（可选，默认为3）
     * 1: 核心流程说明书（1000字以内）
     * 2: 详细流程说明书（3000字以内）
     * 3: 完整技术文档（包含所有实现细节）
     */
    @Min(value = 1, message = "生成层级最小值为1")
    @Max(value = 3, message = "生成层级最大值为3")
    @Builder.Default
    private Integer level = 3;

    /**
     * 入口点ID（可选）
     * 如果为空，则为所有入口点生成说明书
     * 如果有值，则只为指定入口点生成说明书
     */
    private String entryPointId;

    /**
     * 是否强制重新生成（可选，默认false）
     * true: 即使已存在说明书也重新生成
     * false: 如果已存在相同层级的说明书则跳过
     */
    @Builder.Default
    private Boolean forceRegenerate = false;

    /**
     * 生成描述（可选）
     * 用于记录本次生成的目的或备注信息
     */
    private String description;

    /**
     * 判断是否为全量生成（所有入口点）
     *
     * @return true表示为所有入口点生成，false表示为指定入口点生成
     */
    public boolean isGenerateForAll() {
        return entryPointId == null || entryPointId.trim().isEmpty();
    }
}
