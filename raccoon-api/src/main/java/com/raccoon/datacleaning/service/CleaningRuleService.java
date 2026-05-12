package com.raccoon.datacleaning.service;

import com.raccoon.datacleaning.model.CleaningRule;
import com.raccoon.datacleaning.repository.CleaningRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 清洗规则服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleaningRuleService {

    private final CleaningRuleRepository cleaningRuleRepository;

    /**
     * 创建规则
     */
    @Transactional
    public CleaningRule createRule(CleaningRule rule) {
        log.info("创建清洗规则: {}.{} -> {}", 
            rule.getTableName(), rule.getColumnName(), rule.getStandardValue());
        return cleaningRuleRepository.save(rule);
    }

    /**
     * 批量创建规则
     */
    @Transactional
    public List<CleaningRule> createRules(List<CleaningRule> rules) {
        log.info("批量创建清洗规则: {} 条", rules.size());
        return cleaningRuleRepository.saveAll(rules);
    }

    /**
     * 更新规则
     */
    @Transactional
    public CleaningRule updateRule(Long id, CleaningRule rule) {
        CleaningRule existing = cleaningRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("规则不存在: " + id));
        
        existing.setTableName(rule.getTableName());
        existing.setColumnName(rule.getColumnName());
        existing.setColumnDescription(rule.getColumnDescription());
        existing.setStandardValue(rule.getStandardValue());
        existing.setDirtyValues(rule.getDirtyValues());
        existing.setConfidence(rule.getConfidence());
        existing.setAutoApply(rule.getAutoApply());
        
        return cleaningRuleRepository.save(existing);
    }

    /**
     * 删除规则
     */
    @Transactional
    public void deleteRule(Long id) {
        log.info("删除清洗规则: {}", id);
        cleaningRuleRepository.deleteById(id);
    }
    
    /**
     * 批量删除规则
     */
    @Transactional
    public int batchDeleteRules(List<Long> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return 0;
        }
        
        int deletedCount = 0;
        for (Long ruleId : ruleIds) {
            try {
                cleaningRuleRepository.deleteById(ruleId);
                deletedCount++;
                log.info("删除清洗规则: {}", ruleId);
            } catch (Exception e) {
                log.error("删除规则失败: ruleId={}", ruleId, e);
            }
        }
        
        log.info("批量删除清洗规则: 总数={}, 成功={}", ruleIds.size(), deletedCount);
        return deletedCount;
    }

    /**
     * 根据ID查询规则
     */
    public CleaningRule getRule(Long id) {
        return cleaningRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("规则不存在: " + id));
    }

    /**
     * 查询所有规则
     */
    public List<CleaningRule> getAllRules() {
        return cleaningRuleRepository.findAll();
    }

    /**
     * 根据表名和字段名查询规则
     */
    public List<CleaningRule> getRulesByTableAndColumn(String tableName, String columnName) {
        return cleaningRuleRepository.findByTableNameAndColumnName(tableName, columnName);
    }

    /**
     * 查询自动应用的规则
     */
    public List<CleaningRule> getAutoApplyRules() {
        return cleaningRuleRepository.findByAutoApplyTrue();
    }

    /**
     * 查询高置信度规则
     */
    public List<CleaningRule> getHighConfidenceRules(double threshold) {
        return cleaningRuleRepository.findByConfidenceGreaterThanEqual(BigDecimal.valueOf(threshold));
    }

    /**
     * 更新规则使用统计
     */
    @Transactional
    public void updateRuleUsage(Long ruleId, boolean success) {
        CleaningRule rule = getRule(ruleId);
        rule.setUsageCount(rule.getUsageCount() + 1);
        if (success) {
            rule.setSuccessCount(rule.getSuccessCount() + 1);
        }
        rule.setLastUsedAt(LocalDateTime.now());
        cleaningRuleRepository.save(rule);
    }

    /**
     * 添加错误值到现有规则
     */
    @Transactional
    public CleaningRule addDirtyValue(Long ruleId, String dirtyValue) {
        CleaningRule rule = getRule(ruleId);
        
        // 检查是否已存在
        String[] existingValues = rule.getDirtyValues();
        for (String value : existingValues) {
            if (value.equals(dirtyValue)) {
                log.warn("错误值已存在: {}", dirtyValue);
                return rule;
            }
        }
        
        // 添加新的错误值
        String[] newValues = new String[existingValues.length + 1];
        System.arraycopy(existingValues, 0, newValues, 0, existingValues.length);
        newValues[existingValues.length] = dirtyValue;
        rule.setDirtyValues(newValues);
        
        return cleaningRuleRepository.save(rule);
    }
}
