package com.puti.code.documentation.dto;

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
public class DocumentationGenerationDto {
    private String projectId;
    private String branchName;
    private Integer level;
    private String entryPointId;
    @Builder.Default
    private Boolean forceRegenerate = false;
    private String description;
}
