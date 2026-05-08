package com.raccoon.datacleaning.repository;

import com.raccoon.datacleaning.model.CleaningTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 清洗任务 Repository
 */
@Repository
public interface CleaningTaskRepository extends JpaRepository<CleaningTask, Long> {

    /**
     * 根据状态查询任务
     */
    List<CleaningTask> findByStatus(String status);

    /**
     * 根据表名和字段名查询任务
     */
    List<CleaningTask> findByTableNameAndColumnName(String tableName, String columnName);

    /**
     * 查询最近的任务
     */
    List<CleaningTask> findTop50ByOrderByCreatedAtDesc();

    /**
     * 查询指定时间范围内的任务
     */
    List<CleaningTask> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定状态的任务数量
     */
    long countByStatus(String status);

    /**
     * 查询正在运行的任务
     */
    @Query("SELECT t FROM CleaningTask t WHERE t.status = 'running' ORDER BY t.startedAt DESC")
    List<CleaningTask> findRunningTasks();

    /**
     * 统计总清洗记录数
     */
    @Query("SELECT SUM(t.cleanedRecords) FROM CleaningTask t WHERE t.status = 'completed'")
    Long sumCleanedRecords();
}
