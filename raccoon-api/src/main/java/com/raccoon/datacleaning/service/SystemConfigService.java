package com.raccoon.datacleaning.service;

import com.raccoon.datacleaning.model.SystemConfig;
import com.raccoon.datacleaning.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            updateConfig(entry.getKey(), entry.getValue(), updatedBy);
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
