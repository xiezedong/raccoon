package com.raccoon.datacleaning.service;

import com.raccoon.datacleaning.model.SystemConfig;
import com.raccoon.datacleaning.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 获取所有配置
     */
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    /**
     * 获取配置（以 Map 形式返回）
     */
    public Map<String, String> getConfigMap() {
        return systemConfigRepository.findAll().stream()
                .collect(Collectors.toMap(
                        SystemConfig::getConfigKey,
                        config -> config.getConfigValue() != null ? config.getConfigValue() : ""
                ));
    }

    /**
     * 获取单个配置
     */
    public String getConfig(String key) {
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(null);
    }

    /**
     * 获取单个配置（带默认值）
     */
    public String getConfig(String key, String defaultValue) {
        String value = getConfig(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取整数配置
     */
    public int getIntConfig(String key, int defaultValue) {
        String value = getConfig(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置 {} 的值 {} 不是有效的整数，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * 获取布尔配置
     */
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = getConfig(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 获取浮点配置
     */
    public double getDoubleConfig(String key, double defaultValue) {
        String value = getConfig(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("配置 {} 的值 {} 不是有效的浮点数，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 更新配置
     */
    @Transactional
    public void updateConfig(String key, String value, String updatedBy) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElse(new SystemConfig());
        
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setUpdatedBy(updatedBy);
        
        systemConfigRepository.save(config);
        log.info("更新配置: {} = {}", key, value);
    }

    /**
     * 批量更新配置
     */
    @Transactional
    public void batchUpdateConfigs(Map<String, String> configs, String updatedBy) {
        boolean needReschedule = false;
        
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            updateConfig(entry.getKey(), entry.getValue(), updatedBy);
            
            // 检查是否需要重新调度
            if (entry.getKey().startsWith("ai_discovery.scheduled.")) {
                needReschedule = true;
            }
        }
        
        // 如果定时发现配置变更，发布事件
        if (needReschedule) {
            log.info("定时发现配置已变更，将重新调度任务");
            eventPublisher.publishEvent(new ScheduleConfigChangedEvent(this));
        }
    }

    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(String key) {
        systemConfigRepository.findByConfigKey(key)
                .ifPresent(config -> {
                    systemConfigRepository.delete(config);
                    log.info("删除配置: {}", key);
                });
    }

    /**
     * 初始化默认配置
     */
    @Transactional
    public void initDefaultConfigs() {
        // 目标数据库配置
        setDefaultConfig("target.db.host", "localhost", "目标数据库主机地址");
        setDefaultConfig("target.db.port", "5432", "目标数据库端口");
        setDefaultConfig("target.db.database", "raccoon_db", "目标数据库名称");
        setDefaultConfig("target.db.username", "postgres", "目标数据库用户名");
        setDefaultConfig("target.db.password", "postgres", "目标数据库密码");
        
        // 大模型配置
        setDefaultConfig("llm.api_url", "https://api.openai.com/v1/chat/completions", "大模型API地址");
        setDefaultConfig("llm.api_key", "", "大模型API密钥");
        setDefaultConfig("llm.model", "gpt-4", "大模型名称");
        setDefaultConfig("llm.temperature", "0.3", "大模型温度");
        setDefaultConfig("llm.max_tokens", "2000", "最大Token数");
        setDefaultConfig("llm.batch_size", "50", "批处理大小");
        
        // 自学习配置
        setDefaultConfig("self_learning.enabled", "true", "是否启用自学习");
        setDefaultConfig("self_learning.auto_add_threshold", "0.95", "自动添加规则的置信度阈值");
        setDefaultConfig("self_learning.auto_apply_threshold", "0.98", "自动应用清洗的置信度阈值");
        
        // 安全配置
        setDefaultConfig("safety.max_auto_clean_records", "1000", "单次自动清洗的最大记录数");
        setDefaultConfig("safety.manual_confirm_threshold", "100", "需要人工确认的影响记录数阈值");
        
        // 定时扫描配置
        setDefaultConfig("scan.scheduled.enabled", "false", "是否启用定时扫描");
        setDefaultConfig("scan.scheduled.preset", "disabled", "预设时间");
        setDefaultConfig("scan.scheduled.cron", "0 0 2 * * ?", "自定义Cron表达式");
        setDefaultConfig("scan.scheduled.min_interval_hours", "6", "最小执行间隔（小时）");
        
        // 定时 AI 发现配置
        setDefaultConfig("ai_discovery.scheduled.enabled", "false", "是否启用定时AI发现");
        setDefaultConfig("ai_discovery.scheduled.preset", "disabled", "预设时间");
        setDefaultConfig("ai_discovery.scheduled.cron", "0 0 2 * * ?", "自定义Cron表达式");
        setDefaultConfig("ai_discovery.scheduled.min_interval_hours", "6", "最小执行间隔（小时）");
        setDefaultConfig("ai_discovery.scheduled.timeout_hours", "2", "超时时间（小时）");
        setDefaultConfig("ai_discovery.scheduled.max_fields_per_run", "50", "单次最大扫描字段数");
        
        // AI 发现提示词模板（留空则使用默认提示词）
        setDefaultConfig("ai_discovery.prompt_template", "", "AI发现提示词模板（支持占位符：{{tableName}}, {{columnName}}, {{columnDescription}}, {{existingRules}}, {{values}}）");
    }

    /**
     * 配置变更事件
     */
    public static class ScheduleConfigChangedEvent {
        private final Object source;
        
        public ScheduleConfigChangedEvent(Object source) {
            this.source = source;
        }
        
        public Object getSource() {
            return source;
        }
    }

    /**
     * 设置默认配置（如果不存在）
     */
    private void setDefaultConfig(String key, String value, String description) {
        if (!systemConfigRepository.existsByConfigKey(key)) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            config.setUpdatedBy("system");
            systemConfigRepository.save(config);
        }
    }
}
