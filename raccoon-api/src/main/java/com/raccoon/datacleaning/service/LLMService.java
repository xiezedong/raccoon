package com.raccoon.datacleaning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raccoon.datacleaning.config.DataCleaningProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.Semaphore;
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
    private volatile Semaphore limiter;
    private volatile int limiterPermits = 0;
    private final Object intervalLock = new Object();
    private long nextRequestAt = 0;

    /**
     * 调用大模型 API
     * 
     * @param prompt 提示词
     * @return 大模型响应内容
     */
    public String chat(String prompt) {
        Semaphore limiter = getLimiter(getMaxConcurrency());

        boolean acquired = false;
        try {
            awaitRequestInterval();
            limiter.acquire();
            acquired = true;

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

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待模型请求配额（间隔或并发）时被中断", e);
        } catch (IOException e) {
            log.error("调用 LLM API 异常", e);
            throw new RuntimeException("调用 LLM API 异常", e);
        } finally {
            if (acquired) {
                limiter.release();
            }
        }
    }

    /**
     * 模型请求最大并发数，未配置时为 1（串行）
     */
    private int getMaxConcurrency() {
        return Math.max(1, systemConfigService.getIntConfig("llm.max_concurrency", 1));
    }

    /**
     * 两次请求开始之间至少间隔 llm.request_interval_seconds 秒，0 或负数表示不限制。
     * 先预约再抢并发闸门，所以实际间隔只会大于配置值、不会小于。
     */
    private void awaitRequestInterval() throws InterruptedException {
        long intervalMs = getIntervalMs();
        if (intervalMs <= 0) {
            return;
        }

        long waitMs;
        synchronized (intervalLock) {
            long now = System.currentTimeMillis();
            long startAt = Math.max(now, nextRequestAt);
            nextRequestAt = startAt + intervalMs;
            waitMs = startAt - now;
        }

        if (waitMs > 0) {
            log.debug("模型请求间隔等待 {} ms", waitMs);
            Thread.sleep(waitMs);
        }
    }

    /**
     * 每次读取，配置改完下一批请求即生效，无需重启
     */
    private long getIntervalMs() {
        return systemConfigService.getIntConfig("llm.request_interval_seconds", 0) * 1000L;
    }

    /**
     * 全局并发闸门。配置值变化时重建，旧闸门上的在途请求用完后自然废弃
     */
    private Semaphore getLimiter(int permits) {
        Semaphore current = limiter;
        if (current != null && limiterPermits == permits) {
            return current;
        }
        synchronized (this) {
            if (limiter == null || limiterPermits != permits) {
                limiter = new Semaphore(permits, true);
                limiterPermits = permits;
            }
            return limiter;
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
