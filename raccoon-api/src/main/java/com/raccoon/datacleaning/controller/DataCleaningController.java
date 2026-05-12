package com.raccoon.datacleaning.controller;

import com.raccoon.datacleaning.model.DirtyDataScan;
import com.raccoon.datacleaning.service.DataCleaningExecutor;
import com.raccoon.datacleaning.service.DirtyDataDetector;
import com.raccoon.datacleaning.service.DirtyDataScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据清洗 Controller
 */
@RestController
@RequestMapping("/cleaning")
@RequiredArgsConstructor
@CrossOrigin
public class DataCleaningController {

    private final DataCleaningExecutor dataCleaningExecutor;
    private final DirtyDataDetector dirtyDataDetector;
    private final DirtyDataScanService dirtyDataScanService;

    /**
     * 查询脏数据
     */
    @GetMapping("/dirty-data/{ruleId}")
    public ResponseEntity<List<DirtyDataDetector.DirtyDataRecord>> findDirtyData(
            @PathVariable Long ruleId) {
        // 需要先获取规则，这里简化处理
        return ResponseEntity.ok(List.of());
    }

    /**
     * 预览清洗影响
     */
    @GetMapping("/preview/{ruleId}")
    public ResponseEntity<DataCleaningExecutor.CleaningPreview> previewClean(
            @PathVariable Long ruleId) {
        DataCleaningExecutor.CleaningPreview preview = dataCleaningExecutor.previewClean(ruleId);
        return ResponseEntity.ok(preview);
    }

    /**
     * 执行清洗
     */
    @PostMapping("/execute/{ruleId}")
    public ResponseEntity<DataCleaningExecutor.CleaningResult> executeClean(
            @PathVariable Long ruleId,
            @RequestParam(required = false, defaultValue = "system") String executedBy) {
        DataCleaningExecutor.CleaningResult result = dataCleaningExecutor.executeClean(ruleId, executedBy);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 批量执行清洗
     */
    @PostMapping("/execute/batch")
    public ResponseEntity<DataCleaningExecutor.BatchCleaningResult> executeBatchClean(
            @RequestBody List<Long> ruleIds,
            @RequestParam(required = false, defaultValue = "system") String executedBy) {
        DataCleaningExecutor.BatchCleaningResult result = dataCleaningExecutor.executeBatchClean(ruleIds, executedBy);
        return ResponseEntity.ok(result);
    }

    /**
     * 回滚清洗
     */
    @PostMapping("/rollback/{taskId}")
    public ResponseEntity<Map<String, Object>> rollbackClean(
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

    /**
     * 扫描所有表的脏数据
     */
    @GetMapping("/scan")
    public ResponseEntity<Map<String, List<DirtyDataDetector.DirtyDataRecord>>> scanAllTables() {
        Map<String, List<DirtyDataDetector.DirtyDataRecord>> result = dirtyDataDetector.scanAllTables();
        return ResponseEntity.ok(result);
    }

    /**
     * 获取字段的唯一值
     */
    @GetMapping("/unique-values")
    public ResponseEntity<List<DirtyDataDetector.ValueCount>> getUniqueValues(
            @RequestParam String tableName,
            @RequestParam String columnName) {
        List<DirtyDataDetector.ValueCount> values = dirtyDataDetector.getUniqueValues(tableName, columnName);
        return ResponseEntity.ok(values);
    }

    /**
     * 扫描所有规则的脏数据（保存到数据库）
     */
    @PostMapping("/scan")
    public ResponseEntity<DirtyDataScanService.ScanResult> scanAllRules(
            @RequestParam(required = false, defaultValue = "system") String scannedBy) {
        DirtyDataScanService.ScanResult result = dirtyDataScanService.scanAllRules(scannedBy);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取待处理的扫描结果
     */
    @GetMapping("/scans/pending")
    public ResponseEntity<List<DirtyDataScan>> getPendingScans() {
        List<DirtyDataScan> scans = dirtyDataScanService.getPendingScans();
        return ResponseEntity.ok(scans);
    }

    /**
     * 获取最近的扫描结果
     */
    @GetMapping("/scans/recent")
    public ResponseEntity<List<DirtyDataScan>> getRecentScans() {
        List<DirtyDataScan> scans = dirtyDataScanService.getRecentScans();
        return ResponseEntity.ok(scans);
    }

    /**
     * 删除扫描结果
     */
    @DeleteMapping("/scans/{scanId}")
    public ResponseEntity<Map<String, Object>> deleteScan(@PathVariable Long scanId) {
        dirtyDataScanService.deleteScan(scanId);
        return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));
    }
    
    /**
     * 批量删除扫描结果
     */
    @DeleteMapping("/scans/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteScans(@RequestBody List<Long> scanIds) {
        int deletedCount = dirtyDataScanService.batchDeleteScans(scanIds);
        return ResponseEntity.ok(Map.of(
            "success", true, 
            "message", "批量删除成功",
            "deletedCount", deletedCount
        ));
    }
}
