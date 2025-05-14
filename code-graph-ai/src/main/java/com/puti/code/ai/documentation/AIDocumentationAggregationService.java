package com.puti.code.ai.documentation;

import com.puti.code.ai.support.AISupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档聚合AI服务
 * 负责调用AI模型进行文档聚合分析和内容生成
 *
 * @author diodehe
 */
@Slf4j
@Service
public class AIDocumentationAggregationService {

    @Autowired
    @Qualifier("simpleChatClient")
    private ChatClient chatClient;

    // 聚合分析模板资源
    @Value("classpath:/prompts/aggregation/group-analysis.st")
    private Resource groupAnalysisResource;

    // 分批聚合分析模板资源
    @Value("classpath:/prompts/aggregation/group-analysis-batch.st")
    private Resource batchGroupAnalysisResource;

    // 内容生成模板资源
    @Value("classpath:/prompts/aggregation/content-generation.st")
    private Resource contentGenerationResource;

    // 重试配置
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    /**
     * 分析文档分组
     *
     * @param summariesJson 说明书摘要JSON字符串
     * @return 聚合分组列表
     */
    public <T> List<T> analyzeDocumentationGroups(String summariesJson, Type typeReference) {
        try {
            log.info("开始分析文档分组");

            // 构建分组分析提示词
            Prompt prompt = buildGroupAnalysisPrompt(summariesJson);

            // 调用AI服务
            String response = AISupport.callAIServiceWithRetry(chatClient, prompt);
            if (response == null || response.trim().isEmpty()) {
                log.error("AI模型返回空响应");
                return List.of();
            }

            // 解析AI响应
            return AISupport.parseAIResponse(response, typeReference);

        } catch (Exception e) {
            log.error("分析文档分组时发生错误", e);
            return List.of();
        }
    }

    /**
     * 分批分析文档分组（支持增量聚合）
     *
     * @param summariesJson 当前批次说明书摘要JSON字符串
     * @param previousGroupsJson 之前批次的聚合结果JSON字符串
     * @param batchIndex 当前批次索引
     * @param totalBatches 总批次数
     * @return 聚合分组列表
     */
    public <T> List<T> analyzeBatchDocumentationGroups(String summariesJson, String previousGroupsJson,
                                                       int batchIndex, int totalBatches, Type typeReference) {
        try {
            log.info("开始分批分析文档分组，批次: {}/{}", batchIndex, totalBatches);

            // 构建分批分析提示词
            Prompt prompt = buildBatchGroupAnalysisPrompt(summariesJson, previousGroupsJson, batchIndex, totalBatches);

            // 调用AI服务
            String response = AISupport.callAIServiceWithRetry(chatClient, prompt);
            if (response == null || response.trim().isEmpty()) {
                log.error("AI模型返回空响应");
                return List.of();
            }

            // 解析AI响应
            return AISupport.parseAIResponse(response, typeReference);

        } catch (Exception e) {
            log.error("分批分析文档分组时发生错误，批次: {}/{}", batchIndex, totalBatches, e);
            return List.of();
        }
    }

    /**
     * 生成聚合内容
     *
     * @param aggregationType 聚合类型
     * @param description 聚合描述
     * @param docsContent 文档内容
     * @return 聚合内容对象
     */
    public <T> T generateAggregatedContent(String aggregationType, String description, 
                                         String docsContent, Class<T> responseClass) {
        try {
            log.info("开始生成聚合内容，类型: {}", aggregationType);

            // 构建内容生成提示词
            Prompt prompt = buildContentGenerationPrompt(aggregationType, description, docsContent);

            // 调用AI服务
            String response = AISupport.callAIServiceWithRetry(chatClient, prompt);
            if (response == null || response.trim().isEmpty()) {
                log.error("AI模型返回空响应");
                return null;
            }

            // 解析AI响应
            T content = AISupport.parseAIResponse(response, responseClass);
            log.info("成功生成聚合内容，类型: {}", aggregationType);
            return content;

        } catch (Exception e) {
            log.error("生成聚合内容时发生错误，类型: {}", aggregationType, e);
            return null;
        }
    }

    /**
     * 构建分组分析提示词
     */
    private Prompt buildGroupAnalysisPrompt(String summariesJson) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("summariesJson", summariesJson);

        PromptTemplate template = new PromptTemplate(groupAnalysisResource);
        UserMessage userMessage = new UserMessage(template.render(variables));

        return new Prompt(List.of(buildSystemMessage(), userMessage));
    }

    /**
     * 构建分批分组分析提示词
     */
    private Prompt buildBatchGroupAnalysisPrompt(String summariesJson, String previousGroupsJson,
                                                int batchIndex, int totalBatches) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("summariesJson", summariesJson);
        variables.put("previousGroupsJson", previousGroupsJson);
        variables.put("batchIndex", batchIndex);
        variables.put("totalBatches", totalBatches);
        variables.put("isFirstBatch", batchIndex == 1);
        variables.put("isLastBatch", batchIndex == totalBatches);

        PromptTemplate template = new PromptTemplate(batchGroupAnalysisResource);
        UserMessage userMessage = new UserMessage(template.render(variables));

        return new Prompt(List.of(buildSystemMessage(), userMessage));
    }

    /**
     * 构建内容生成提示词
     */
    private Prompt buildContentGenerationPrompt(String aggregationType, String description, String docsContent) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("aggregationType", aggregationType);
        variables.put("description", description);
        variables.put("docsContent", docsContent);

        PromptTemplate template = new PromptTemplate(contentGenerationResource);
        UserMessage userMessage = new UserMessage(template.render(variables));

        return new Prompt(List.of(buildSystemMessage(), userMessage));
    }

    /**
     * 构建系统消息
     */
    private SystemMessage buildSystemMessage() {
        return new SystemMessage("你是一个专业的文档聚合和分析助手。请严格按照用户要求的JSON格式返回结果，不要添加任何其他内容。");
    }
}
