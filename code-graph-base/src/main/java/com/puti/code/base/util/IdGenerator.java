package com.puti.code.base.util;

import com.puti.code.base.config.AppConfig;
import com.puti.code.base.enums.ParseType;
import lombok.Builder;
import org.apache.commons.codec.digest.DigestUtils;

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
        //1.如果是library，则不关注仓库名和分支
        //2.如果是生成外部引用的id，则也不关注仓库名和分支
        //3.如果是生成内部的id，则需要关注仓库名和分支
        // 使用MD5生成32位定长字符串
        AppConfig appConfig = AppConfig.getInstance();
        String projectId = appConfig.getProjectId();
        String branch = appConfig.getBranch();
        String idStr = context.isShadow || ParseType.LIBRARY.equals(appConfig.getParseType()) ?
            context.fullQualifiedName : context.fullQualifiedName + "#" + projectId + "#" + branch;
        return DigestUtils.md5Hex(idStr);
    }

    @Builder
    public static class IdGeneratorContext {
        private String fullQualifiedName;
        private boolean isShadow;
    }
}
