package com.puti.code.ai.vector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.puti.code.base.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * 向量生成器，调用 Embedding API 将文本转为向量。
 * 输入文本超过 {@link #MAX_INPUT_LENGTH} 时自动截断，避免 API 返回 413 token 超限错误。
 */
@Slf4j
public class VectorGenerator implements AutoCloseable {

    /** Embedding API 单次请求最大输入字符数（8192 token 的保守上界） */
    private static final int MAX_INPUT_LENGTH = 8000;

    private final AppConfig config;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public VectorGenerator() {
        config = AppConfig.getInstance();
        httpClient = HttpClients.createDefault();
        objectMapper = new ObjectMapper();
    }

    /**
     * 生成文本向量，输入超过 {@link #MAX_INPUT_LENGTH} 时自动截断。
     *
     * @param text 文本
     * @return 向量
     */
    public float[] generateVector(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Empty text provided for vector generation");
            return new float[config.getMilvusDimension()];
        }

        if (text.length() > MAX_INPUT_LENGTH) {
            text = text.substring(0, MAX_INPUT_LENGTH);
        }

        try {
            // 3. 创建 HttpPost 请求
            HttpPost httpPost = getHttpPost(text);

            // 5. 执行请求并获取响应 (使用 try-with-resources 确保关闭)
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity responseEntity = response.getEntity();
                String responseBody = responseEntity != null ? EntityUtils.toString(responseEntity) : null;

                log.info("Response Status Code: " + statusCode);

                if (statusCode == 200) {
                    // 假设响应体是包含嵌入向量的 JSON，你需要解析它
                    // 这里只是返回原始响应体作为示例
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    JsonNode embeddingNode = jsonNode.path("data").path(0).path("embedding");

                    if (embeddingNode.isMissingNode()) {
                        log.error("No embedding found in response: {}", responseBody);
                        return new float[config.getMilvusDimension()];
                    }
                    float[] result = objectMapper.convertValue(embeddingNode, float[].class);

                    log.debug("Generated vector with dimension: {}", result.length);
                    return result;
                } else {
                    log.error("Failed to generate vector: {}", responseBody);
                    return new float[config.getMilvusDimension()];
                }
            }
        } catch (IOException e) {
            log.error("Failed to generate vector", e);
            return new float[config.getMilvusDimension()];
        }
    }

    private HttpPost getHttpPost(String text) throws JsonProcessingException {
        HttpPost httpPost = new HttpPost(config.getEmbeddingUrl());

        // 预处理文本：移除换行符，替换为空格，并转义特殊字符
        String processedText = text.replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .replace("\"", "\\\"");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode requestNode = mapper.createObjectNode();
        requestNode.put("input", processedText);
        requestNode.put("model", config.getEmbeddingModel());

        String requestBody = mapper.writeValueAsString(requestNode);

        StringEntity requestEntity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntity);

//             (可选) 如果需要设置其他头部信息，可以在这里添加
        httpPost.setHeader("Authorization", "Bearer " + config.getEmbeddingApiKey());
        return httpPost;
    }

    /**
     * 关闭HttpClient资源
     */
    @Override
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            log.error("Failed to close HttpClient", e);
        }
    }
}
