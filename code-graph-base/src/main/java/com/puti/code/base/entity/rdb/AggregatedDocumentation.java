package com.puti.code.base.entity.rdb;

import com.puti.code.base.annotation.AutoKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聚合说明书实体类
 * 用于存储多个流程说明书聚合后的结果
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedDocumentation {
    
    /**
     * 主键ID
     */
    @AutoKey
    private String id;
    
    /**
     * 聚合标题
     */
    private String title;
    
    /**
     * 聚合摘要
     */
    private String summary;
    
    /**
     * 聚合内容
     */
    private String content;
    
    /**
     * 项目ID
     */
    private String projectId;
    
    /**
     * 分支名称
     */
    private String branchName;
    
    /**
     * 聚合类型（如：订单流程、用户管理流程等）
     */
    private String aggregationType;
    
    /**
     * 聚合状态
     */
    private AggregationStatus status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 聚合状态枚举
     */
    public enum AggregationStatus {
        PENDING("待聚合"),
        AGGREGATING("聚合中"),
        COMPLETED("已完成"),
        FAILED("聚合失败");
        
        private final String description;
        
        AggregationStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
