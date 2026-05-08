package com.raccoon.datacleaning.repository;

import com.raccoon.datacleaning.model.LearningLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习日志 Repository
 */
@Repository
public interface LearningLogRepository extends JpaRepository<LearningLog, Long> {

    /**
     * 根据表名和字段名查询学习日志
     */
    List<LearningLog> findByTableNameAndColumnName(String tableName, String columnName);

    /**
     * 根据状态查询学习日志
     */
    List<LearningLog> findByStatus(String status);

    /**
     * 根据学习方法查询日志
     */
    List<LearningLog> findByLearningMethod(String learningMethod);

    /**
     * 查询指定时间范围内的学习日志
     */
    List<LearningLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询最近的学习日志
     */
    List<LearningLog> findTop50ByOrderByCreatedAtDesc();

    /**
     * 统计指定状态的学习日志数量
     */
    long countByStatus(String status);
}
