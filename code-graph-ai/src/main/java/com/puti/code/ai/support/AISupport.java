package com.puti.code.ai.support;

import com.puti.code.base.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.lang.reflect.Type;
import java.util.stream.Collectors;

@Slf4j
public class AISupport {

    // 重试配置
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 10000;

    /**
     * 带重试机制的AI服务调用（支持Prompt对象）
     */
    public static String callAIServiceWithRetry(ChatClient chatClient, Prompt prompt) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                log.debug("第 {} 次尝试调用AI服务", attempt);

                String result = callAIService(chatClient, prompt);

                if (result != null && !result.trim().isEmpty()) {
                    log.debug("AI服务调用成功，第 {} 次尝试", attempt);
                    return result;
                }

                log.warn("AI服务返回空内容，第 {} 次尝试", attempt);

            } catch (Exception e) {
                lastException = e;
                log.warn("AI服务调用失败，第 {} 次尝试: {}", attempt, e.getMessage());

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("AI服务调用失败，已重试 {} 次", MAX_RETRY_ATTEMPTS, lastException);
        return null;
    }

    public static String callAIService(ChatClient chatClient, Prompt prompt) {
        try {
            log.debug("调用 Spring AI ChatClient，内部使用 stream 模式");

            // 1. 使用 .stream().content() 获取一个包含内容块的 Flux<String>
            Flux<String> contentFlux = chatClient.prompt(prompt)
                    .advisors(new SimpleLoggerAdvisor())
                    .stream()
                    .content();

            // 2. 使用 .collect(Collectors.joining()) 将所有字符串块拼接成一个 Mono<String>
            // 3. 使用 .block() 订阅并阻塞，直到流完成并返回最终的完整字符串
            String response = contentFlux
                    .collect(Collectors.joining())
                    .block();

            log.debug("AI服务流式响应接收完成，总内容长度: {}", response != null ? response.length() : 0);
            return response;

        } catch (Exception e) {
            log.error("Spring AI ChatClient 调用失败", e);
            // 向上层抛出异常，以便重试逻辑可以捕获它
            throw new RuntimeException("AI服务调用失败", e);
        }
    }

    public static <T> T parseAIResponse(String response, Class<T> responseClass) {
        try {
            String jsonPart = extractJsonFromResponse(response);
            return Json.fromJson(jsonPart, responseClass);
        } catch (Exception e) {
            log.error("解析AI响应时发生错误: {}", response, e);
            return null;
        }
    }

    public static <T> T parseAIResponse(String response, Type type) {
        try {
            String jsonPart = extractJsonFromResponse(response);
            return Json.fromJson(jsonPart, type);
        } catch (Exception e) {
            log.error("解析AI响应时发生错误: {}", response, e);
            return null;
        }
    }

    /**
     * 从AI响应中提取JSON内容
     */
    public static String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }

        final String START_TAG = "```json";
        final String END_TAG = "```";
        int startTagIndex = response.indexOf(START_TAG);
        if (startTagIndex != -1) {
            // 寻找最后一个结束标记
            int endTagIndex = response.lastIndexOf(END_TAG);
            // 确保找到的结束标记在开始标记之后
            if (endTagIndex > startTagIndex) {
                // 提取标记之间的内容
                int contentStartIndex = startTagIndex + START_TAG.length();
                return response.substring(contentStartIndex, endTagIndex).trim();
            }
        }

        // 策略二：【保持不变】如果找不到代码块，则查找第一个独立的JSON对象或数组
        // (此策略对于原始JSON是有效的)
        int startIndexObject = response.indexOf('{');
        int startIndexArray = response.indexOf('[');
        int startIndex = -1;

        if (startIndexObject != -1 && startIndexArray != -1) {
            startIndex = Math.min(startIndexObject, startIndexArray);
        } else {
            startIndex = Math.max(startIndexObject, startIndexArray);
        }

        if (startIndex == -1) {
            return null; // 找不到任何JSON起始符号
        }

        char startChar = response.charAt(startIndex);
        char endChar = (startChar == '{') ? '}' : ']';
        int balance = 0;
        int endIndex = -1;

        for (int i = startIndex; i < response.length(); i++) {
            char currentChar = response.charAt(i);
            // 注意：这里简化处理，未考虑字符串或注释中的括号，对于多数LLM场景足够
            if (currentChar == startChar) {
                balance++;
            } else if (currentChar == endChar) {
                balance--;
            }
            if (balance == 0) {
                endIndex = i;
                break;
            }
        }

        if (endIndex != -1) {
            return response.substring(startIndex, endIndex + 1).trim();
        }

        return null; // 未找到匹配的结束括号
    }
}
