package com.raccoon.datacleaning.service;

import com.raccoon.datacleaning.model.CleaningRule;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

    /**
     * 根据规则查询脏数据
     */
    public List<DirtyDataRecord> findDirtyData(CleaningRule rule) {
        log.info("查询脏数据: {}.{}", rule.getTableName(), rule.getColumnName());
        
        try {
            // 构建动态 SQL
            String sql = buildQuerySql(rule);
            
            // 执行查询
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, (Object[]) rule.getDirtyValues());
            
            // 转换结果
            return rows.stream()
                    .map(row -> {
                        DirtyDataRecord record = new DirtyDataRecord();
                        record.setId(((Number) row.get("id")).longValue());
                        record.setDirtyValue((String) row.get("dirty_value"));
                        record.setTableName(rule.getTableName());
                        record.setColumnName(rule.getColumnName());
                        record.setStandardValue(rule.getStandardValue());
                        record.setRuleId(rule.getId());
                        return record;
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("查询脏数据失败", e);
            throw new RuntimeException("查询脏数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 统计脏数据数量
     */
    public int countDirtyData(CleaningRule rule) {
        try {
            String sql = buildCountSql(rule);
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, (Object[]) rule.getDirtyValues());
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("统计脏数据失败", e);
            return 0;
        }
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
     * 获取字段的所有唯一值
     */
    public List<ValueCount> getUniqueValues(String tableName, String columnName) {
        String sql = String.format("""
            SELECT %s as value, COUNT(*) as cnt
            FROM %s
            WHERE %s IS NOT NULL
            GROUP BY %s
            HAVING COUNT(*) > 1
            ORDER BY cnt DESC
            """,
            columnName, tableName, columnName, columnName
        );
        
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            
            return rows.stream()
                    .map(row -> {
                        ValueCount vc = new ValueCount();
                        vc.setValue((String) row.get("value"));
                        vc.setCount(((Number) row.get("cnt")).intValue());
                        return vc;
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("获取唯一值失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建查询 SQL
     */
    private String buildQuerySql(CleaningRule rule) {
        // 构建 IN 子句的占位符
        String placeholders = rule.getDirtyValues().length > 0 
            ? String.join(",", "?".repeat(rule.getDirtyValues().length).split(""))
            : "?";
        
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
        String placeholders = rule.getDirtyValues().length > 0 
            ? String.join(",", "?".repeat(rule.getDirtyValues().length).split(""))
            : "?";
        
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
