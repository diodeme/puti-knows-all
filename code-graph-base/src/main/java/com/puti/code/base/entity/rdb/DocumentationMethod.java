package com.puti.code.base.entity.rdb;

import com.puti.code.base.annotation.AutoKey;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 说明书涉及方法信息实体类
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationMethod {
    
    /**
     * 主键ID
     */
    @AutoKey
    private String id;

    /**
     * 说明书ID（外键）
     */
    private String documentationId;
    
    /**
     * 方法节点ID
     */
    private String methodId;
    
    /**
     * 方法全限定名
     */
    private String methodName;
    
    /**
     * 方法类型
     */
    private MethodType methodType;
    
    /**
     * 调用层级（从入口点开始的距离）
     */
    private Integer callLevel;
    
    /**
     * 方法描述
     */
    private String description;
    
    /**
     * 方法签名
     */
    private String signature;
    
    /**
     * 所属类名
     */
    private String className;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 方法类型枚举
     */
    @Getter
    public enum MethodType {
        ENTRY_POINT("入口方法"),
        DIRECT_CALL("直接调用"),
        INDIRECT_CALL("间接调用"),
        UTILITY_METHOD("工具方法"),
        EXCEPTION_HANDLER("异常处理");
        
        private final String description;
        
        MethodType(String description) {
            this.description = description;
        }

    }
}
