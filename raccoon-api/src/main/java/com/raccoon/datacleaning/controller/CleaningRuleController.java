package com.raccoon.datacleaning.controller;

import com.raccoon.datacleaning.model.CleaningRule;
import com.raccoon.datacleaning.service.CleaningRuleService;
import com.raccoon.datacleaning.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 清洗规则 Controller
 */
@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
@CrossOrigin
public class CleaningRuleController {

    private final CleaningRuleService cleaningRuleService;
    private final ExcelImportService excelImportService;

    /**
     * 创建规则
     */
    @PostMapping
    public ResponseEntity<CleaningRule> createRule(@RequestBody CleaningRule rule) {
        CleaningRule created = cleaningRuleService.createRule(rule);
        return ResponseEntity.ok(created);
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    public ResponseEntity<CleaningRule> updateRule(
            @PathVariable Long id,
            @RequestBody CleaningRule rule) {
        CleaningRule updated = cleaningRuleService.updateRule(id, rule);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        cleaningRuleService.deleteRule(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 查询规则
     */
    @GetMapping("/{id}")
    public ResponseEntity<CleaningRule> getRule(@PathVariable Long id) {
        CleaningRule rule = cleaningRuleService.getRule(id);
        return ResponseEntity.ok(rule);
    }

    /**
     * 查询所有规则
     */
    @GetMapping
    public ResponseEntity<List<CleaningRule>> getAllRules() {
        List<CleaningRule> rules = cleaningRuleService.getAllRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * 根据表名和字段名查询规则
     */
    @GetMapping("/search")
    public ResponseEntity<List<CleaningRule>> searchRules(
            @RequestParam String tableName,
            @RequestParam String columnName) {
        List<CleaningRule> rules = cleaningRuleService.getRulesByTableAndColumn(tableName, columnName);
        return ResponseEntity.ok(rules);
    }

    /**
     * 导入 Excel 规则
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importRules(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证格式
            if (!excelImportService.validateExcelFormat(file)) {
                result.put("success", false);
                result.put("message", "Excel 格式不正确");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 导入
            int count = excelImportService.importRules(file);
            
            result.put("success", true);
            result.put("count", count);
            result.put("message", "成功导入 " + count + " 条规则");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 添加错误值到规则
     */
    @PostMapping("/{id}/dirty-values")
    public ResponseEntity<CleaningRule> addDirtyValue(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String dirtyValue = request.get("dirtyValue");
        CleaningRule updated = cleaningRuleService.addDirtyValue(id, dirtyValue);
        return ResponseEntity.ok(updated);
    }
}
