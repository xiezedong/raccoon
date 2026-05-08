package com.raccoon.datacleaning.repository;

import com.raccoon.datacleaning.model.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户反馈 Repository
 */
@Repository
public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {

    /**
     * 根据规则ID查询反馈
     */
    List<UserFeedback> findByRuleId(Long ruleId);

    /**
     * 根据操作类型查询反馈
     */
    List<UserFeedback> findByAction(String action);

    /**
     * 查询最近的反馈
     */
    List<UserFeedback> findTop100ByOrderByFeedbackTimeDesc();

    /**
     * 查询指定规则的最近反馈
     */
    @Query("SELECT f FROM UserFeedback f WHERE f.ruleId = :ruleId ORDER BY f.feedbackTime DESC")
    List<UserFeedback> findRecentByRuleId(@Param("ruleId") Long ruleId);

    /**
     * 统计指定操作类型的反馈数量
     */
    long countByAction(String action);

    /**
     * 统计指定规则的反馈数量
     */
    long countByRuleId(Long ruleId);
}
