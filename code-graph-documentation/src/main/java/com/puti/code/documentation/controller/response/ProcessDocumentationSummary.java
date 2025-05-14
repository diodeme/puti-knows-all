package com.puti.code.documentation.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程说明书摘要响应DTO
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDocumentationSummary {
    
    /**
     * 流程说明书ID
     */
    private String id;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 摘要
     */
    private String summary;
    
    /**
     * 入口点ID
     */
    private String entryPointId;
    
    /**
     * 入口点名称
     */
    private String entryPointName;
    
    /**
     * 项目ID
     */
    private String projectId;
    
    /**
     * 分支名称
     */
    private String branchName;
}
