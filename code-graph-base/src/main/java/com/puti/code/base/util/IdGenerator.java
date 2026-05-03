package com.puti.code.base.util;

import com.puti.code.base.config.AppConfig;
import com.puti.code.base.enums.ParseType;
import lombok.Builder;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Map;

/**
 * ID生成器，将全限定名转换为32位定长字符串
 */
public class IdGenerator {

    public static IdGeneratorContext.IdGeneratorContextBuilder builder() {
        return IdGeneratorContext.builder();
    }
    
    /**
     * 生成32位定长ID
     *
     * @param context id生成上下文
     * @return 32位定长ID
     */
    public static String generate(IdGeneratorContext context) {
        AppConfig appConfig = AppConfig.getInstance();
        String projectId = appConfig.getProjectId();
        String branch = appConfig.getBranch();

        if (ParseType.LIBRARY.equals(appConfig.getParseType())) {
            // 库模式：查 classIndex 获取目标 GAV，生成版本化节点 ID
            String gav = lookupGav(appConfig, context.fullQualifiedName);
            if (gav != null) {
                return DigestUtils.md5Hex(context.fullQualifiedName + "#lib:" + gav);
            }
            return DigestUtils.md5Hex(context.fullQualifiedName);
        }

        // 项目模式：始终先查 classIndex
        // 无论 isShadow 值如何都必须查 classIndex，因为 on-classpath 的库类
        // （如 org.HdrHistogram.AbstractHistogram）isShadow=false 但属于依赖库，
        // 若不查 classIndex 会生成项目风格 ID，导致边指向的 ID 与 LibraryHandler
        // 创建的节点 ID 不匹配，图谱断裂
        String gav = lookupGav(appConfig, context.fullQualifiedName);
        if (gav != null) {
            return DigestUtils.md5Hex(context.fullQualifiedName + "#lib:" + gav);
        }

        // 未命中 classIndex：项目内部代码用项目风格 ID，shadow 引用降级为裸 FQN
        if (context.isShadow) {
            return DigestUtils.md5Hex(context.fullQualifiedName);
        }
        return DigestUtils.md5Hex(context.fullQualifiedName + "#" + projectId + "#" + branch);
    }

    /**
     * 从 classIndex 中查找 FQN 对应的 GAV。
     * 提取 FQN 中的类名部分（# 之前），查 className → GAV 映射。
     * 跳过非全限定名（不含 '.'），避免泛型类型变量（T, E, K, V）
     * 被误匹配到 classIndex 中的同名条目。
     */
    private static String lookupGav(AppConfig appConfig, String fullQualifiedName) {
        Map<String, String> classIndex = appConfig.getClassToGavIndex();
        if (classIndex == null || classIndex.isEmpty()) return null;
        String className = extractClassName(fullQualifiedName);
        if (!className.contains(".")) return null;
        return classIndex.get(className);
    }

    /**
     * 从 FQN 中提取类名。
     * 方法签名：com.example.Service#doWork(java.lang.String) → com.example.Service
     * 类名：com.example.Service → com.example.Service
     * 字段：com.example.Service#fieldName → com.example.Service
     */
    private static String extractClassName(String fullQualifiedName) {
        int hashIndex = fullQualifiedName.indexOf('#');
        return hashIndex >= 0 ? fullQualifiedName.substring(0, hashIndex) : fullQualifiedName;
    }

    @Builder
    public static class IdGeneratorContext {
        private String fullQualifiedName;
        private boolean isShadow;
    }
}
