package com.puti.code.base.entity.rdb;

import com.puti.code.base.annotation.AutoKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 说明书方法信息归档实体类
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationMethodArchive {
    
    /**
     * 主键ID
     */
    @AutoKey
    private String id;

    /**
     * 原始方法记录ID
     */
    private String originalMethodId;

    /**
     * 归档文档ID（外键关联DocumentationArchive）
     */
    private String archivedDocumentationId;
    
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
    private DocumentationMethod.MethodType methodType;
    
    /**
     * 调用层级
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
     * 原始创建时间
     */
    private LocalDateTime originalCreatedAt;
    
    /**
     * 归档时间
     */
    private LocalDateTime archivedAt;
}
