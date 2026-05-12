package com.raccoon.datacleaning.controller;

import com.raccoon.datacleaning.service.DataCleaningExecutor;
import com.raccoon.datacleaning.service.DirtyDataDetector;
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
        int count = dataCleaningExecutor.rollbackClean(taskId, executedBy);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "rolledBackCount", count,
            "message", "成功回滚 " + count + " 条记录"
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
}
