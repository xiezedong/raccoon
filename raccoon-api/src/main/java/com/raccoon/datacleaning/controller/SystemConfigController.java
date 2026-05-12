package com.raccoon.datacleaning.controller;

import com.raccoon.datacleaning.model.SystemConfig;
import com.raccoon.datacleaning.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置 Controller
 */
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
@CrossOrigin
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 获取所有配置
     */
    @GetMapping
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        List<SystemConfig> configs = systemConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * 获取配置（Map格式）
     */
    @GetMapping("/map")
    public ResponseEntity<Map<String, String>> getConfigMap() {
        Map<String, String> configMap = systemConfigService.getConfigMap();
        return ResponseEntity.ok(configMap);
    }

    /**
     * 获取单个配置
     */
    @GetMapping("/{key}")
    public ResponseEntity<String> getConfig(@PathVariable String key) {
        String value = systemConfigService.getConfig(key);
        return ResponseEntity.ok(value);
    }

    /**
     * 更新单个配置
     */
    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> request,
            @RequestParam(required = false, defaultValue = "system") String updatedBy) {
        
        String value = request.get("value");
        systemConfigService.updateConfig(key, value, updatedBy);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "配置更新成功"
        ));
    }

    /**
     * 批量更新配置
     */
    @PutMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchUpdateConfigs(
            @RequestBody Map<String, String> configs,
            @RequestParam(required = false, defaultValue = "system") String updatedBy) {
        
        systemConfigService.batchUpdateConfigs(configs, updatedBy);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "配置批量更新成功",
                "count", configs.size()
        ));
    }

    /**
     * 初始化默认配置
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initDefaultConfigs() {
        systemConfigService.initDefaultConfigs();
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "默认配置初始化成功"
        ));
    }
    
    /**
     * 获取默认提示词模板
     */
    @GetMapping("/default-prompt")
    public ResponseEntity<Map<String, String>> getDefaultPrompt() {
        String defaultPrompt = getDefaultPromptTemplate();
        
        return ResponseEntity.ok(Map.of(
                "template", defaultPrompt,
                "placeholders", "{{tableName}}, {{columnName}}, {{columnDescription}}, {{existingRules}}, {{values}}"
        ));
    }
    
    /**
     * 重置提示词为默认值
     */
    @PostMapping("/reset-prompt")
    public ResponseEntity<Map<String, Object>> resetPrompt(
            @RequestParam(required = false, defaultValue = "system") String updatedBy) {
        
        // 清空自定义提示词，使用默认提示词
        systemConfigService.updateConfig("ai_discovery.prompt_template", "", updatedBy);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "提示词已重置为默认值"
        ));
    }
    
    /**
     * 获取默认提示词模板
     */
    private String getDefaultPromptTemplate() {
        return """
# 任务
你是一个数据质量专家。分析以下数据库字段的值，识别出可能的脏数据（变体、简称、错别字、不规范写法等），并给出标准值。

# 字段信息
- 表名: {{tableName}}
- 字段名: {{columnName}}
- 字段描述: {{columnDescription}}

# 已有清洗规则（作为参考）
{{existingRules}}

# 字段当前的唯一值（值 | 出现次数）
{{values}}

# 分析要求
1. 仔细分析上述值，找出可能的脏数据模式
2. 脏数据包括：同一概念的不同写法、缩写、错别字、多余空格/符号、不规范格式等
3. 如果所有值都是规范的、没有明显的脏数据，则返回空数组 []
4. 不要为了完成任务而强行创建规则
5. 只有在确实发现脏数据时才输出规则

# 输出格式
以 JSON 数组格式输出发现的清洗规则，每条规则包含：
- standardValue: 标准值（从上述值中选择最规范的）
- dirtyValues: 错误值数组（需要清洗的值，不包含标准值本身）
- reason: 判断理由（说明为什么这些是脏数据）
- confidence: 置信度（0.0-1.0）

# 重要约束
1. 只输出 JSON 数组，不要其他文字
2. 不要重复已有规则
3. 置信度低于 0.7 的不要输出
4. 标准值必须是上述值中的一个
5. dirtyValues 数组中不能包含 standardValue
6. 如果没有发现脏数据，返回空数组 []
7. 理由中不能出现"无需清洗"、"数据规范"等表示不需要清洗的词语

# 输出示例
发现脏数据时：
[
  {
    "standardValue": "高级工程师",
    "dirtyValues": ["高工", "高级工程师（外聘）"],
    "reason": "'高工'是'高级工程师'的缩写，带括号的是非规范写法",
    "confidence": 0.95
  }
]

没有发现脏数据时：
[]
""";
    }
}
