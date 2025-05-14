package com.puti.code.ai.support;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.puti.code.base.model.MethodInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Token计算支持类
 * 使用JTokkit库进行精确的OpenAI token计算
 */
@Slf4j
public class TokenSupport {

    private static final EncodingRegistry ENCODING_REGISTRY = Encodings.newDefaultEncodingRegistry();

    /**
     * 计算文本的精确token数量
     *
     * @param text 要计算的文本
     * @param encodingType 编码类型
     * @return token数量
     */
    public static int calculateTokens(String text, EncodingType encodingType) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        try {
            return ENCODING_REGISTRY.getEncoding(encodingType).countTokens(text);
        } catch (Exception e) {
            log.warn("计算token时发生错误，使用字符长度估算: {}", e.getMessage());
            // 降级到字符长度估算 (大约每4个字符1个token)
            return (int) Math.ceil(text.length() / 4.0);
        }
    }


    /**
     * 计算方法信息的token数量
     *
     * @param method 方法信息
     * @return token数量
     */
    public static int calculateTokens(MethodInfo method, EncodingType encodingType) {
        if (method == null) {
            return 0;
        }

        int totalTokens = 0;

        if (method.getSignature() != null) {
            totalTokens += calculateTokens(method.getSignature(), encodingType);
        }
        if (method.getDescription() != null) {
            totalTokens += calculateTokens(method.getDescription(), encodingType);
        }
        if (method.getContent() != null) {
            totalTokens += calculateTokens(method.getContent(), encodingType);
        }

        return totalTokens;
    }

    /**
     * 计算方法列表的总token数量
     *
     * @param methods 方法列表
     * @return 总token数量
     */
    public static int calculateTokens(List<MethodInfo> methods, EncodingType encodingType) {
        if (methods == null || methods.isEmpty()) {
            return 0;
        }

        return methods.stream()
                .mapToInt(e -> calculateTokens(e, encodingType))
                .sum();
    }
}
