package com.raccoon.datacleaning.service;

import com.raccoon.datacleaning.model.CleaningLog;
import com.raccoon.datacleaning.model.CleaningTask;
import com.raccoon.datacleaning.repository.CleaningLogRepository;
import com.raccoon.datacleaning.repository.CleaningTaskRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 清洗日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleaningLogService {

    private final CleaningLogRepository cleaningLogRepository;
    private final CleaningTaskRepository cleaningTaskRepository;

    /**
     * 获取所有日志（分页）
     */
    public Page<CleaningLog> getLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executedAt"));
        return cleaningLogRepository.findAll(pageable);
    }

    /**
     * 获取最近的日志
     */
    public List<CleaningLog> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "executedAt"));
        return cleaningLogRepository.findAll(pageable).getContent();
    }

    /**
     * 根据规则ID查询日志
     */
    public List<CleaningLog> getLogsByRuleId(Long ruleId) {
        return cleaningLogRepository.findByRuleId(ruleId);
    }

    /**
     * 根据表名和字段名查询日志
     */
    public List<CleaningLog> getLogsByTableAndColumn(String tableName, String columnName) {
        return cleaningLogRepository.findByTableNameAndColumnName(tableName, columnName);
    }

    /**
     * 根据时间范围查询日志
     */
    public List<CleaningLog> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return cleaningLogRepository.findByExecutedAtBetween(startTime, endTime);
    }

    /**
     * 获取所有任务
     */
    public List<CleaningTask> getAllTasks() {
        return cleaningTaskRepository.findTop50ByOrderByCreatedAtDesc();
    }

    /**
     * 根据任务ID获取任务详情
     */
    public CleaningTask getTaskById(Long taskId) {
        return cleaningTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
    }

    /**
     * 获取任务的日志
     */
    public List<CleaningLog> getLogsByTaskId(Long taskId) {
        return cleaningLogRepository.findByTaskId(taskId);
    }

    /**
     * 获取统计信息
     */
    public LogStatistics getStatistics() {
        LogStatistics stats = new LogStatistics();
        
        // 总日志数
        stats.setTotalLogs(cleaningLogRepository.count());
        
        // 总任务数
        stats.setTotalTasks(cleaningTaskRepository.count());
        
        // 各状态任务数
        stats.setCompletedTasks(cleaningTaskRepository.countByStatus("completed"));
        stats.setFailedTasks(cleaningTaskRepository.countByStatus("failed"));
        stats.setRunningTasks(cleaningTaskRepository.countByStatus("running"));
        
        // 总清洗记录数
        Long totalCleaned = cleaningTaskRepository.sumCleanedRecords();
        stats.setTotalCleanedRecords(totalCleaned != null ? totalCleaned : 0L);
        
        // 今日统计
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now();
        stats.setTodayLogs(cleaningLogRepository.countByExecutedAtBetween(todayStart, todayEnd));
        
        // 本周统计
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        stats.setWeekLogs(cleaningLogRepository.countByExecutedAtBetween(weekStart, todayEnd));
        
        return stats;
    }

    /**
     * 按表统计
     */
    public Map<String, Long> getStatsByTable() {
        List<CleaningLog> logs = cleaningLogRepository.findAll();
        Map<String, Long> stats = new HashMap<>();
        
        for (CleaningLog log : logs) {
            String key = log.getTableName();
            stats.put(key, stats.getOrDefault(key, 0L) + 1);
        }
        
        return stats;
    }

    /**
     * 日志统计信息
     */
    @Data
    public static class LogStatistics {
        private Long totalLogs;
        private Long totalTasks;
        private Long completedTasks;
        private Long failedTasks;
        private Long runningTasks;
        private Long totalCleanedRecords;
        private Long todayLogs;
        private Long weekLogs;
    }
}
