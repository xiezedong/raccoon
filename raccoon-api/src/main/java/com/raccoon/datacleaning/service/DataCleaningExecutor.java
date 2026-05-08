package com.raccoon.datacleaning.service;

import com.raccoon.datacleaning.model.CleaningLog;
import com.raccoon.datacleaning.model.CleaningRule;
import com.raccoon.datacleaning.model.CleaningTask;
import com.raccoon.datacleaning.repository.CleaningLogRepository;
import com.raccoon.datacleaning.repository.CleaningTaskRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据清洗执行服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataCleaningExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final CleaningRuleService cleaningRuleService;
    private final CleaningLogRepository cleaningLogRepository;
    private final CleaningTaskRepository cleaningTaskRepository;
    private final DirtyDataDetector dirtyDataDetector;

    /**
     * 执行清洗
     */
    @Transactional
    public CleaningResult executeClean(Long ruleId, String executedBy) {
        CleaningRule rule = cleaningRuleService.getRule(ruleId);
        
        log.info("开始执行清洗: {}.{} -> {}", 
            rule.getTableName(), rule.getColumnName(), rule.getStandardValue());
        
        // 创建清洗任务
        CleaningTask task = createTask(rule, executedBy);
        
        try {
            // 查询需要清洗的数据
            List<DirtyDataDetector.DirtyDataRecord> dirtyRecords = 
                dirtyDataDetector.findDirtyData(rule);
            
            task.setTotalRecords(dirtyRecords.size());
            task.setStatus("running");
            task.setStartedAt(LocalDateTime.now());
            cleaningTaskRepository.save(task);
            
            int cleanedCount = 0;
            List<CleaningLog> logs = new ArrayList<>();
            
            // 逐条清洗
            for (DirtyDataDetector.DirtyDataRecord record : dirtyRecords) {
                try {
                    // 执行更新
                    String updateSql = String.format(
                        "UPDATE %s SET %s = ? WHERE id = ?",
                        rule.getTableName(),
                        rule.getColumnName()
                    );
                    
                    jdbcTemplate.update(updateSql, rule.getStandardValue(), record.getId());
                    
                    // 记录日志
                    CleaningLog cleaningLog = new CleaningLog();
                    cleaningLog.setTableName(rule.getTableName());
                    cleaningLog.setColumnName(rule.getColumnName());
                    cleaningLog.setOldValue(record.getDirtyValue());
                    cleaningLog.setNewValue(rule.getStandardValue());
                    cleaningLog.setRecordId(record.getId());
                    cleaningLog.setRuleId(ruleId);
                    cleaningLog.setExecutedBy(executedBy);
                    logs.add(cleaningLog);
                    
                    cleanedCount++;
                    
                } catch (Exception e) {
                    log.error("清洗记录失败: id={}", record.getId(), e);
                }
            }
            
            // 批量保存日志
            if (!logs.isEmpty()) {
                cleaningLogRepository.saveAll(logs);
            }
            
            // 更新任务状态
            task.setCleanedRecords(cleanedCount);
            task.setStatus("completed");
            task.setCompletedAt(LocalDateTime.now());
            cleaningTaskRepository.save(task);
            
            // 更新规则使用统计
            cleaningRuleService.updateRuleUsage(ruleId, true);
            
            log.info("清洗完成: 总数={}, 成功={}", dirtyRecords.size(), cleanedCount);
            
            CleaningResult result = new CleaningResult();
            result.setTaskId(task.getId());
            result.setTotalRecords(dirtyRecords.size());
            result.setCleanedRecords(cleanedCount);
            result.setSuccess(true);
            
            return result;
            
        } catch (Exception e) {
            log.error("执行清洗失败", e);
            
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            cleaningTaskRepository.save(task);
            
            CleaningResult result = new CleaningResult();
            result.setTaskId(task.getId());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            
            return result;
        }
    }

    /**
     * 预览清洗影响
     */
    public CleaningPreview previewClean(Long ruleId) {
        CleaningRule rule = cleaningRuleService.getRule(ruleId);
        
        // 统计影响的记录数
        int affectedCount = dirtyDataDetector.countDirtyData(rule);
        
        // 获取样本数据
        List<DirtyDataDetector.DirtyDataRecord> samples = 
            dirtyDataDetector.findDirtyData(rule);
        
        CleaningPreview preview = new CleaningPreview();
        preview.setRuleId(ruleId);
        preview.setTableName(rule.getTableName());
        preview.setColumnName(rule.getColumnName());
        preview.setStandardValue(rule.getStandardValue());
        preview.setAffectedCount(affectedCount);
        preview.setSamples(samples.stream().limit(10).toList());
        
        return preview;
    }

    /**
     * 回滚清洗
     */
    @Transactional
    public int rollbackClean(Long taskId, String executedBy) {
        CleaningTask task = cleaningTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
        
        log.info("开始回滚清洗任务: {}", taskId);
        
        // 查询该任务的所有清洗日志
        List<CleaningLog> logs = cleaningLogRepository.findByRuleId(task.getId());
        
        int rolledBackCount = 0;
        
        for (CleaningLog cleaningLog : logs) {
            try {
                // 恢复原值
                String updateSql = String.format(
                    "UPDATE %s SET %s = ? WHERE id = ?",
                    cleaningLog.getTableName(),
                    cleaningLog.getColumnName()
                );
                
                jdbcTemplate.update(updateSql, cleaningLog.getOldValue(), cleaningLog.getRecordId());
                rolledBackCount++;
                
            } catch (Exception e) {
                log.error("回滚记录失败: id={}", cleaningLog.getRecordId(), e);
            }
        }
        
        log.info("回滚完成: {}/{}", rolledBackCount, logs.size());
        
        return rolledBackCount;
    }

    /**
     * 创建清洗任务
     */
    private CleaningTask createTask(CleaningRule rule, String executedBy) {
        CleaningTask task = new CleaningTask();
        task.setTaskName(String.format("清洗 %s.%s", rule.getTableName(), rule.getColumnName()));
        task.setTableName(rule.getTableName());
        task.setColumnName(rule.getColumnName());
        task.setStatus("pending");
        task.setCreatedBy(executedBy);
        
        return cleaningTaskRepository.save(task);
    }

    /**
     * 清洗结果
     */
    @Data
    public static class CleaningResult {
        private Long taskId;
        private Integer totalRecords;
        private Integer cleanedRecords;
        private Boolean success;
        private String errorMessage;
    }

    /**
     * 清洗预览
     */
    @Data
    public static class CleaningPreview {
        private Long ruleId;
        private String tableName;
        private String columnName;
        private String standardValue;
        private Integer affectedCount;
        private List<DirtyDataDetector.DirtyDataRecord> samples;
    }
}
