package com.puti.code.ai.documentation;

import com.puti.code.ai.config.BatchProcessingConfig;
import com.puti.code.ai.support.AISupport;
import com.puti.code.ai.support.TokenSupport;
import com.puti.code.base.model.MethodInfo;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;



/**
 * 分批文档生成服务
 * 处理大内容的分批生成和合并
 *
 * @author diodehe
 */
@Slf4j
@Service
public class BatchDocumentationService {

    private final PromptBuilder promptBuilder;
    private final ChatClient simpleChatClient;
    private final ExecutorService executorService;

    // 重试配置
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 10000;

    @Autowired
    public BatchDocumentationService(
            @Qualifier("simpleChatClient") ChatClient simpleChatClient,
            PromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;

        this.simpleChatClient = simpleChatClient;

        this.executorService = Executors.newFixedThreadPool(BatchProcessingConfig.getMaxConcurrentBatches());
        log.info("分批文档生成服务已初始化，最大并发数: {}，支持多轮对话模式", BatchProcessingConfig.getMaxConcurrentBatches());
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            log.info("分批文档生成服务已关闭");
        }
    }

    /**
     * 检查是否需要分批处理
     *
     * @param context 文档生成上下文
     * @return 是否需要分批处理
     */
    public boolean needsBatchProcessing(DocumentationGenerationContext context) {
        // 获取当前层级的方法
        List<MethodInfo> methods = context.getMethodsAtLevel();

        // 获取模型配置
        int maxTokens = BatchProcessingConfig.getMaxTokensForLevel(context.getLevel());

        int estimatedTokens = TokenSupport.calculateTokens(methods, ModelConfig.getDefaultConfig().getEncodingType());

        boolean needsBatch = estimatedTokens > maxTokens;
        if (needsBatch) {
            log.info("第{}层内容过大({} tokens > {} tokens)，需要分批处理",
                    context.getLevel(), estimatedTokens, maxTokens);
        }

        return needsBatch;
    }

    /**
     * 分批生成文档（支持所有层级）
     * 根据配置选择使用传统并行模式或多轮对话模式
     *
     * @param context 文档生成上下文
     * @return 合并后的完整文档内容
     */
    public String generateBatchDocumentation(DocumentationGenerationContext context) {
        // 可以通过配置决定使用哪种模式，这里默认使用多轮对话模式
        return generateConversationalDocumentation(context);
//        boolean useConversationalMode = shouldUseConversationalMode(context);
//
//        if (useConversationalMode) {
//            return generateConversationalDocumentation(context);
//        } else {
//            return generateTraditionalBatchDocumentation(context);
//        }
    }


    /**
     * 多轮对话式文档生成
     * 手动管理对话上下文，每轮对话都携带初始prompt和历史对话信息
     *
     * @param context 文档生成上下文
     * @return 完整的文档内容
     */
    public String generateConversationalDocumentation(DocumentationGenerationContext context) {
        try {
            log.info("开始多轮对话式生成第{}层文档，入口点: {}", context.getLevel(), context.getEntryPointId());

            // 1. 构建分批提示词
            List<String> batchPrompts = promptBuilder.buildBatchPrompts(context);
            if (batchPrompts.isEmpty()) {
                log.error("构建分批提示词失败");
                return null;
            }

            log.info("将第{}层文档分为 {} 轮对话处理", context.getLevel(), batchPrompts.size());

            // 2. 创建对话上下文，设置初始prompt
            String initialPrompt = promptBuilder.buildInitializationPromptString(context, batchPrompts.size());
            ConversationContext conversationContext = ConversationContext.builder()
                    .initialPrompt(initialPrompt)
                    .docContext(context)
                    .rounds(new ArrayList<>())
                    .build();

            // 3. 逐轮进行对话，每轮都基于完整的上下文
            String finalResponse = null;

            for (int i = 0; i < batchPrompts.size(); i++) {
                log.info("开始第 {} 轮对话 (共 {} 轮)", i + 1, batchPrompts.size());

                Prompt fullPrompt;
                boolean isLastRound = (i == batchPrompts.size() - 1);

                if (isLastRound) {
                    // 最后一轮：分析当前批次 + 生成最终完整文档
                    fullPrompt = buildFinalRoundPrompt(conversationContext, batchPrompts.get(i));
                    log.info("最后一轮对话，将同时完成批次分析和最终文档生成");
                } else {
                    // 常规轮次：只分析当前批次
                    fullPrompt = buildFullPrompt(conversationContext, batchPrompts.get(i));
                }

                // 调用AI服务
                String roundResponse = AISupport.callAIServiceWithRetry(simpleChatClient, fullPrompt);

                if (roundResponse != null && !roundResponse.trim().isEmpty()) {
                    log.info("完成第 {} 轮对话，内容长度: {} 字符", i + 1, roundResponse.length());

                    if (isLastRound) {
                        // 最后一轮的响应就是最终文档
                        finalResponse = roundResponse;
                    } else {
                        // 将本轮对话结果添加到上下文中
                        conversationContext.addRound(i + 1, batchPrompts.get(i), roundResponse);
                    }
                } else {
                    log.warn("第 {} 轮对话返回空内容", i + 1);
                }
            }

            log.info("完成第{}层多轮对话文档生成，最终内容长度: {} 字符",
                    context.getLevel(), finalResponse != null ? finalResponse.length() : 0);

            return finalResponse;

        } catch (Exception e) {
            log.error("多轮对话生成文档时发生错误", e);
            return null;
        }
    }

    /**
     * 构建包含完整上下文的prompt
     *
     * @param context       对话上下文
     * @param currentPrompt 当前轮次的具体任务prompt
     * @return 包含完整上下文的prompt
     */
    private Prompt buildFullPrompt(ConversationContext context, String currentPrompt) {
        SystemMessage systemMessage = promptBuilder.buildSystemMessage();

        String userMessageContent = context.getInitialPrompt() +
                context.getHistorySummary() +
                "\n\n## 当前轮次任务：\n" +
                currentPrompt;

        UserMessage userMessage = new UserMessage(userMessageContent);
        return new Prompt(List.of(systemMessage, userMessage));
    }

    /**
     * 构建最后一轮的prompt（包含当前批次分析 + 最终文档生成）
     *
     * @param context       对话上下文
     * @param currentPrompt 当前轮次的具体任务prompt
     * @return 最后一轮的完整prompt
     */
    private Prompt buildFinalRoundPrompt(ConversationContext context, String currentPrompt) {

        SystemMessage systemMessage = promptBuilder.buildSystemMessage();

        String userMessageContent = context.getInitialPrompt() +
                context.getHistorySummary() +
                "\n\n## 当前轮次任务：\n" +
                currentPrompt +
                "\n\n## 重要：这是最后一轮对话\n" +
                promptBuilder.buildFinalIntegrationPromptString(context.getDocContext());

        UserMessage userMessage = new UserMessage(userMessageContent);
        return new Prompt(List.of(systemMessage, userMessage));
    }





    /**
     * 传统的并行分批文档生成（保持向后兼容）
     *
     * @param context 文档生成上下文
     * @return 合并后的完整文档内容
     */
    public String generateTraditionalBatchDocumentation(DocumentationGenerationContext context) {
        try {
            log.info("开始传统分批生成第{}层文档，入口点: {}", context.getLevel(), context.getEntryPointId());

            // 1. 构建分批提示词
            List<String> batchPrompts = promptBuilder.buildBatchPrompts(context);

            if (batchPrompts.isEmpty()) {
                log.error("构建分批提示词失败");
                return null;
            }

            log.info("将第{}层文档分为 {} 批处理", context.getLevel(), batchPrompts.size());

            // 2. 并行生成各批次内容
            List<CompletableFuture<String>> futures = new ArrayList<>();

            for (int i = 0; i < batchPrompts.size(); i++) {
                final int batchIndex = i;
                final String prompt = batchPrompts.get(i);

                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        log.info("开始生成第 {} 批文档内容", batchIndex + 1);
                        String content = "";
//                                callAIServiceWithRetry(prompt);
                        log.info("完成第 {} 批文档生成，内容长度: {} 字符", batchIndex + 1,
                                content != null ? content.length() : 0);
                        return content;
                    } catch (Exception e) {
                        log.error("生成第 {} 批文档失败", batchIndex + 1, e);
                        return null;
                    }
                }, executorService);

                futures.add(future);
            }

            // 3. 等待所有批次完成并收集结果
            List<String> batchResults = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            // 4. 合并批次结果
            String mergedContent = mergeBatchResults(batchResults, context);

            log.info("完成第{}层传统分批文档生成，最终内容长度: {} 字符",
                    context.getLevel(), mergedContent != null ? mergedContent.length() : 0);

            return mergedContent;

        } catch (Exception e) {
            log.error("传统分批生成文档时发生错误", e);
            return null;
        }
    }

    /**
     * 合并批次结果
     */
    private String mergeBatchResults(List<String> batchResults, DocumentationGenerationContext context) {
        if (batchResults == null || batchResults.isEmpty()) {
            return null;
        }

        // 过滤掉空结果
        List<String> validResults = batchResults.stream()
                .filter(result -> result != null && !result.trim().isEmpty())
                .toList();

        if (validResults.isEmpty()) {
            log.warn("所有批次结果都为空");
            return null;
        }

        // 简单合并策略：用分隔符连接
        StringBuilder merged = new StringBuilder();
        merged.append("# 第").append(context.getLevel()).append("层级完整说明书\n\n");

        for (int i = 0; i < validResults.size(); i++) {
            if (i > 0) {
                merged.append("\n---\n\n");
            }
            merged.append("## 第").append(i + 1).append("部分\n\n");
            merged.append(validResults.get(i));
        }

        return merged.toString();
    }
}
