package com.puti.code.documentation.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聚合说明书查询响应DTO
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedDocumentationResponse {
    
    /**
     * 说明书ID
     */
    private String id;
    
    /**
     * 说明书内容
     */
    private String content;
}
