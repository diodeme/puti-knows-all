package com.puti.code.base.entity.rdb;

import com.puti.code.base.annotation.AutoKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 说明书归档实体类
 * 用于存储已完成的中间态文档
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationArchive {
    
    /**
     * 主键ID
     */
    @AutoKey
    private String id;

    /**
     * 原始文档ID
     */
    private String originalDocumentationId;
    
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
     * 说明书内容（mediumText字段）
     */
    private String content;
    
    /**
     * 生成层级
     */
    private Integer level;
    
    /**
     * 版本号
     */
    private Integer version;
    
    /**
     * 项目ID
     */
    private String projectId;
    
    /**
     * 分支名称
     */
    private String branchName;
    
    /**
     * 原始创建时间
     */
    private LocalDateTime originalCreatedAt;
    
    /**
     * 原始更新时间
     */
    private LocalDateTime originalUpdatedAt;
    
    /**
     * 归档时间
     */
    private LocalDateTime archivedAt;
    
    /**
     * 归档原因
     */
    private ArchiveReason archiveReason;
    
    /**
     * 最终版本文档ID（关联到完整文档）
     */
    private String finalDocumentationId;
    
    /**
     * 归档原因枚举
     */
    public enum ArchiveReason {
        FINAL_VERSION_COMPLETED("最终版本已完成"),
        VERSION_SUPERSEDED("版本被替代"),
        MANUAL_ARCHIVE("手动归档"),
        CLEANUP("清理归档");
        
        private final String description;
        
        ArchiveReason(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
