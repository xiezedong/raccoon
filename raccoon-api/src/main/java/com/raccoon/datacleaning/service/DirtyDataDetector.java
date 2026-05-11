package com.raccoon.datacleaning.service;

import com.raccoon.datacleaning.model.CleaningRule;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 脏数据检测服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirtyDataDetector {

    private final JdbcTemplate jdbcTemplate;
    private final CleaningRuleService cleaningRuleService;
    private final TargetDatabaseService targetDatabaseService;

    /**
     * 根据规则查询脏数据（从目标数据库）
     */
    public List<DirtyDataRecord> findDirtyData(CleaningRule rule) {
        log.info("查询脏数据: {}.{}", rule.getTableName(), rule.getColumnName());
        
        List<DirtyDataRecord> records = new ArrayList<>();
        
        try (Connection conn = targetDatabaseService.getTargetConnection()) {
            String sql = buildQuerySql(rule);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // 设置参数
                for (int i = 0; i < rule.getDirtyValues().length; i++) {
                    pstmt.setString(i + 1, rule.getDirtyValues()[i]);
                }
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        DirtyDataRecord record = new DirtyDataRecord();
                        record.setId(rs.getLong("id"));
                        record.setDirtyValue(rs.getString("dirty_value"));
                        record.setTableName(rule.getTableName());
                        record.setColumnName(rule.getColumnName());
                        record.setStandardValue(rule.getStandardValue());
                        record.setRuleId(rule.getId());
                        records.add(record);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("查询脏数据失败", e);
            throw new RuntimeException("查询脏数据失败: " + e.getMessage(), e);
        }
        
        return records;
    }

    /**
     * 统计脏数据数量（从目标数据库）
     */
    public int countDirtyData(CleaningRule rule) {
        try (Connection conn = targetDatabaseService.getTargetConnection()) {
            String sql = buildCountSql(rule);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // 设置参数
                for (int i = 0; i < rule.getDirtyValues().length; i++) {
                    pstmt.setString(i + 1, rule.getDirtyValues()[i]);
                }
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            log.error("统计脏数据失败", e);
        }
        
        return 0;
    }

    /**
     * 批量检测多个表的脏数据
     */
    public Map<String, List<DirtyDataRecord>> scanAllTables() {
        List<CleaningRule> rules = cleaningRuleService.getAllRules();
        
        return rules.stream()
                .collect(Collectors.groupingBy(
                        rule -> rule.getTableName() + "." + rule.getColumnName(),
                        Collectors.flatMapping(
                                rule -> findDirtyData(rule).stream(),
                                Collectors.toList()
                        )
                ));
    }

    /**
     * 获取字段的所有唯一值（从目标数据库）
     */
    public List<ValueCount> getUniqueValues(String tableName, String columnName) {
        String sql = String.format("""
            SELECT %s as value, COUNT(*) as cnt
            FROM %s
            WHERE %s IS NOT NULL
            GROUP BY %s
            ORDER BY cnt DESC
            """,
            columnName, tableName, columnName, columnName
        );
        
        List<ValueCount> results = new ArrayList<>();
        
        try (Connection conn = targetDatabaseService.getTargetConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                ValueCount vc = new ValueCount();
                vc.setValue(rs.getString("value"));
                vc.setCount(rs.getInt("cnt"));
                results.add(vc);
            }
            
            log.info("获取到 {} 个唯一值", results.size());
            
        } catch (Exception e) {
            log.error("获取唯一值失败", e);
            throw new RuntimeException("获取唯一值失败: " + e.getMessage(), e);
        }
        
        return results;
    }

    /**
     * 构建查询 SQL
     */
    private String buildQuerySql(CleaningRule rule) {
        // 构建占位符
        String placeholders = String.join(",", "?".repeat(rule.getDirtyValues().length).split(""));
        
        return String.format("""
            SELECT id, %s as dirty_value
            FROM %s
            WHERE %s = ANY(ARRAY[%s]::text[])
            LIMIT 1000
            """,
            rule.getColumnName(),
            rule.getTableName(),
            rule.getColumnName(),
            placeholders
        );
    }

    /**
     * 构建统计 SQL
     */
    private String buildCountSql(CleaningRule rule) {
        String placeholders = String.join(",", "?".repeat(rule.getDirtyValues().length).split(""));
        
        return String.format("""
            SELECT COUNT(*)
            FROM %s
            WHERE %s = ANY(ARRAY[%s]::text[])
            """,
            rule.getTableName(),
            rule.getColumnName(),
            placeholders
        );
    }

    /**
     * 脏数据记录
     */
    @Data
    public static class DirtyDataRecord {
        private Long id;
        private String tableName;
        private String columnName;
        private String dirtyValue;
        private String standardValue;
        private Long ruleId;
    }

    /**
     * 值计数
     */
    @Data
    public static class ValueCount {
        private String value;
        private Integer count;
    }
}
