package com.puti.code.ai.documentation;

import com.puti.code.base.entity.rdb.Documentation;
import com.puti.code.base.model.MethodInfo;
import com.puti.code.base.model.SubgraphData;
import lombok.*;

import java.util.List;

/**
 * 文档生成上下文
 * 包含生成文档所需的所有上下文信息
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationGenerationContext {
    
    /**
     * 入口点ID
     */
    private String entryPointId;
    
    /**
     * 当前生成层级
     */
    private int level;
    
    /**
     * 子图数据
     */
    private SubgraphData subgraph;
    
    /**
     * 前一层级的文档（用于增量生成）
     */
    private Documentation previousDocumentation;
    
    /**
     * 最大内容长度限制
     */
    private int maxLength;
    
    /**
     * 项目ID
     */
    private String projectId;
    
    /**
     * 分支名称
     */
    private String branchName;
    
    /**
     * 生成配置参数
     */
    private GenerationConfig config;
    
    /**
     * 生成配置类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationConfig {
        
        /**
         * 是否包含代码示例
         */
        @Builder.Default
        private boolean includeCodeExamples = true;
        
        /**
         * 是否包含性能分析
         */
        @Builder.Default
        private boolean includePerformanceAnalysis = true;
        
        /**
         * 是否包含异常处理说明
         */
        @Builder.Default
        private boolean includeExceptionHandling = true;
        
        /**
         * 是否包含依赖关系分析
         */
        @Builder.Default
        private boolean includeDependencyAnalysis = true;
        
        /**
         * 文档风格（TECHNICAL, BUSINESS, MIXED）
         */
        @Builder.Default
        private DocumentationStyle style = DocumentationStyle.MIXED;
        
        /**
         * 详细程度（BRIEF, NORMAL, DETAILED）
         */
        @Builder.Default
        private DetailLevel detailLevel = DetailLevel.NORMAL;
        
        /**
         * 目标读者（DEVELOPER, ARCHITECT, BUSINESS）
         */
        @Builder.Default
        private TargetAudience targetAudience = TargetAudience.DEVELOPER;
    }
    
    /**
     * 文档风格枚举
     */
    @Getter
    public enum DocumentationStyle {
        TECHNICAL("技术导向"),
        BUSINESS("业务导向"),
        MIXED("技术业务混合");

        private final String description;

        DocumentationStyle(String description) {
            this.description = description;
        }

    }

    /**
     * 详细程度枚举
     */
    @Getter
    public enum DetailLevel {
        BRIEF("简要"),
        NORMAL("正常"),
        DETAILED("详细");

        private final String description;

        DetailLevel(String description) {
            this.description = description;
        }

    }

    /**
     * 目标读者枚举
     */
    @Getter
    public enum TargetAudience {
        DEVELOPER("开发人员"),
        ARCHITECT("架构师"),
        BUSINESS("业务人员");

        private final String description;

        TargetAudience(String description) {
            this.description = description;
        }

    }

    public List<MethodInfo> getMethodsAtLevel() {
        return switch (level) {
            case 1 -> subgraph.getMethodsAtLevel(1);
            case 2 -> subgraph.getMethodsAtLevel(2);
            default -> subgraph.getAllMethods();
        };
    }

    public int getMethodCountForLevel() {
        if (subgraph == null) {
            return 0;
        }

        return switch (level) {
            case 1 -> subgraph.getMethodsAtLevel(1).size();
            case 2 -> subgraph.getMethodsAtLevel(2).size();
            default -> subgraph.getAllMethods().size();
        };
    }

}
