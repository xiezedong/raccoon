package com.raccoon.datacleaning.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字段元数据实体
 */
@Data
@Entity
@Table(name = "column_metadata", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"table_name", "column_name"}),
    indexes = {
        @Index(name = "idx_column_metadata_monitor", columnList = "monitor_enabled"),
        @Index(name = "idx_column_metadata_table", columnList = "table_name")
    }
)
public class ColumnMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "data_type", length = 50)
    private String dataType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "value_pattern", length = 100)
    private String valuePattern;

    @Column(name = "cardinality")
    private Integer cardinality;

    @Column(name = "sample_values", columnDefinition = "TEXT[]")
    private String[] sampleValues;

    @Column(name = "monitor_enabled")
    private Boolean monitorEnabled = true;

    @Column(name = "last_analyzed")
    private LocalDateTime lastAnalyzed;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
