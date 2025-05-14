package com.puti.code.base.entity.rdb;

import com.puti.code.base.annotation.AutoKey;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 说明书实体类
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Documentation {
    
    /**
     * 主键ID
     */
    @AutoKey
    private String id;
    
    /**
     * 入口节点ID
     */
    private String entryPointId;
    
    /**
     * 入口节点名称
     */
    private String entryPointName;
    
    /**
     * 说明书标题
     */
    private String title;

    /**
     * 说明书摘要
     */
    private String summary;

    /**
     * 说明书内容（mediumText字段）
     */
    private String content;
    
    /**
     * 生成层级（1-核心流程，2-详细流程，3-完整文档）
     */
    private Integer level;

    /**
     * 生成状态
     */
    private DocumentationStatus status;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 是否为最终版本（true表示完整文档，false表示中间态）
     */
    private Boolean isFinalVersion;

    /**
     * 父文档ID（用于关联中间态到最终态）
     */
    private String parentDocumentationId;
    
    /**
     * 项目ID
     */
    private String projectId;
    
    /**
     * 分支名称
     */
    private String branchName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 说明书生成状态枚举
     */
    @Getter
    public enum DocumentationStatus {
        PENDING("待生成"),
        GENERATING("生成中"),
        COMPLETED("已完成"),
        FAILED("生成失败");
        
        private final String description;
        
        DocumentationStatus(String description) {
            this.description = description;
        }

    }
}
