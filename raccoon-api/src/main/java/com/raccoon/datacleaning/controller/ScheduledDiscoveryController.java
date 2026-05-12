package com.raccoon.datacleaning.controller;

import com.raccoon.datacleaning.service.ScheduledDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 定时发现 Controller
 */
@RestController
@RequestMapping("/scheduled-discovery")
@RequiredArgsConstructor
@CrossOrigin
public class ScheduledDiscoveryController {

    private final ScheduledDiscoveryService scheduledDiscoveryService;

    /**
     * 获取定时任务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(scheduledDiscoveryService.getStatus());
    }

    /**
     * 重新调度任务（配置变更后调用）
     */
    @PostMapping("/reschedule")
    public ResponseEntity<Map<String, Object>> reschedule() {
        scheduledDiscoveryService.reschedule();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "定时任务已重新调度"
        ));
    }

    /**
     * 手动触发一次执行
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> trigger() {
        scheduledDiscoveryService.triggerNow();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "已触发执行"
        ));
    }
}
