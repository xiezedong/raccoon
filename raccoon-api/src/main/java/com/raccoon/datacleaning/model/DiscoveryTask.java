package com.raccoon.datacleaning.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 发现任务实体
 */
@Data
@Entity
@Table(name = "discovery_tasks", indexes = {
    @Index(name = "idx_discovery_tasks_status", columnList = "status"),
    @Index(name = "idx_discovery_tasks_created_at", columnList = "created_at")
})
public class DiscoveryTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_name", length = 200)
    private String taskName;

    @Column(name = "status", length = 50)
    private String status = "pending"; // pending, running, completed, failed, cancelled

    @Column(name = "total_fields")
    private Integer totalFields = 0;

    @Column(name = "processed_fields")
    private Integer processedFields = 0;

    @Column(name = "current_table", length = 100)
    private String currentTable;

    @Column(name = "current_column", length = 100)
    private String currentColumn;

    @Column(name = "candidates_found")
    private Integer candidatesFound = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 计算进度百分比
     */
    public int getProgress() {
        if (totalFields == null || totalFields == 0) {
            return 0;
        }
        return (int) ((processedFields * 100.0) / totalFields);
    }
}
