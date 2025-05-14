package com.puti.code.documentation.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目查询请求DTO
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectQueryRequest {
    
    /**
     * 项目ID
     */
    @NotBlank(message = "项目ID不能为空")
    private String projectId;
}
