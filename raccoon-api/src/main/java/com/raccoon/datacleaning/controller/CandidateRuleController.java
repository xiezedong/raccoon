package com.raccoon.datacleaning.controller;

import com.raccoon.datacleaning.model.CandidateRule;
import com.raccoon.datacleaning.repository.CandidateRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 候选规则 Controller
 */
@RestController
@RequestMapping("/candidate-rules")
@RequiredArgsConstructor
@CrossOrigin
public class CandidateRuleController {

    private final CandidateRuleRepository candidateRuleRepository;

    /**
     * 查询待审核的候选规则
     */
    @GetMapping("/pending")
    public ResponseEntity<List<CandidateRule>> getPendingRules(
            @RequestParam(required = false, defaultValue = "0.0") Double minConfidence) {
        List<CandidateRule> rules = candidateRuleRepository
            .findByStatusAndConfidenceGreaterThanEqualOrderByConfidenceDesc(
                "pending", 
                BigDecimal.valueOf(minConfidence)
            );
        return ResponseEntity.ok(rules);
    }

    /**
     * 批量审核候选规则
     */
    @PostMapping("/review")
    public ResponseEntity<Map<String, Object>> reviewRules(@RequestBody ReviewRequest request) {
        int approvedCount = 0;
        int rejectedCount = 0;
        
        for (Long id : request.getApprovedIds()) {
            CandidateRule rule = candidateRuleRepository.findById(id).orElse(null);
            if (rule != null) {
                rule.setStatus("approved");
                rule.setReviewedAt(LocalDateTime.now());
                rule.setReviewedBy(request.getReviewedBy());
                candidateRuleRepository.save(rule);
                approvedCount++;
            }
        }
        
        for (Long id : request.getRejectedIds()) {
            CandidateRule rule = candidateRuleRepository.findById(id).orElse(null);
            if (rule != null) {
                rule.setStatus("rejected");
                rule.setReviewedAt(LocalDateTime.now());
                rule.setReviewedBy(request.getReviewedBy());
                candidateRuleRepository.save(rule);
                rejectedCount++;
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "approvedCount", approvedCount,
            "rejectedCount", rejectedCount
        ));
    }

    /**
     * 查询候选规则统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long pendingCount = candidateRuleRepository.countByStatus("pending");
        long approvedCount = candidateRuleRepository.countByStatus("approved");
        long rejectedCount = candidateRuleRepository.countByStatus("rejected");
        
        return ResponseEntity.ok(Map.of(
            "pending", pendingCount,
            "approved", approvedCount,
            "rejected", rejectedCount,
            "total", pendingCount + approvedCount + rejectedCount
        ));
    }

    /**
     * 审核请求
     */
    public static class ReviewRequest {
        private List<Long> approvedIds;
        private List<Long> rejectedIds;
        private String reviewedBy;

        public List<Long> getApprovedIds() {
            return approvedIds;
        }

        public void setApprovedIds(List<Long> approvedIds) {
            this.approvedIds = approvedIds;
        }

        public List<Long> getRejectedIds() {
            return rejectedIds;
        }

        public void setRejectedIds(List<Long> rejectedIds) {
            this.rejectedIds = rejectedIds;
        }

        public String getReviewedBy() {
            return reviewedBy;
        }

        public void setReviewedBy(String reviewedBy) {
            this.reviewedBy = reviewedBy;
        }
    }
}
