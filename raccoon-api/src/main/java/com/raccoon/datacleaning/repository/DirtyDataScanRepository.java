package com.raccoon.datacleaning.repository;

import com.raccoon.datacleaning.model.DirtyDataScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 脏数据扫描结果 Repository
 */
@Repository
public interface DirtyDataScanRepository extends JpaRepository<DirtyDataScan, Long> {

    /**
     * 查询待处理的扫描结果
     */
    List<DirtyDataScan> findByStatusOrderByAffectedCountDesc(String status);

    /**
     * 根据规则ID查询最新的扫描结果
     */
    Optional<DirtyDataScan> findFirstByRuleIdOrderByScannedAtDesc(Long ruleId);

    /**
     * 查询最近的扫描结果
     */
    List<DirtyDataScan> findTop100ByOrderByScannedAtDesc();

    /**
     * 删除指定时间之前的扫描结果
     */
    @Modifying
    @Query("DELETE FROM DirtyDataScan d WHERE d.scannedAt < :beforeTime AND d.status = 'completed'")
    void deleteOldScans(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计待处理的扫描结果数量
     */
    long countByStatus(String status);

    /**
     * 更新扫描结果状态
     */
    @Modifying
    @Query("UPDATE DirtyDataScan d SET d.status = :status, d.cleanedAt = :cleanedAt WHERE d.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status, @Param("cleanedAt") LocalDateTime cleanedAt);
}
