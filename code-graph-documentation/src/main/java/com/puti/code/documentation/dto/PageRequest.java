package com.puti.code.documentation.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页请求DTO
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    
    /**
     * 页码，从1开始
     */
    @Min(value = 1, message = "页码必须大于0")
    @Builder.Default
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于0")
    @Builder.Default
    private Integer size = 10;
    
    /**
     * 获取偏移量（用于SQL OFFSET）
     */
    public int getOffset() {
        return (page - 1) * size;
    }
    
    /**
     * 获取限制数量（用于SQL LIMIT）
     */
    public int getLimit() {
        return size;
    }
}
