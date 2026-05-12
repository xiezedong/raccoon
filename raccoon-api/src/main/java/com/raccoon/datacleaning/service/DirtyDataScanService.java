package com.raccoon.datacleaning.service;

import com.raccoon.datacleaning.model.CleaningRule;
import com.raccoon.datacleaning.model.DirtyDataScan;
import com.raccoon.datacleaning.repository.DirtyDataScanRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 脏数据扫描服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirtyDataScanService {

    private final DirtyDataScanRepository dirtyDataScanRepository;
    private final CleaningRuleService cleaningRuleService;
    private final DataCleaningExecutor dataCleaningExecutor;

    /**
     * 扫描所有规则的脏数据
     */
    @Transactional
    public ScanResult scanAllRules(String scannedBy) {
        log.info("开始扫描所有规则的脏数据");
        
        List<CleaningRule> rules = cleaningRuleService.getAllRules();
        
        if (rules.isEmpty()) {
            return new ScanResult(0, 0, 0, "暂无清洗规则");
        }
        
        // 清空旧的待处理扫描结果
        List<DirtyDataScan> oldScans = dirtyDataScanRepository.findByStatusOrderByAffectedCountDesc("pending");
        dirtyDataScanRepository.deleteAll(oldScans);
        
        int totalRules = rules.size();
        int foundCount = 0;
        int totalAffected = 0;
        
        List<DirtyDataScan> scans = new ArrayList<>();
        
        for (CleaningRule rule : rules) {
            try {
                // 预览该规则的影响
                DataCleaningExecutor.CleaningPreview preview = 
                    dataCleaningExecutor.previewClean(rule.getId());
                
                if (preview.getAffectedCount() > 0) {
                    // 创建扫描结果
                    DirtyDataScan scan = new DirtyDataScan();
                    scan.setRuleId(rule.getId());
                    scan.setTableName(rule.getTableName());
                    scan.setColumnName(rule.getColumnName());
                    scan.setStandardValue(rule.getStandardValue());
                    scan.setDirtyValues(rule.getDirtyValues());
                    scan.setAffectedCount(preview.getAffectedCount());
                    scan.setStatus("pending");
                    scan.setScannedBy(scannedBy);
                    
                    scans.add(scan);
                    foundCount++;
                    totalAffected += preview.getAffectedCount();
                }
                
            } catch (Exception e) {
                log.error("扫描规则 {} 失败", rule.getId(), e);
            }
        }
        
        // 批量保存扫描结果
        if (!scans.isEmpty()) {
            dirtyDataScanRepository.saveAll(scans);
        }
        
        log.info("扫描完成: 总规则={}, 发现脏数据={}, 总影响记录={}", 
            totalRules, foundCount, totalAffected);
        
        return new ScanResult(totalRules, foundCount, totalAffected, 
            String.format("扫描完成，发现 %d 条规则命中脏数据，共 %d 条记录", foundCount, totalAffected));
    }

    /**
     * 获取待处理的扫描结果
     */
    public List<DirtyDataScan> getPendingScans() {
        return dirtyDataScanRepository.findByStatusOrderByAffectedCountDesc("pending");
    }

    /**
     * 获取最近的扫描结果
     */
    public List<DirtyDataScan> getRecentScans() {
        return dirtyDataScanRepository.findTop100ByOrderByScannedAtDesc();
    }

    /**
     * 更新扫描结果状态
     */
    @Transactional
    public void updateScanStatus(Long scanId, String status) {
        dirtyDataScanRepository.updateStatus(scanId, status, LocalDateTime.now());
    }

    /**
     * 删除扫描结果
     */
    @Transactional
    public void deleteScan(Long scanId) {
        dirtyDataScanRepository.deleteById(scanId);
    }
    
    /**
     * 批量删除扫描结果
     */
    @Transactional
    public int batchDeleteScans(List<Long> scanIds) {
        if (scanIds == null || scanIds.isEmpty()) {
            return 0;
        }
        
        int deletedCount = 0;
        for (Long scanId : scanIds) {
            try {
                dirtyDataScanRepository.deleteById(scanId);
                deletedCount++;
            } catch (Exception e) {
                log.error("删除扫描结果失败: scanId={}", scanId, e);
            }
        }
        
        log.info("批量删除扫描结果: 总数={}, 成功={}", scanIds.size(), deletedCount);
        return deletedCount;
    }

    /**
     * 清理旧的扫描结果（保留最近7天）
     */
    @Transactional
    public void cleanOldScans() {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(7);
        dirtyDataScanRepository.deleteOldScans(beforeTime);
        log.info("清理了 7 天前的扫描结果");
    }

    /**
     * 扫描结果
     */
    @Data
    public static class ScanResult {
        private final int totalRules;
        private final int foundCount;
        private final int totalAffected;
        private final String message;
    }
}
