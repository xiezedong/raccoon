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
}
