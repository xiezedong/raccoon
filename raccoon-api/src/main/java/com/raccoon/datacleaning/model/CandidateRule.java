package com.raccoon.datacleaning.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 候选规则实体（AI发现的待确认规则）
 */
@Data
@Entity
@Table(name = "candidate_rules", indexes = {
    @Index(name = "idx_candidate_rules_status", columnList = "status"),
    @Index(name = "idx_candidate_rules_table_column", columnList = "table_name,column_name"),
    @Index(name = "idx_candidate_rules_confidence", columnList = "confidence")
})
public class CandidateRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "column_description", columnDefinition = "TEXT")
    private String columnDescription;

    @Column(name = "standard_value", nullable = false)
    private String standardValue;

    @Column(name = "dirty_values", nullable = false, columnDefinition = "TEXT[]")
    private String[] dirtyValues;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "confidence", nullable = false, precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "source", length = 50)
    private String source = "ai_discovery";

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
