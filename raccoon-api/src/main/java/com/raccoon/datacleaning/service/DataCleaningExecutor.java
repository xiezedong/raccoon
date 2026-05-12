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
    private final SystemConfigService systemConfigService;
    private final TargetDatabaseService targetDatabaseService;

    /**
     * 执行清洗（支持手动和自动模式）
     * 
     * @param ruleId 规则ID
     * @param executedBy 执行人
     * @param isAutomatic 是否自动执行（自学习触发）
     */
    @Transactional
    public CleaningResult executeClean(Long ruleId, String executedBy, boolean isAutomatic) {
        CleaningRule rule = cleaningRuleService.getRule(ruleId);
        
        log.info("开始执行清洗: {}.{} -> {}, 模式: {}", 
            rule.getTableName(), rule.getColumnName(), rule.getStandardValue(),
            isAutomatic ? "自动" : "手动");
        
        // 创建清洗任务
        CleaningTask task = createTask(rule, executedBy);
        
        try {
            // 先统计影响记录数
            int totalCount = dirtyDataDetector.countDirtyData(rule);
            
            // 安全检查
            SafetyCheckResult safetyCheck = performSafetyCheck(totalCount, isAutomatic);
            if (!safetyCheck.isAllowed()) {
                log.warn("安全检查未通过: {}", safetyCheck.getReason());
                
                CleaningResult result = new CleaningResult();
                result.setTaskId(task.getId());
                result.setSuccess(false);
                result.setNeedConfirm(true);
                result.setErrorMessage(safetyCheck.getReason());
                result.setTotalRecords(totalCount);
                
                task.setStatus("pending_confirm");
                task.setTotalRecords(totalCount);
                cleaningTaskRepository.save(task);
                
                return result;
            }
            
            task.setTotalRecords(totalCount);
            task.setStatus("running");
            task.setStartedAt(LocalDateTime.now());
            cleaningTaskRepository.save(task);
            
            // 批量执行清洗
            int cleanedCount = executeBatchClean(rule, task);
            
            // 更新任务状态
            task.setCleanedRecords(cleanedCount);
            task.setStatus("completed");
            task.setCompletedAt(LocalDateTime.now());
            cleaningTaskRepository.save(task);
            
            // 更新规则使用统计
            cleaningRuleService.updateRuleUsage(ruleId, true);
            
            log.info("清洗完成: 总数={}, 成功={}", totalCount, cleanedCount);
            
            CleaningResult result = new CleaningResult();
            result.setTaskId(task.getId());
            result.setTotalRecords(totalCount);
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
     * 执行清洗（兼容旧接口）
     */
    @Transactional
    public CleaningResult executeClean(Long ruleId, String executedBy) {
        return executeClean(ruleId, executedBy, false);
    }
    
    /**
     * 批量执行清洗
     */
    @Transactional
    public BatchCleaningResult executeBatchClean(List<Long> ruleIds, String executedBy) {
        log.info("开始批量执行清洗: {} 条规则", ruleIds.size());
        
        BatchCleaningResult batchResult = new BatchCleaningResult();
        batchResult.setTotalRules(ruleIds.size());
        batchResult.setStartTime(LocalDateTime.now());
        
        List<CleaningResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        int totalRecords = 0;
        int totalCleaned = 0;
        
        for (Long ruleId : ruleIds) {
            try {
                CleaningResult result = executeClean(ruleId, executedBy, false);
                results.add(result);
                
                if (result.getSuccess()) {
                    successCount++;
                    totalRecords += result.getTotalRecords();
                    totalCleaned += result.getCleanedRecords();
                } else {
                    failedCount++;
                }
                
            } catch (Exception e) {
                log.error("批量执行规则 {} 失败", ruleId, e);
                failedCount++;
                
                CleaningResult errorResult = new CleaningResult();
                errorResult.setSuccess(false);
                errorResult.setErrorMessage(e.getMessage());
                results.add(errorResult);
            }
        }
        
        batchResult.setSuccessCount(successCount);
        batchResult.setFailedCount(failedCount);
        batchResult.setTotalRecords(totalRecords);
        batchResult.setTotalCleaned(totalCleaned);
        batchResult.setResults(results);
        batchResult.setEndTime(LocalDateTime.now());
        
        log.info("批量清洗完成: 成功={}, 失败={}, 总记录={}, 已清洗={}", 
            successCount, failedCount, totalRecords, totalCleaned);
        
        return batchResult;
    }
    
    /**
     * 批量执行清洗（性能优化版）
     */
    private int executeBatchClean(CleaningRule rule, CleaningTask task) {
        try (var conn = targetDatabaseService.getTargetConnection()) {
            // 1. 先查询需要清洗的数据（用于记录日志）
            List<DirtyDataDetector.DirtyDataRecord> dirtyRecords = 
                dirtyDataDetector.findDirtyData(rule);
            
            if (dirtyRecords.isEmpty()) {
                log.info("没有需要清洗的数据");
                return 0;
            }
            
            // 2. 构建批量更新 SQL
            String updateSql = buildBatchUpdateSql(rule);
            
            // 3. 执行批量更新
            int updatedCount;
            try (var pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, rule.getStandardValue());
                
                // 设置 IN 子句的参数
                for (int i = 0; i < rule.getDirtyValues().length; i++) {
                    pstmt.setString(i + 2, rule.getDirtyValues()[i]);
                }
                
                updatedCount = pstmt.executeUpdate();
                log.info("批量更新完成: {} 条记录", updatedCount);
            }
            
            // 4. 记录清洗日志（关联 taskId）
            List<CleaningLog> logs = new ArrayList<>();
            for (DirtyDataDetector.DirtyDataRecord record : dirtyRecords) {
                CleaningLog cleaningLog = new CleaningLog();
                cleaningLog.setTableName(rule.getTableName());
                cleaningLog.setColumnName(rule.getColumnName());
                cleaningLog.setOldValue(record.getDirtyValue());
                cleaningLog.setNewValue(rule.getStandardValue());
                cleaningLog.setRecordId(record.getId());
                cleaningLog.setRuleId(rule.getId());
                cleaningLog.setTaskId(task.getId());  // 关联任务ID
                cleaningLog.setExecutedBy(task.getCreatedBy());
                logs.add(cleaningLog);
            }
            
            if (!logs.isEmpty()) {
                cleaningLogRepository.saveAll(logs);
            }
            
            return updatedCount;
            
        } catch (Exception e) {
            log.error("批量清洗失败", e);
            throw new RuntimeException("批量清洗失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建批量更新 SQL
     */
    private String buildBatchUpdateSql(CleaningRule rule) {
        // 构建占位符 (?, ?, ?)
        String placeholders = String.join(",", 
            java.util.Collections.nCopies(rule.getDirtyValues().length, "?"));
        
        return String.format(
            "UPDATE %s SET %s = ? WHERE %s IN (%s)",
            rule.getTableName(),
            rule.getColumnName(),
            rule.getColumnName(),
            placeholders
        );
    }
    
    /**
     * 安全检查
     */
    private SafetyCheckResult performSafetyCheck(int affectedCount, boolean isAutomatic) {
        SafetyCheckResult result = new SafetyCheckResult();
        
        if (isAutomatic) {
            // 自动清洗：检查是否超过最大记录数限制
            int maxAutoClean = systemConfigService.getIntConfig(
                "safety.max_auto_clean_records", 1000);
            
            if (affectedCount > maxAutoClean) {
                result.setAllowed(false);
                result.setReason(String.format(
                    "影响记录数 %d 超过自动清洗限制 %d，需要人工确认",
                    affectedCount, maxAutoClean));
                return result;
            }
        } else {
            // 手动清洗：检查是否需要二次确认
            int confirmThreshold = systemConfigService.getIntConfig(
                "safety.manual_confirm_threshold", 100);
            
            if (affectedCount > confirmThreshold) {
                result.setAllowed(false);
                result.setNeedConfirm(true);
                result.setReason(String.format(
                    "影响记录数 %d 超过确认阈值 %d，请确认后再执行",
                    affectedCount, confirmThreshold));
                return result;
            }
        }
        
        result.setAllowed(true);
        return result;
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
    public RollbackResult rollbackClean(Long taskId, String executedBy) {
        CleaningTask task = cleaningTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
        
        // 检查任务状态
        if (!"completed".equals(task.getStatus())) {
            throw new RuntimeException("只能回滚已完成的任务，当前状态: " + task.getStatus());
        }
        
        log.info("开始回滚清洗任务: {}, 执行人: {}", taskId, executedBy);
        
        // 直接通过 taskId 查询该任务的所有清洗日志
        List<CleaningLog> logs = cleaningLogRepository.findByTaskId(taskId);
        
        if (logs.isEmpty()) {
            log.warn("任务 {} 没有找到清洗日志", taskId);
            return new RollbackResult(0, 0, "未找到清洗日志");
        }
        
        log.info("找到 {} 条清洗日志需要回滚", logs.size());
        
        int rolledBackCount = 0;
        int failedCount = 0;
        
        try (var conn = targetDatabaseService.getTargetConnection()) {
            for (CleaningLog cleaningLog : logs) {
                try {
                    // 恢复原值
                    String updateSql = String.format(
                        "UPDATE %s SET %s = ? WHERE id = ?",
                        cleaningLog.getTableName(),
                        cleaningLog.getColumnName()
                    );
                    
                    try (var pstmt = conn.prepareStatement(updateSql)) {
                        pstmt.setString(1, cleaningLog.getOldValue());
                        pstmt.setLong(2, cleaningLog.getRecordId());
                        
                        int updated = pstmt.executeUpdate();
                        if (updated > 0) {
                            rolledBackCount++;
                        } else {
                            failedCount++;
                            log.warn("回滚记录未找到: table={}, id={}", 
                                cleaningLog.getTableName(), cleaningLog.getRecordId());
                        }
                    }
                    
                } catch (Exception e) {
                    failedCount++;
                    log.error("回滚记录失败: id={}", cleaningLog.getRecordId(), e);
                }
            }
            
            // 更新任务状态为已回滚
            task.setStatus("rolled_back");
            task.setCompletedAt(LocalDateTime.now());
            cleaningTaskRepository.save(task);
            
        } catch (Exception e) {
            log.error("回滚失败", e);
            throw new RuntimeException("回滚失败: " + e.getMessage(), e);
        }
        
        log.info("回滚完成: 成功={}, 失败={}, 总数={}", rolledBackCount, failedCount, logs.size());
        
        return new RollbackResult(rolledBackCount, failedCount, 
            String.format("成功回滚 %d 条记录，失败 %d 条", rolledBackCount, failedCount));
    }
    
    /**
     * 回滚结果
     */
    @Data
    public static class RollbackResult {
        private final int successCount;
        private final int failedCount;
        private final String message;
        
        public RollbackResult(int successCount, int failedCount, String message) {
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.message = message;
        }
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
        private Boolean needConfirm = false;  // 是否需要人工确认
        private String errorMessage;
    }
    
    /**
     * 批量清洗结果
     */
    @Data
    public static class BatchCleaningResult {
        private Integer totalRules;          // 总规则数
        private Integer successCount;        // 成功数
        private Integer failedCount;         // 失败数
        private Integer totalRecords;        // 总记录数
        private Integer totalCleaned;        // 总清洗数
        private List<CleaningResult> results; // 详细结果
        private LocalDateTime startTime;     // 开始时间
        private LocalDateTime endTime;       // 结束时间
    }
    
    /**
     * 安全检查结果
     */
    @Data
    private static class SafetyCheckResult {
        private boolean allowed = true;
        private boolean needConfirm = false;
        private String reason;
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
