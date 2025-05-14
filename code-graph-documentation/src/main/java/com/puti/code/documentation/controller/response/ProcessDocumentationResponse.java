package com.puti.code.documentation.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流程说明书查询响应DTO
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDocumentationResponse {
    
    /**
     * 说明书ID
     */
    private String id;
    
    /**
     * 说明书内容
     */
    private String content;
    
    /**
     * 入口点ID
     */
    private String entryPointId;
    
    /**
     * 入口点名称
     */
    private String entryPointName;
}
