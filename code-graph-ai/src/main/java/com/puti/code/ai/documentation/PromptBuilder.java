package com.puti.code.ai.documentation;

import com.puti.code.ai.config.BatchProcessingConfig;
import com.puti.code.ai.support.TokenSupport;
import com.puti.code.base.model.MethodInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.Content;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于Spring AI PromptTemplate的提示词构建器
 * 使用外部化模板和结构化消息构建高质量提示词
 *
 * @author diodehe
 */
@Slf4j
@Component
public class PromptBuilder {

    // 系统消息模板资源
    @Value("classpath:/prompts/document/system-base.st")
    private Resource systemBaseResource;

    @Value("classpath:/prompts/document/single-analysis.st")
    private Resource singleAnalysisResource;

    // 共享模板片段资源
    @Value("classpath:/prompts/document/shared/document-structure.st")
    private Resource documentStructureResource;

    @Value("classpath:/prompts/document/shared/analysis-requirements-level1.st")
    private Resource level1RequirementsResource;

    @Value("classpath:/prompts/document/shared/analysis-requirements-level2.st")
    private Resource level2RequirementsResource;

    @Value("classpath:/prompts/document/shared/analysis-requirements-level3.st")
    private Resource level3RequirementsResource;

    @Value("classpath:/prompts/document/shared/integration-requirements.st")
    private Resource integrationRequirementsResource;

    // 多轮对话模板资源
    @Value("classpath:/prompts/document/conversational-init.st")
    private Resource conversationalInitResource;

    @Value("classpath:/prompts/document/conversational-final.st")
    private Resource conversationalFinalResource;

    // 统一的批次分析模板资源
    @Value("classpath:/prompts/document/batch-analysis.st")
    private Resource batchAnalysisResource;

    /**
     * 构建系统消息
     */
    public SystemMessage buildSystemMessage() {
        PromptTemplate systemTemplate = new PromptTemplate(systemBaseResource);
        return new SystemMessage(systemTemplate.render());
    }

    /**
     * 构建层级分析的完整Prompt
     */
    public Prompt buildLevelAnalysisPrompt(DocumentationGenerationContext context) {
        SystemMessage systemMessage = buildSystemMessage();
        UserMessage userMessage = buildLevelAnalysisUserMessage(context);

        return new Prompt(List.of(systemMessage, userMessage));
    }

    /**
     * 构建层级分析的用户消息
     * 使用模板引用方式，将共享片段注入到主模板中
     */
    private UserMessage buildLevelAnalysisUserMessage(DocumentationGenerationContext context) {
        // 获取层级特定的模板资源
        Resource templateResource = singleAnalysisResource;

        // 构建变量映射，包含共享片段的内容
        Map<String, Object> variables = buildAnalysisVariables(context);
        variables.put("codeContent", buildCodeContent(context));
        variables.put("methodCount", context.getMethodCountForLevel());

        // 渲染共享片段
        variables.put("documentStructure", renderTemplate(documentStructureResource, variables));
        variables.put("analysisRequirements", renderTemplate(getLevelRequirementsResource(context.getLevel()), variables));

        // 渲染主模板
        PromptTemplate mainTemplate = new PromptTemplate(templateResource);
        String userContent = mainTemplate.render(variables);

        return new UserMessage(userContent);
    }

    /**
     * 构建层级分析的模板变量
     */
    private Map<String, Object> buildAnalysisVariables(DocumentationGenerationContext context) {
        Map<String, Object> variables = new HashMap<>();

        variables.put("entryPoint", context.getEntryPointId());
        variables.put("level", context.getLevel());
        variables.put("levelDescription", getLevelDescription(context.getLevel()));
        variables.put("projectId", context.getProjectId() != null ? context.getProjectId() : "未指定");
        variables.put("branchName", context.getBranchName() != null ? context.getBranchName() : "未指定");
        // 如果有前一层文档，添加到变量中
        variables.put("previousDocumentation", null);
        if (context.getPreviousDocumentation() != null) {
            variables.put("previousDocumentation", context.getPreviousDocumentation().getContent());
        }

        return variables;
    }

    /**
     * 构建多轮对话初始化Prompt
     */
    public Prompt buildConversationalInitPrompt(DocumentationGenerationContext context, int batchCount) {
        SystemMessage systemMessage = buildSystemMessage();
        UserMessage userMessage = buildConversationalInitUserMessage(context, batchCount);

        return new Prompt(List.of(systemMessage, userMessage));
    }

    /**
     * 构建多轮对话初始化的用户消息
     */
    private UserMessage buildConversationalInitUserMessage(DocumentationGenerationContext context, int batchCount) {
        Map<String, Object> variables = buildAnalysisVariables(context);
        variables.put("batchCount", batchCount);
        variables.put("methodCount", context.getSubgraph().getAllMethods().size());
        PromptTemplate userTemplate = new PromptTemplate(conversationalInitResource);
        String userContent = userTemplate.render(variables);

        return new UserMessage(userContent);
    }


    /**
     * 构建多轮对话最终整合Prompt
     */
    public Prompt buildConversationalFinalPrompt(DocumentationGenerationContext context) {
        UserMessage userMessage = buildConversationalFinalUserMessage(context);
        return new Prompt(List.of(userMessage));
    }

    /**
     * 构建多轮对话最终整合的用户消息
     * 使用模板引用方式，复用共享的文档结构
     */
    private UserMessage buildConversationalFinalUserMessage(DocumentationGenerationContext context) {
        Map<String, Object> variables = buildAnalysisVariables(context);

        // 渲染共享片段
        variables.put("documentStructure", renderTemplate(documentStructureResource, variables));
        variables.put("integrationRequirements", renderTemplate(integrationRequirementsResource, variables));

        // 渲染多轮对话最终模板
        PromptTemplate finalTemplate = new PromptTemplate(conversationalFinalResource);
        String userContent = finalTemplate.render(variables);

        return new UserMessage(userContent);
    }

    /**
     * 构建代码内容字符串
     */
    private String buildCodeContent(DocumentationGenerationContext context) {
        if (context.getSubgraph() == null) {
            return "无代码内容";
        }

        List<MethodInfo> methods = context.getMethodsAtLevel();
        StringBuilder codeContent = new StringBuilder();
        appendMethodsContent(codeContent, methods);
        return codeContent.toString();
    }

    /**
     * 获取层级对应的分析要求资源
     */
    private Resource getLevelRequirementsResource(int level) {
        return switch (level) {
            case 1 -> level1RequirementsResource;
            case 2 -> level2RequirementsResource;
            default -> level3RequirementsResource;
        };
    }

    /**
     * 获取层级描述
     */
    private String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> "核心流程";
            case 2 -> "详细流程";
            default -> "完整系统";
        };
    }

    /**
     * 渲染模板片段
     */
    private String renderTemplate(Resource templateResource, Map<String, Object> variables) {
        PromptTemplate template = new PromptTemplate(templateResource);
        return template.render(variables);
    }


    /**
     * 为指定层级构建分批提示词
     */
    public List<String> buildBatchPrompts(DocumentationGenerationContext context) {
        List<MethodInfo> methods = context.getMethodsAtLevel();
        List<List<MethodInfo>> batches = splitMethodsIntoBatches(methods, context.getLevel());

        List<String> prompts = new ArrayList<>();
        for (int i = 0; i < batches.size(); i++) {
            String prompt = buildBatchPrompt(context, batches.get(i), i, batches.size());
            prompts.add(prompt);
        }

        return prompts;
    }

    /**
     * 构建指定层级的批次提示词
     * 使用统一模板，根据层级动态设置参数
     */
    private String buildBatchPrompt(DocumentationGenerationContext context, List<MethodInfo> methods,
                                    int batchIndex, int totalBatches) {
        // 构建变量映射，包含共享片段的内容
        Map<String, Object> variables = buildAnalysisVariables(context);
        variables.put("batchIndex", batchIndex + 1); // 显示从1开始
        variables.put("totalBatches", totalBatches);

        // 构建代码内容
        StringBuilder codeContent = new StringBuilder();
        appendMethodsContent(codeContent, methods);
        variables.put("codeContent", codeContent.toString());
        variables.put("methodCount", methods.size());

        // 渲染共享片段
        variables.put("documentStructure", renderTemplate(documentStructureResource, variables));
        variables.put("analysisRequirements", renderTemplate(getLevelRequirementsResource(context.getLevel()), variables));


        variables.put("isFirstBatch", batchIndex == 1);
        variables.put("isNotRootLevel", context.getLevel() > 1);
        variables.put("previousLevel", context.getLevel() - 1);
        // 渲染统一模板
        PromptTemplate batchTemplate = new PromptTemplate(batchAnalysisResource);
        return batchTemplate.render(variables);
    }

    private List<List<MethodInfo>> splitMethodsIntoBatches(List<MethodInfo> methods, int level) {
        int maxTokens = BatchProcessingConfig.getMaxTokensForLevel(level);
        int maxTokensForLevel = BatchProcessingConfig.getMaxTokensForLevel(Integer.MAX_VALUE);

        Map<String, Integer> methodInfoTokenMap = methods.stream().collect(Collectors.toMap(MethodInfo::getMethodId,
                e -> TokenSupport.calculateTokens(e, ModelConfig.getDefaultConfig().getEncodingType()), (s1, s2) -> s2));
        List<MethodInfo> methodInfoOrderList = methods.stream().sorted(Comparator.comparingInt(e -> methodInfoTokenMap.get(e.getMethodId())))
                .toList();

        List<List<MethodInfo>> batches = new ArrayList<>();
        List<MethodInfo> currentBatch = new ArrayList<>();
        int currentBatchTokens = 0;

        for (MethodInfo method : methodInfoOrderList) {
            String methodId = method.getMethodId();
            int methodTokens = methodInfoTokenMap.get(methodId);

            // 检查方法是否超出最大允许token限制
            if (methodTokens > maxTokensForLevel) {
                log.error("方法 {} 的token数({})超出了最大允许限制({}), 跳过该方法",
                        methodId, methodTokens, maxTokensForLevel);
                continue;
            }

            // 检查是否需要开启新分组
            if (currentBatchTokens + methodTokens > maxTokens) {
                if (currentBatchTokens <= maxTokensForLevel) {
                    // 当前分组还在容忍范围内，方法加入当前分组，然后开启下一个分组
                    currentBatch.add(method);

                    // 保存当前分组并开启新分组
                    batches.add(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                    currentBatchTokens = 0;
                } else {
                    // 当前分组已经超过maxTokensForLevel，方法进入下一个分组
                    // 先保存当前分组
                    if (!currentBatch.isEmpty()) {
                        batches.add(new ArrayList<>(currentBatch));
                        currentBatch.clear();
                    }

                    // 方法进入新分组
                    currentBatch.add(method);
                    currentBatchTokens = methodTokens;
                }
            } else {
                // 方法可以加入当前分组而不超过maxTokens
                currentBatch.add(method);
                currentBatchTokens += methodTokens;
            }
        }

        // 添加最后一个分组（如果不为空）
        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }

        log.info("方法分组完成，共{}个方法分为{}个批次，每批次token限制: {}, 最大允许token: {}",
                methods.size(), batches.size(), maxTokens, maxTokensForLevel);

        return batches;
    }

    /**
     * 添加方法内容到提示词
     */
    private void appendMethodsContent(StringBuilder prompt, List<MethodInfo> methods) {
        for (int i = 0; i < methods.size(); i++) {
            MethodInfo method = methods.get(i);
            prompt.append(String.format("### 方法 %d: %s\n\n", i + 1, method.getSignature()));

            if (method.getDescription() != null && !method.getDescription().trim().isEmpty()) {
                prompt.append("**描述**: ").append(method.getDescription()).append("\n\n");
            }

            if (method.getContent() != null && !method.getContent().trim().isEmpty()) {
                prompt.append("**代码**:\n```java\n")
                        .append(method.getContent())
                        .append("\n```\n\n");
            }

            prompt.append("---\n\n");
        }
    }


    public String buildInitializationPromptString(DocumentationGenerationContext context, int batchCount) {
        Prompt prompt = buildConversationalInitPrompt(context, batchCount);

        return extractUserMessageContent(prompt);
    }

    public String buildFinalIntegrationPromptString(DocumentationGenerationContext context) {
        Prompt prompt = buildConversationalFinalPrompt(context);
        return extractUserMessageContent(prompt);
    }

    /**
     * 从Prompt中提取用户消息内容
     */
    private String extractUserMessageContent(Prompt prompt) {
        return prompt.getInstructions().stream()
                .filter(message -> message instanceof UserMessage)
                .map(Content::getText)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到用户消息内容"));
    }

    public static void main(String[] args) {
        String template = """
                {if(previousDocumentation)}
                ## 上一层分析总结
                {previousDocumentation}
                {endif}
                """;
        ST st = new ST(template, '{', '}');
//        st.add("previousDocumentation", "1111");
        System.out.println(st.render());
    }
}
