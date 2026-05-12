package com.raccoon.datacleaning.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 脏数据扫描结果实体
 */
@Data
@Entity
@Table(name = "dirty_data_scans", indexes = {
    @Index(name = "idx_dirty_data_scans_rule_id", columnList = "rule_id"),
    @Index(name = "idx_dirty_data_scans_scanned_at", columnList = "scanned_at"),
    @Index(name = "idx_dirty_data_scans_status", columnList = "status")
})
public class DirtyDataScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "standard_value", nullable = false)
    private String standardValue;

    @Column(name = "dirty_values", columnDefinition = "TEXT[]")
    private String[] dirtyValues;

    @Column(name = "affected_count", nullable = false)
    private Integer affectedCount;

    @Column(name = "status", length = 20)
    private String status = "pending"; // pending/cleaning/completed

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    @Column(name = "scanned_by", length = 100)
    private String scannedBy;

    @Column(name = "cleaned_at")
    private LocalDateTime cleanedAt;

    @PrePersist
    protected void onCreate() {
        if (scannedAt == null) {
            scannedAt = LocalDateTime.now();
        }
    }
}
