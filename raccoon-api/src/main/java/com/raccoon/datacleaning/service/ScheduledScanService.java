package com.raccoon.datacleaning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时扫描服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledScanService {

    private final DirtyDataScanService dirtyDataScanService;
    private final SystemConfigService systemConfigService;
    private final TaskScheduler taskScheduler;
    
    private ScheduledFuture<?> scheduledTask;
    private LocalDateTime lastExecutionTime;

    @PostConstruct
    public void init() {
        // 启动时检查配置并启动定时任务
        rescheduleTask();
    }

    /**
     * 监听配置变更事件
     */
    @EventListener
    public void onConfigChanged(SystemConfigService.ScheduleConfigChangedEvent event) {
        log.info("检测到定时扫描配置变更，重新调度任务");
        rescheduleTask();
    }

    /**
     * 重新调度任务
     */
    public synchronized void rescheduleTask() {
        // 取消现有任务
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
            log.info("已取消现有的定时扫描任务");
        }

        // 检查是否启用
        boolean enabled = systemConfigService.getBooleanConfig("scan.scheduled.enabled", false);
        
        if (!enabled) {
            log.info("定时扫描未启用");
            return;
        }

        // 获取 cron 表达式
        String preset = systemConfigService.getConfig("scan.scheduled.preset", "disabled");
        String cronExpression;
        
        if ("custom".equals(preset)) {
            cronExpression = systemConfigService.getConfig("scan.scheduled.cron", "0 0 2 * * ?");
        } else {
            cronExpression = getCronByPreset(preset);
        }

        if (cronExpression == null) {
            log.warn("无效的定时扫描配置: {}", preset);
            return;
        }

        try {
            // 创建新的定时任务
            scheduledTask = taskScheduler.schedule(
                this::executeScheduledScan,
                new CronTrigger(cronExpression)
            );
            log.info("定时扫描任务已启动，Cron 表达式: {}", cronExpression);
        } catch (Exception e) {
            log.error("启动定时扫描任务失败", e);
        }
    }

    /**
     * 执行定时扫描
     */
    private void executeScheduledScan() {
        try {
            // 检查最小执行间隔
            int minIntervalHours = systemConfigService.getIntConfig("scan.scheduled.min_interval_hours", 6);
            
            if (lastExecutionTime != null) {
                LocalDateTime nextAllowedTime = lastExecutionTime.plusHours(minIntervalHours);
                if (LocalDateTime.now().isBefore(nextAllowedTime)) {
                    log.info("距离上次执行不足 {} 小时，跳过本次扫描", minIntervalHours);
                    return;
                }
            }

            log.info("开始执行定时扫描任务");
            
            DirtyDataScanService.ScanResult result = dirtyDataScanService.scanAllRules("system");
            
            lastExecutionTime = LocalDateTime.now();
            
            log.info("定时扫描完成: {}", result.getMessage());
            
            // 清理旧的扫描结果
            dirtyDataScanService.cleanOldScans();
            
        } catch (Exception e) {
            log.error("定时扫描任务执行失败", e);
        }
    }

    /**
     * 根据预设获取 Cron 表达式
     */
    private String getCronByPreset(String preset) {
        return switch (preset) {
            case "daily_2am" -> "0 0 2 * * ?";           // 每天凌晨 2:00
            case "daily_3am" -> "0 0 3 * * ?";           // 每天凌晨 3:00
            case "weekly_sunday" -> "0 0 2 ? * SUN";     // 每周日凌晨 2:00
            case "weekly_monday" -> "0 0 2 ? * MON";     // 每周一凌晨 2:00
            case "monthly_1st" -> "0 0 2 1 * ?";         // 每月1号凌晨 2:00
            default -> null;
        };
    }

    /**
     * 手动触发扫描
     */
    public void triggerManualScan() {
        log.info("手动触发定时扫描");
        executeScheduledScan();
    }

    /**
     * 获取任务状态
     */
    public TaskStatus getTaskStatus() {
        boolean enabled = systemConfigService.getBooleanConfig("scan.scheduled.enabled", false);
        boolean running = scheduledTask != null && !scheduledTask.isCancelled();
        
        TaskStatus status = new TaskStatus();
        status.setEnabled(enabled);
        status.setRunning(running);
        status.setLastExecutionTime(lastExecutionTime);
        
        if (enabled) {
            String preset = systemConfigService.getConfig("scan.scheduled.preset", "disabled");
            String cron = "custom".equals(preset) 
                ? systemConfigService.getConfig("scan.scheduled.cron", "0 0 2 * * ?")
                : getCronByPreset(preset);
            status.setCronExpression(cron);
        }
        
        return status;
    }

    /**
     * 任务状态
     */
    public static class TaskStatus {
        private boolean enabled;
        private boolean running;
        private LocalDateTime lastExecutionTime;
        private String cronExpression;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public LocalDateTime getLastExecutionTime() {
            return lastExecutionTime;
        }

        public void setLastExecutionTime(LocalDateTime lastExecutionTime) {
            this.lastExecutionTime = lastExecutionTime;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public void setCronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
        }
    }
}
