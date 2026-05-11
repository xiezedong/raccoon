package com.raccoon.datacleaning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raccoon.datacleaning.config.DataCleaningProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 大模型服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMService {

    private final DataCleaningProperties properties;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private OkHttpClient httpClient;

    /**
     * 调用大模型 API
     * 
     * @param prompt 提示词
     * @return 大模型响应内容
     */
    public String chat(String prompt) {
        try {
            OkHttpClient client = getHttpClient();
            
            // 构建请求体
            String requestBody = buildRequestBody(prompt);
            
            // 从数据库获取配置（优先级更高）
            String apiUrl = getConfigValue("llm.api_url", properties.getLlm().getApiUrl());
            String apiKey = getConfigValue("llm.api_key", properties.getLlm().getApiKey());
            
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();
            
            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("LLM API 调用失败: {}", response.code());
                    throw new RuntimeException("LLM API 调用失败: " + response.code());
                }
                
                String responseBody = response.body().string();
                return parseResponse(responseBody);
            }
            
        } catch (IOException e) {
            log.error("调用 LLM API 异常", e);
            throw new RuntimeException("调用 LLM API 异常", e);
        }
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(String prompt) throws IOException {
        String model = getConfigValue("llm.model", properties.getLlm().getModel());
        String temperature = getConfigValue("llm.temperature", properties.getLlm().getTemperature().toString());
        String maxTokens = getConfigValue("llm.max_tokens", properties.getLlm().getMaxTokens().toString());
        
        String json = String.format("""
            {
                "model": "%s",
                "messages": [
                    {
                        "role": "system",
                        "content": "你是一个数据清洗专家，擅长识别数据中的变体、简称、错别字等问题。"
                    },
                    {
                        "role": "user",
                        "content": %s
                    }
                ],
                "temperature": %s,
                "max_tokens": %s
            }
            """,
            model,
            objectMapper.writeValueAsString(prompt),
            temperature,
            maxTokens
        );
        
        return json;
    }

    /**
     * 解析响应
     */
    private String parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.get("choices");
        
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                if (content != null) {
                    return content.asText();
                }
            }
        }
        
        throw new RuntimeException("无法解析 LLM 响应");
    }

    /**
     * 获取 HTTP 客户端
     */
    private OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(properties.getLlm().getTimeout(), TimeUnit.MILLISECONDS)
                    .readTimeout(properties.getLlm().getTimeout(), TimeUnit.MILLISECONDS)
                    .writeTimeout(properties.getLlm().getTimeout(), TimeUnit.MILLISECONDS)
                    .build();
        }
        return httpClient;
    }

    /**
     * 获取配置值（优先从数据库读取）
     */
    private String getConfigValue(String key, String defaultValue) {
        try {
            String value = systemConfigService.getConfig(key);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            log.warn("从数据库读取配置失败，使用默认值: {}", key);
            return defaultValue;
        }
    }
}
