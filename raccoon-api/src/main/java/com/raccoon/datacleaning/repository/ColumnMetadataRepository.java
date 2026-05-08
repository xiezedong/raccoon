package com.raccoon.datacleaning.repository;

import com.raccoon.datacleaning.model.ColumnMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 字段元数据 Repository
 */
@Repository
public interface ColumnMetadataRepository extends JpaRepository<ColumnMetadata, Long> {

    /**
     * 根据表名和字段名查询元数据
     */
    Optional<ColumnMetadata> findByTableNameAndColumnName(String tableName, String columnName);

    /**
     * 根据表名查询所有字段元数据
     */
    List<ColumnMetadata> findByTableName(String tableName);

    /**
     * 查询启用监控的字段
     */
    List<ColumnMetadata> findByMonitorEnabled(Boolean monitorEnabled);

    /**
     * 查询所有启用监控的字段
     */
    @Query("SELECT c FROM ColumnMetadata c WHERE c.monitorEnabled = true ORDER BY c.tableName, c.columnName")
    List<ColumnMetadata> findAllMonitoredColumns();

    /**
     * 统计启用监控的字段数量
     */
    long countByMonitorEnabled(Boolean monitorEnabled);
}
