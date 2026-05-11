package com.raccoon.datacleaning.repository;

import com.raccoon.datacleaning.model.CandidateRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 候选规则 Repository
 */
@Repository
public interface CandidateRuleRepository extends JpaRepository<CandidateRule, Long> {

    /**
     * 根据状态查询候选规则
     */
    List<CandidateRule> findByStatus(String status);

    /**
     * 根据表名和字段名查询候选规则
     */
    List<CandidateRule> findByTableNameAndColumnName(String tableName, String columnName);

    /**
     * 查询待审核的候选规则，按置信度降序
     */
    List<CandidateRule> findByStatusOrderByConfidenceDesc(String status);

    /**
     * 查询置信度大于指定值的待审核规则
     */
    List<CandidateRule> findByStatusAndConfidenceGreaterThanEqualOrderByConfidenceDesc(String status, BigDecimal confidence);

    /**
     * 统计待审核的候选规则数量
     */
    long countByStatus(String status);
}
