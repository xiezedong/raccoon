package com.raccoon.datacleaning.repository;

import com.raccoon.datacleaning.model.CleaningLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 清洗日志 Repository
 */
@Repository
public interface CleaningLogRepository extends JpaRepository<CleaningLog, Long> {

    /**
     * 根据表名和字段名查询日志
     */
    List<CleaningLog> findByTableNameAndColumnName(String tableName, String columnName);

    /**
     * 根据规则ID查询日志
     */
    List<CleaningLog> findByRuleId(Long ruleId);

    /**
     * 查询指定时间范围内的日志
     */
    List<CleaningLog> findByExecutedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询最近的日志
     */
    List<CleaningLog> findTop100ByOrderByExecutedAtDesc();

    /**
     * 统计指定规则的清洗记录数
     */
    @Query("SELECT COUNT(l) FROM CleaningLog l WHERE l.ruleId = :ruleId")
    long countByRuleId(@Param("ruleId") Long ruleId);

    /**
     * 统计指定时间范围内的清洗记录数
     */
    @Query("SELECT COUNT(l) FROM CleaningLog l WHERE l.executedAt BETWEEN :startTime AND :endTime")
    long countByExecutedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根据旧值查询日志（用于回滚）
     */
    List<CleaningLog> findByTableNameAndColumnNameAndNewValue(String tableName, String columnName, String newValue);
}
