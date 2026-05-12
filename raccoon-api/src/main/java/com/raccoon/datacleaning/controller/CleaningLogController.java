package com.raccoon.datacleaning.controller;

import com.raccoon.datacleaning.model.CleaningLog;
import com.raccoon.datacleaning.model.CleaningTask;
import com.raccoon.datacleaning.service.CleaningLogService;
import com.raccoon.datacleaning.service.DataCleaningExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 清洗日志 Controller
 */
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@CrossOrigin
public class CleaningLogController {

    private final CleaningLogService cleaningLogService;
    private final DataCleaningExecutor dataCleaningExecutor;

    /**
     * 获取所有日志（分页）
     */
    @GetMapping
    public ResponseEntity<Page<CleaningLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<CleaningLog> logs = cleaningLogService.getLogs(page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * 获取最近的日志
     */
    @GetMapping("/recent")
    public ResponseEntity<List<CleaningLog>> getRecentLogs(
            @RequestParam(defaultValue = "100") int limit) {
        List<CleaningLog> logs = cleaningLogService.getRecentLogs(limit);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据规则ID查询日志
     */
    @GetMapping("/rule/{ruleId}")
    public ResponseEntity<List<CleaningLog>> getLogsByRuleId(@PathVariable Long ruleId) {
        List<CleaningLog> logs = cleaningLogService.getLogsByRuleId(ruleId);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据表名和字段名查询日志
     */
    @GetMapping("/table/{tableName}/column/{columnName}")
    public ResponseEntity<List<CleaningLog>> getLogsByTableAndColumn(
            @PathVariable String tableName,
            @PathVariable String columnName) {
        List<CleaningLog> logs = cleaningLogService.getLogsByTableAndColumn(tableName, columnName);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据时间范围查询日志
     */
    @GetMapping("/time-range")
    public ResponseEntity<List<CleaningLog>> getLogsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<CleaningLog> logs = cleaningLogService.getLogsByTimeRange(startTime, endTime);
        return ResponseEntity.ok(logs);
    }

    /**
     * 获取所有任务
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<CleaningTask>> getAllTasks() {
        List<CleaningTask> tasks = cleaningLogService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<CleaningTask> getTaskById(@PathVariable Long taskId) {
        CleaningTask task = cleaningLogService.getTaskById(taskId);
        return ResponseEntity.ok(task);
    }

    /**
     * 获取任务的日志
     */
    @GetMapping("/tasks/{taskId}/logs")
    public ResponseEntity<List<CleaningLog>> getLogsByTaskId(@PathVariable Long taskId) {
        List<CleaningLog> logs = cleaningLogService.getLogsByTaskId(taskId);
        return ResponseEntity.ok(logs);
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<CleaningLogService.LogStatistics> getStatistics() {
        CleaningLogService.LogStatistics stats = cleaningLogService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 按表统计
     */
    @GetMapping("/statistics/by-table")
    public ResponseEntity<Map<String, Long>> getStatsByTable() {
        Map<String, Long> stats = cleaningLogService.getStatsByTable();
        return ResponseEntity.ok(stats);
    }

    /**
     * 回滚任务
     */
    @PostMapping("/tasks/{taskId}/rollback")
    public ResponseEntity<Map<String, Object>> rollbackTask(
            @PathVariable Long taskId,
            @RequestParam(required = false, defaultValue = "system") String executedBy) {
        DataCleaningExecutor.RollbackResult result = dataCleaningExecutor.rollbackClean(taskId, executedBy);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "successCount", result.getSuccessCount(),
            "failedCount", result.getFailedCount(),
            "message", result.getMessage()
        ));
    }
}
