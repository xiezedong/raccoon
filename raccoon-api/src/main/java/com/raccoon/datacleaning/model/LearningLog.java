package com.raccoon.datacleaning.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学习日志实体
 */
@Data
@Entity
@Table(name = "learning_logs", indexes = {
    @Index(name = "idx_learning_logs_table_column", columnList = "table_name,column_name"),
    @Index(name = "idx_learning_logs_status", columnList = "status"),
    @Index(name = "idx_learning_logs_created_at", columnList = "created_at")
})
public class LearningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "discovered_standard")
    private String discoveredStandard;

    @Column(name = "discovered_variants", columnDefinition = "TEXT[]")
    private String[] discoveredVariants;

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "evidence_count")
    private Integer evidenceCount;

    @Column(name = "learning_method", length = 50)
    private String learningMethod;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
