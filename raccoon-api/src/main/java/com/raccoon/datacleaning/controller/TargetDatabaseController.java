package com.raccoon.datacleaning.controller;

import com.raccoon.datacleaning.service.TargetDatabaseService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 目标数据库 Controller
 */
@RestController
@RequestMapping("/target-database")
@RequiredArgsConstructor
@CrossOrigin
public class TargetDatabaseController {

    private final TargetDatabaseService targetDatabaseService;

    /**
     * 测试数据库连接
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody ConnectionRequest request) {
        boolean success = targetDatabaseService.testConnection(
                request.getHost(),
                request.getPort(),
                request.getDatabase(),
                request.getUsername(),
                request.getPassword()
        );
        
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "连接成功" : "连接失败"
        ));
    }

    /**
     * 获取数据库列表
     */
    @PostMapping("/databases")
    public ResponseEntity<List<String>> getDatabaseList(@RequestBody ConnectionRequest request) {
        List<String> databases = targetDatabaseService.getDatabaseList(
                request.getHost(),
                request.getPort(),
                request.getUsername(),
                request.getPassword()
        );
        
        return ResponseEntity.ok(databases);
    }

    /**
     * 连接请求
     */
    @Data
    public static class ConnectionRequest {
        private String host;
        private String port;
        private String database;
        private String username;
        private String password;
    }
}
