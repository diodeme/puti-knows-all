package com.puti.code.ai.documentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI文档生成响应DTO
 * 用于解析AI返回的结构化JSON响应
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIDocumentationResponse {
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 文档摘要
     */
    private String summary;
    
    /**
     * 文档内容
     */
    private String content;
    
    /**
     * 验证响应是否有效
     * 
     * @return 如果title、summary、content都不为空则返回true
     */
    public boolean isValid() {
        return title != null && !title.trim().isEmpty() &&
               summary != null && !summary.trim().isEmpty() &&
               content != null && !content.trim().isEmpty();
    }
    
    /**
     * 获取清理后的标题（去除多余空格和换行）
     */
    public String getCleanTitle() {
        return title != null ? title.trim().replaceAll("\\s+", " ") : null;
    }
    
    /**
     * 获取清理后的摘要（去除多余空格和换行）
     */
    public String getCleanSummary() {
        return summary != null ? summary.trim().replaceAll("\\s+", " ") : null;
    }
    
    /**
     * 获取清理后的内容（保留换行但去除多余空行）
     */
    public String getCleanContent() {
        return content != null ? content.trim().replaceAll("\n{3,}", "\n\n") : null;
    }
}
