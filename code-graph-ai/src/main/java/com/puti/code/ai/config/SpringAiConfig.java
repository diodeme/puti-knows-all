package com.puti.code.ai.config;

import com.puti.code.base.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;

import java.time.Duration;

/**
 * Spring AI 配置类
 * 配置 ChatClient 和 EmbeddingClient
 *
 * @author diodehe
 */
@Slf4j
@Configuration
public class SpringAiConfig {

    private final AppConfig appConfig;

    public SpringAiConfig() {
        this.appConfig = AppConfig.getInstance();
        log.info("Spring AI 配置初始化完成");
    }

    /**
     * 配置 OpenAI Chat API
     */
    @Bean
    public OpenAiApi openAiChatApi() {
        String apiKey = appConfig.getChatApiKey();
        String baseUrl = appConfig.getChatUrl();
        String completionPath = appConfig.getChatCompletionPath();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Chat API Key 未配置");
        }

        log.info("配置 OpenAI Chat API，Base URL: {}", baseUrl);
        return OpenAiApi.builder().baseUrl(baseUrl).completionsPath(completionPath).apiKey(apiKey).build();
    }

    /**
     * 配置 OpenAI Embedding API
     */
    @Bean
    public OpenAiApi openAiEmbeddingApi() {
        String apiKey = appConfig.getEmbeddingApiKey();
        String baseUrl = appConfig.getEmbeddingUrl();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Embedding API Key 未配置");
        }

        log.info("配置 OpenAI Embedding API，Base URL: {}", baseUrl);
        return OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build();
    }

    /**
     * 配置 Chat Model
     */
    @Bean
    public OpenAiChatModel openAiChatModel(@Qualifier("openAiChatApi") OpenAiApi openAiChatApi) {
        String modelName = appConfig.getChatModel();

        log.info("配置 Chat Model: {}", modelName);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelName)
                .temperature(0.7d)
                .build();
        return OpenAiChatModel.builder().openAiApi(openAiChatApi)
                .defaultOptions(options).build();
    }

    /**
     * 配置 Embedding Model
     */
    @Bean
    public OpenAiEmbeddingModel openAiEmbeddingModel(@Qualifier("openAiEmbeddingApi") OpenAiApi openAiEmbeddingApi) {
        String modelName = appConfig.getEmbeddingModel();

        log.info("配置 Embedding Model: {}", modelName);
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(modelName)
                .build();
        return new OpenAiEmbeddingModel(openAiEmbeddingApi, MetadataMode.EMBED, options);
    }

    @Bean
    public ChatClient simpleChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个专业的代码分析和文档生成助手。请用中文回答，使用 Markdown 格式输出。")
                .build();
    }

    @Bean
    public ChatClient multiConversationChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个专业的代码分析和文档生成助手。请用中文回答，使用 Markdown 格式输出。" +
                        "你正在参与一个多轮对话来生成完整的代码文档，请基于之前的对话历史和当前提供的信息，" +
                        "生成连贯、完整的文档内容。")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(new InMemoryChatMemory()).build())
                .build();
    }

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> {
            restClientBuilder
                    .requestFactory(new BufferingClientHttpRequestFactory(
                            ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                                    .withConnectTimeout(Duration.ofSeconds(600))
                                    .withReadTimeout(Duration.ofSeconds(1200))
                            )));
        };
    }
}
