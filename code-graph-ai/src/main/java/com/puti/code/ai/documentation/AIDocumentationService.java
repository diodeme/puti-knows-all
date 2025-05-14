package com.puti.code.ai.documentation;

import com.puti.code.ai.documentation.dto.AIDocumentationResponse;
import com.puti.code.ai.support.AISupport;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * AI文档生成服务
 * 负责调用AI大模型生成说明书内容
 *
 * @author diodehe
 */
@Slf4j
@Service
public class AIDocumentationService {

    @Qualifier("simpleChatClient")
    @Resource
    private ChatClient chatClient;
    @Resource
    private PromptBuilder promptBuilder;
    @Resource
    private BatchDocumentationService batchDocumentationService;

    /**
     * 同步生成文档内容
     *
     * @param context 文档生成上下文
     * @return 生成的文档响应对象
     */
    public AIDocumentationResponse generateDocumentationForLevel(DocumentationGenerationContext context) {
        try {
            log.info("开始为层级 {} 生成文档，入口点: {}", context.getLevel(), context.getEntryPointId());

            // 检查是否需要分批处理（所有层级都支持）
            if (batchDocumentationService.needsBatchProcessing(context)) {
                log.info("第{}层内容过大，使用分批处理模式", context.getLevel());
                String batchContent = batchDocumentationService.generateBatchDocumentation(context);

                if (StringUtils.isBlank(batchContent)) {
                    log.error("分批生成文档失败");
                    return null;
                }

                // 解析分批生成的内容
                AIDocumentationResponse response = parseAIResponse(batchContent);
                if (response != null) {
                    log.info("成功完成第{}层分批文档生成", context.getLevel());
                    return response;
                } else {
                    log.error("解析分批生成的文档响应失败");
                    return null;
                }
            }

            // 常规单次生成流程
            Prompt prompt = promptBuilder.buildLevelAnalysisPrompt(context);

            String content = AISupport.callAIServiceWithRetry(chatClient, prompt);

            if (content == null || content.trim().isEmpty()) {
                log.error("AI服务返回空内容");
                return null;
            }

            // 3. 解析AI响应
            AIDocumentationResponse response = parseAIResponse(content);

            if (response != null) {
                log.info("成功生成层级 {} 的文档", context.getLevel());
                return response;
            } else {
                log.error("解析AI响应失败");
                return null;
            }

        } catch (Exception e) {
            log.error("生成文档时发生错误", e);
            return null;
        }
    }



    /**
     * 解析AI返回的JSON响应
     *
     * @param aiResponse AI返回的原始响应
     * @return 解析后的文档响应对象
     */
    private AIDocumentationResponse parseAIResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            log.error("AI响应为空");
            return null;
        }

        try {
            // 使用Json类解析JSON
            AIDocumentationResponse response = AISupport.parseAIResponse(aiResponse, AIDocumentationResponse.class);

            if (response == null) {
                log.error("JSON解析失败: {}", aiResponse);
                return null;
            }

            if (!response.isValid()) {
                log.error("AI响应JSON格式不完整，缺少必要字段: {}", aiResponse);
                return null;
            }

            log.debug("成功解析AI响应，title: {}, summary长度: {}, content长度: {}",
                    response.getTitle(),
                    response.getSummary() != null ? response.getSummary().length() : 0,
                    response.getContent() != null ? response.getContent().length() : 0);

            return response;

        } catch (Exception e) {
            log.error("解析AI响应JSON时发生错误: {}", e.getMessage(), e);
            return null;
        }
    }

}
