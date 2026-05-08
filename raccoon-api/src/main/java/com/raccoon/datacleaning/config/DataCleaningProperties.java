package com.raccoon.datacleaning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据清洗配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "data-cleaning")
public class DataCleaningProperties {

    private SelfLearning selfLearning = new SelfLearning();
    private Llm llm = new Llm();
    private Vector vector = new Vector();

    @Data
    public static class SelfLearning {
        private Boolean enabled = true;
        private Double autoAddThreshold = 0.95;
        private Double autoApplyThreshold = 0.98;
        private ScheduledScan scheduledScan = new ScheduledScan();
        private Integer maxColumnsPerScan = 50;
        private Notification notification = new Notification();
        private Safety safety = new Safety();
    }

    @Data
    public static class ScheduledScan {
        private Boolean enabled = true;
        private String cron = "0 0 2 * * ?";
    }

    @Data
    public static class Notification {
        private Boolean enabled = true;
        private String email;
    }

    @Data
    public static class Safety {
        private Integer maxAutoCleanRecords = 1000;
        private Integer manualConfirmThreshold = 100;
    }

    @Data
    public static class Llm {
        private String provider = "openai";
        private String apiKey;
        private String apiUrl = "https://api.openai.com/v1/chat/completions";
        private String model = "gpt-4";
        private Double temperature = 0.3;
        private Integer maxTokens = 2000;
        private Long timeout = 60000L;
        private Integer batchSize = 50;
    }

    @Data
    public static class Vector {
        private Boolean enabled = false;
        private String embeddingModel = "text-embedding-ada-002";
        private String embeddingApiUrl = "https://api.openai.com/v1/embeddings";
        private Double similarityThreshold = 0.7;
    }
}
