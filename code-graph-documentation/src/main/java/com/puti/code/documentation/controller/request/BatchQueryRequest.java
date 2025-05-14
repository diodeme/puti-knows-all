package com.puti.code.documentation.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量查询请求DTO
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchQueryRequest {
    
    /**
     * 说明书ID列表
     */
    @NotEmpty(message = "说明书ID列表不能为空")
    @Size(max = 100, message = "单次查询最多支持100个ID")
    private List<String> documentIds;
}
