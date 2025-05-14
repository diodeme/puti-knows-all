package com.puti.code.documentation.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 方法内容响应DTO
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodContent {
    
    /**
     * 方法ID
     */
    private String methodId;
    
    /**
     * 方法全限定名
     */
    private String fullName;
    
    /**
     * 方法名
     */
    private String methodName;
    
    /**
     * 方法内容（解压后的源代码）
     */
    private String content;

    /**
     * 是否为入口点
     */
    private Boolean isEntryPoint;
}
