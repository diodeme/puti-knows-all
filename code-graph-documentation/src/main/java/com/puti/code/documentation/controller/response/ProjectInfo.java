package com.puti.code.documentation.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目信息响应DTO
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInfo {
    
    /**
     * 项目ID
     */
    private String projectId;
    
    /**
     * 项目名称（可选，如果有的话）
     */
    private String projectName;
    
    /**
     * 聚合说明书数量
     */
    private Integer documentationCount;
}
