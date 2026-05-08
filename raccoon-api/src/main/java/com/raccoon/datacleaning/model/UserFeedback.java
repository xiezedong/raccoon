package com.raccoon.datacleaning.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户反馈实体
 */
@Data
@Entity
@Table(name = "user_feedbacks", indexes = {
    @Index(name = "idx_user_feedbacks_rule_id", columnList = "rule_id"),
    @Index(name = "idx_user_feedbacks_action", columnList = "action"),
    @Index(name = "idx_user_feedbacks_time", columnList = "feedback_time")
})
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "original_suggestion", columnDefinition = "TEXT")
    private String originalSuggestion;

    @Column(name = "user_modification", columnDefinition = "TEXT")
    private String userModification;

    @Column(name = "feedback_time", updatable = false)
    private LocalDateTime feedbackTime;

    @Column(name = "user_id", length = 100)
    private String userId;

    @PrePersist
    protected void onCreate() {
        feedbackTime = LocalDateTime.now();
    }
}
