package com.raccoon.datacleaning.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 清洗规则实体
 */
@Data
@Entity
@Table(name = "cleaning_rules", indexes = {
    @Index(name = "idx_cleaning_rules_table_column", columnList = "table_name,column_name"),
    @Index(name = "idx_cleaning_rules_source", columnList = "source"),
    @Index(name = "idx_cleaning_rules_confidence", columnList = "confidence")
})
public class CleaningRule {

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

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence = BigDecimal.ONE;

    @Column(name = "source", length = 50)
    private String source = "manual";

    @Column(name = "auto_apply")
    private Boolean autoApply = false;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "success_count")
    private Integer successCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
