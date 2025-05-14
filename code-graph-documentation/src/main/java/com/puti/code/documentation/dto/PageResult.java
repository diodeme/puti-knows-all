package com.puti.code.documentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果DTO
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    
    /**
     * 数据列表
     */
    private List<T> data;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 是否有下一页
     */
    private Boolean hasNext;
    
    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;
    
    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> data, PageRequest pageRequest, Long total) {
        int totalPages = (int) Math.ceil((double) total / pageRequest.getSize());
        
        return PageResult.<T>builder()
                .data(data)
                .page(pageRequest.getPage())
                .size(pageRequest.getSize())
                .total(total)
                .totalPages(totalPages)
                .hasNext(pageRequest.getPage() < totalPages)
                .hasPrevious(pageRequest.getPage() > 1)
                .build();
    }
    
    /**
     * 创建空的分页结果
     */
    public static <T> PageResult<T> empty(PageRequest pageRequest) {
        return PageResult.<T>builder()
                .data(List.of())
                .page(pageRequest.getPage())
                .size(pageRequest.getSize())
                .total(0L)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}
