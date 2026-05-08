package com.raccoon.datacleaning.repository;

import com.raccoon.datacleaning.model.CleaningRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 清洗规则 Repository
 */
@Repository
public interface CleaningRuleRepository extends JpaRepository<CleaningRule, Long> {

    /**
     * 根据表名和字段名查询规则
     */
    List<CleaningRule> findByTableNameAndColumnName(String tableName, String columnName);

    /**
     * 根据表名查询规则
     */
    List<CleaningRule> findByTableName(String tableName);

    /**
     * 根据来源查询规则
     */
    List<CleaningRule> findBySource(String source);

    /**
     * 查询自动应用的规则
     */
    List<CleaningRule> findByAutoApplyTrue();

    /**
     * 查询置信度大于指定值的规则
     */
    List<CleaningRule> findByConfidenceGreaterThanEqual(BigDecimal confidence);

    /**
     * 查询指定字段的所有标准值
     */
    @Query("SELECT DISTINCT r.standardValue FROM CleaningRule r WHERE r.tableName = :tableName AND r.columnName = :columnName")
    List<String> findStandardValuesByTableAndColumn(@Param("tableName") String tableName, @Param("columnName") String columnName);

    /**
     * 统计规则数量
     */
    @Query("SELECT COUNT(r) FROM CleaningRule r WHERE r.source = :source")
    long countBySource(@Param("source") String source);
}
