package com.raccoon.datacleaning.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 清洗日志实体
 */
@Data
@Entity
@Table(name = "cleaning_logs", indexes = {
    @Index(name = "idx_cleaning_logs_table_column", columnList = "table_name,column_name"),
    @Index(name = "idx_cleaning_logs_executed_at", columnList = "executed_at"),
    @Index(name = "idx_cleaning_logs_rule_id", columnList = "rule_id")
})
public class CleaningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "rule_id")
    private Long ruleId;
    
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "executed_at", updatable = false)
    private LocalDateTime executedAt;

    @Column(name = "executed_by", length = 100)
    private String executedBy;

    @PrePersist
    protected void onCreate() {
        executedAt = LocalDateTime.now();
    }
}
