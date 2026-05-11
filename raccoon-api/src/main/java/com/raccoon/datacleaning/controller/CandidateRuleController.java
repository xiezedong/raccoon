package com.raccoon.datacleaning.controller;

import com.raccoon.datacleaning.model.CandidateRule;
import com.raccoon.datacleaning.model.CleaningRule;
import com.raccoon.datacleaning.repository.CandidateRuleRepository;
import com.raccoon.datacleaning.service.AIDiscoveryService;
import com.raccoon.datacleaning.service.CleaningRuleService;
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
    private final AIDiscoveryService aiDiscoveryService;
    private final CleaningRuleService cleaningRuleService;

    /**
     * 全自动 AI 发现（扫描所有表和字段）
     */
    @PostMapping("/discover-all")
    public ResponseEntity<AIDiscoveryService.DiscoveryResult> discoverAll() {
        AIDiscoveryService.DiscoveryResult result = aiDiscoveryService.discoverAll();
        return ResponseEntity.ok(result);
    }

    /**
     * 触发 AI 发现（指定表和字段）
     */
    @PostMapping("/discover")
    public ResponseEntity<AIDiscoveryService.DiscoveryResult> discover(@RequestBody DiscoveryRequest request) {
        AIDiscoveryService.DiscoveryResult result = aiDiscoveryService.discover(
            request.getTableName(),
            request.getColumnName(),
            request.getColumnDescription()
        );
        return ResponseEntity.ok(result);
    }

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
            CandidateRule candidate = candidateRuleRepository.findById(id).orElse(null);
            if (candidate != null) {
                // 通过审核，转为正式规则
                CleaningRule rule = new CleaningRule();
                rule.setTableName(candidate.getTableName());
                rule.setColumnName(candidate.getColumnName());
                rule.setColumnDescription(candidate.getColumnDescription());
                rule.setStandardValue(candidate.getStandardValue());
                rule.setDirtyValues(candidate.getDirtyValues());
                rule.setSource("ai_discovery");
                rule.setConfidence(candidate.getConfidence());
                rule.setAutoApply(false); // 默认不自动应用
                rule.setCreatedBy(request.getReviewedBy());
                CleaningRule savedRule = cleaningRuleService.createRule(rule);
                
                // 更新候选规则状态，并记录关联的正式规则ID
                candidate.setRuleId(savedRule.getId());
                candidate.setStatus("approved");
                candidate.setReviewedAt(LocalDateTime.now());
                candidate.setReviewedBy(request.getReviewedBy());
                candidateRuleRepository.save(candidate);
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
     * 发现请求
     */
    public static class DiscoveryRequest {
        private String tableName;
        private String columnName;
        private String columnDescription;

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public String getColumnDescription() {
            return columnDescription;
        }

        public void setColumnDescription(String columnDescription) {
            this.columnDescription = columnDescription;
        }
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
