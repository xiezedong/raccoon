package com.raccoon.datacleaning.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定时 AI 发现服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledDiscoveryService {

    private final TaskScheduler taskScheduler;
    private final AIDiscoveryService aiDiscoveryService;
    private final SystemConfigService systemConfigService;
    
    private ScheduledFuture<?> currentTask;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private LocalDateTime lastExecutionTime;
    
    // 预设时间映射
    private static final Map<String, String> PRESET_TO_CRON = Map.of(
        "disabled", "",
        "daily_2am", "0 0 2 * * ?",
        "daily_3am", "0 0 3 * * ?",
        "weekly_sunday", "0 0 2 ? * SUN",
        "weekly_monday", "0 0 2 ? * MON",
        "monthly_1st", "0 0 2 1 * ?"
    );

    /**
     * Bean 初始化后自动调用
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化定时 AI 发现服务");
        reschedule();
    }

    /**
     * 监听配置变更事件
     */
    @EventListener
    public void onConfigChanged(SystemConfigService.ScheduleConfigChangedEvent event) {
        log.info("检测到定时发现配置变更，重新调度任务");
        reschedule();
    }

    /**
     * 重新调度任务
     */
    public synchronized void reschedule() {
        log.info("重新调度定时 AI 发现任务");
        
        // 取消旧任务
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(false);
            log.info("已取消旧的定时任务");
        }
        
        // 检查是否启用
        boolean enabled = Boolean.parseBoolean(
            systemConfigService.getConfig("ai_discovery.scheduled.enabled", "false")
        );
        
        if (!enabled) {
            log.info("定时 AI 发现已禁用");
            return;
        }
        
        // 获取 cron 表达式
        String cron = getCronExpression();
        
        if (cron == null || cron.isEmpty()) {
            log.warn("未配置有效的 cron 表达式");
            return;
        }
        
        try {
            // 创建新任务
            currentTask = taskScheduler.schedule(
                this::executeDiscovery,
                new CronTrigger(cron)
            );
            log.info("定时任务已启动，cron: {}", cron);
        } catch (Exception e) {
            log.error("启动定时任务失败", e);
        }
    }

    /**
     * 获取 cron 表达式
     */
    private String getCronExpression() {
        String preset = systemConfigService.getConfig("ai_discovery.scheduled.preset", "disabled");
        
        if ("custom".equals(preset)) {
            // 使用自定义 cron
            return systemConfigService.getConfig("ai_discovery.scheduled.cron", "");
        } else {
            // 使用预设
            return PRESET_TO_CRON.getOrDefault(preset, "");
        }
    }

    /**
     * 执行发现任务
     */
    private void executeDiscovery() {
        // 检查是否有任务正在执行
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("上一次 AI 发现任务还在执行中，跳过本次");
            return;
        }
        
        try {
            // 检查最小执行间隔
            if (!checkMinInterval()) {
                log.info("未达到最小执行间隔，跳过本次");
                return;
            }
            
            log.info("开始执行定时 AI 发现任务");
            lastExecutionTime = LocalDateTime.now();
            
            // 执行发现
            AIDiscoveryService.DiscoveryResult result = aiDiscoveryService.discoverAll();
            
            if (result.isSuccess()) {
                log.info("定时 AI 发现完成: {}", result.getMessage());
            } else {
                log.error("定时 AI 发现失败: {}", result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("执行定时 AI 发现任务异常", e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * 检查最小执行间隔
     */
    private boolean checkMinInterval() {
        if (lastExecutionTime == null) {
            return true;
        }
        
        int minIntervalHours = Integer.parseInt(
            systemConfigService.getConfig("ai_discovery.scheduled.min_interval_hours", "6")
        );
        
        LocalDateTime nextAllowedTime = lastExecutionTime.plusHours(minIntervalHours);
        return LocalDateTime.now().isAfter(nextAllowedTime);
    }

    /**
     * 获取任务状态
     */
    public Map<String, Object> getStatus() {
        boolean enabled = Boolean.parseBoolean(
            systemConfigService.getConfig("ai_discovery.scheduled.enabled", "false")
        );
        
        return Map.of(
            "enabled", enabled,
            "running", isRunning.get(),
            "scheduled", currentTask != null && !currentTask.isCancelled(),
            "lastExecutionTime", lastExecutionTime != null ? lastExecutionTime.toString() : "从未执行",
            "cronExpression", getCronExpression()
        );
    }

    /**
     * 手动触发一次执行
     */
    public void triggerNow() {
        log.info("手动触发 AI 发现任务");
        taskScheduler.schedule(this::executeDiscovery, java.time.Instant.now());
    }
}
