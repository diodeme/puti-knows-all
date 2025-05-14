package com.puti.code.base.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 方法信息DTO
 * 用于在系统中传递方法的基本信息
 * 
 * @author diodehe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodInfo {
    
    /**
     * 方法节点ID（在图数据库中的唯一标识）
     */
    private String methodId;
    
    /**
     * 方法全限定名（包名.类名.方法名）
     */
    private String fullName;
    
    /**
     * 方法名称
     */
    private String methodName;
    
    /**
     * 所属类名
     */
    private String className;
    
    /**
     * 包名
     */
    private String packageName;
    
    /**
     * 方法签名（包含参数类型）
     */
    private String signature;
    
    /**
     * 方法描述
     */
    private String description;
    
    /**
     * 方法可见性（public, private, protected, package）
     */
    private String visibility;
    
    /**
     * 是否为静态方法
     */
    private Boolean isStatic;
    
    /**
     * 是否为抽象方法
     */
    private Boolean isAbstract;
    
    /**
     * 是否为final方法
     */
    private Boolean isFinal;
    
    /**
     * 返回类型
     */
    private String returnType;
    
    /**
     * 参数类型列表（逗号分隔）
     */
    private String parameterTypes;
    
    /**
     * 异常类型列表（逗号分隔）
     */
    private String exceptionTypes;
    
    /**
     * 方法在调用链中的层级
     */
    private Integer level;
    
    /**
     * 调用次数（在当前分析中）
     */
    private Integer callCount;
    
    /**
     * 是否为入口点
     */
    private Boolean isEntryPoint;
    
    /**
     * 方法复杂度评分
     */
    private Integer complexityScore;
    
    /**
     * 源文件路径
     */
    private String sourceFile;
    
    /**
     * 起始行号
     */
    private Integer startLine;
    
    /**
     * 结束行号
     */
    private Integer endLine;

    /**
     * 函数代码
     */
    private String content;
    
    /**
     * 获取方法的简短描述
     */
    public String getShortDescription() {
        if (description != null && description.length() > 100) {
            return description.substring(0, 97) + "...";
        }
        return description;
    }
    
    /**
     * 检查是否为公共方法
     */
    public boolean isPublic() {
        return "public".equals(visibility);
    }
    
    /**
     * 检查是否为私有方法
     */
    public boolean isPrivate() {
        return "private".equals(visibility);
    }
    
    /**
     * 检查是否为受保护方法
     */
    public boolean isProtected() {
        return "protected".equals(visibility);
    }
    
    /**
     * 获取方法修饰符描述
     */
    public String getModifiersDescription() {
        StringBuilder sb = new StringBuilder();
        
        if (visibility != null) {
            sb.append(visibility).append(" ");
        }
        
        if (Boolean.TRUE.equals(isStatic)) {
            sb.append("static ");
        }
        
        if (Boolean.TRUE.equals(isAbstract)) {
            sb.append("abstract ");
        }
        
        if (Boolean.TRUE.equals(isFinal)) {
            sb.append("final ");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 获取完整的方法签名描述
     */
    public String getFullSignature() {
        StringBuilder sb = new StringBuilder();
        
        // 修饰符
        String modifiers = getModifiersDescription();
        if (!modifiers.isEmpty()) {
            sb.append(modifiers).append(" ");
        }
        
        // 返回类型
        if (returnType != null) {
            sb.append(returnType).append(" ");
        }
        
        // 方法名
        if (methodName != null) {
            sb.append(methodName);
        }
        
        // 参数
        sb.append("(");
        if (parameterTypes != null && !parameterTypes.isEmpty()) {
            sb.append(parameterTypes);
        }
        sb.append(")");
        
        // 异常
        if (exceptionTypes != null && !exceptionTypes.isEmpty()) {
            sb.append(" throws ").append(exceptionTypes);
        }
        
        return sb.toString();
    }
    
    /**
     * 检查方法信息是否完整
     */
    public boolean isComplete() {
        return methodId != null && 
               methodName != null && 
               className != null;
    }
}
