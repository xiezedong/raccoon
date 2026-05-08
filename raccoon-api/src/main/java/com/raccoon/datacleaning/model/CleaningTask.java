package com.raccoon.datacleaning.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 清洗任务实体
 */
@Data
@Entity
@Table(name = "cleaning_tasks", indexes = {
    @Index(name = "idx_cleaning_tasks_status", columnList = "status"),
    @Index(name = "idx_cleaning_tasks_created_at", columnList = "created_at")
})
public class CleaningTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "total_records")
    private Integer totalRecords = 0;

    @Column(name = "cleaned_records")
    private Integer cleanedRecords = 0;

    @Column(name = "status", length = 50)
    private String status = "pending";

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
}
