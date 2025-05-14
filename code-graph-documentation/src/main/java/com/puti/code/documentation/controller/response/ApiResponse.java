package com.puti.code.documentation.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应包装类
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    /**
     * 请求是否成功
     */
    private boolean success;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 创建成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("操作成功")
                .data(data)
                .build();
    }
    
    /**
     * 创建成功响应（带自定义消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * 创建失败响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
    
    /**
     * 创建失败响应（带数据）
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .build();
    }
}
