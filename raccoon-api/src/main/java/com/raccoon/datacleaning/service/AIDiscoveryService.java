package com.raccoon.datacleaning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raccoon.datacleaning.model.CandidateRule;
import com.raccoon.datacleaning.model.CleaningRule;
import com.raccoon.datacleaning.repository.CandidateRuleRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 发现服务
 * 利用大模型发现潜在的脏数据和清洗规则
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIDiscoveryService {

    private final LLMService llmService;
    private final CleaningRuleService cleaningRuleService;
    private final DirtyDataDetector dirtyDataDetector;
    private final CandidateRuleRepository candidateRuleRepository;
    private final SystemConfigService systemConfigService;
    private final TargetDatabaseService targetDatabaseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 全自动发现 - 扫描所有表和字段
     */
    @Transactional
    public DiscoveryResult discoverAll() {
        log.info("开始全自动 AI 发现");
        
        DiscoveryResult result = new DiscoveryResult();
        result.setTableName("全部表");
        result.setColumnName("全部字段");
        result.setStartTime(LocalDateTime.now());
        
        try {
            // 1. 获取目标数据库的所有表和字段
            List<TableColumn> tableColumns = getTargetTableColumns();
            log.info("扫描到 {} 个表字段", tableColumns.size());
            
            if (tableColumns.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("目标数据库中没有找到可分析的表");
                return result;
            }
            
            // 2. 逐个字段进行发现
            int totalCandidates = 0;
            for (TableColumn tc : tableColumns) {
                try {
                    log.info("分析字段: {}.{}", tc.getTableName(), tc.getColumnName());
                    DiscoveryResult fieldResult = discover(tc.getTableName(), tc.getColumnName(), tc.getColumnComment());
                    if (fieldResult.isSuccess()) {
                        totalCandidates += fieldResult.getCandidateCount();
                    }
                } catch (Exception e) {
                    log.warn("分析字段失败: {}.{}", tc.getTableName(), tc.getColumnName(), e);
                    // 继续处理下一个字段
                }
            }
            
            result.setSuccess(true);
            result.setCandidateCount(totalCandidates);
            result.setMessage(String.format("扫描完成，共分析 %d 个字段，发现 %d 条候选规则", 
                tableColumns.size(), totalCandidates));
            
        } catch (Exception e) {
            log.error("全自动 AI 发现失败", e);
            result.setSuccess(false);
            result.setMessage("发现失败: " + e.getMessage());
        } finally {
            result.setEndTime(LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 发现指定表字段的潜在脏数据
     */
    @Transactional
    public DiscoveryResult discover(String tableName, String columnName, String columnDescription) {
        log.info("开始 AI 发现: {}.{}", tableName, columnName);
        
        DiscoveryResult result = new DiscoveryResult();
        result.setTableName(tableName);
        result.setColumnName(columnName);
        result.setStartTime(LocalDateTime.now());
        
        try {
            // 1. 获取该字段的所有唯一值
            List<DirtyDataDetector.ValueCount> uniqueValues = dirtyDataDetector.getUniqueValues(tableName, columnName);
            log.info("获取到 {} 个唯一值", uniqueValues.size());
            
            if (uniqueValues.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("该字段没有数据");
                return result;
            }
            
            // 2. 获取该字段已有的清洗规则
            List<CleaningRule> existingRules = cleaningRuleService.getRulesByTableAndColumn(tableName, columnName);
            log.info("已有 {} 条清洗规则", existingRules.size());
            
            // 3. 过滤掉已有规则覆盖的值
            List<DirtyDataDetector.ValueCount> filteredValues = filterExistingRules(uniqueValues, existingRules);
            log.info("过滤后剩余 {} 个唯一值", filteredValues.size());
            
            if (filteredValues.isEmpty()) {
                result.setSuccess(true);
                result.setCandidateCount(0);
                result.setMessage("该字段的所有值都已被现有规则覆盖");
                return result;
            }
            
            // 4. 批量处理，避免上下文过大
            int batchSize = getBatchSize();
            List<CandidateRule> allCandidates = new ArrayList<>();
            
            for (int i = 0; i < filteredValues.size(); i += batchSize) {
                int end = Math.min(i + batchSize, filteredValues.size());
                List<DirtyDataDetector.ValueCount> batch = filteredValues.subList(i, end);
                
                log.info("处理批次 {}/{}", (i / batchSize + 1), (filteredValues.size() + batchSize - 1) / batchSize);
                
                List<CandidateRule> batchCandidates = discoverBatch(
                    tableName, columnName, columnDescription, 
                    batch, existingRules
                );
                
                allCandidates.addAll(batchCandidates);
            }
            
            // 4. 保存候选规则
            if (!allCandidates.isEmpty()) {
                candidateRuleRepository.saveAll(allCandidates);
                log.info("保存了 {} 条候选规则", allCandidates.size());
            }
            
            result.setSuccess(true);
            result.setCandidateCount(allCandidates.size());
            result.setMessage("发现完成");
            
        } catch (Exception e) {
            log.error("AI 发现失败", e);
            result.setSuccess(false);
            
            // 提供更友好的错误信息
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("does not exist")) {
                    result.setMessage("表或字段不存在，请检查表名和字段名是否正确");
                } else if (errorMsg.contains("connection")) {
                    result.setMessage("无法连接到目标数据库，请检查系统设置中的数据库配置");
                } else if (errorMsg.contains("authentication")) {
                    result.setMessage("数据库认证失败，请检查用户名和密码");
                } else {
                    result.setMessage("发现失败: " + errorMsg);
                }
            } else {
                result.setMessage("发现失败，请查看日志");
            }
        } finally {
            result.setEndTime(LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 批量发现
     */
    private List<CandidateRule> discoverBatch(
            String tableName, 
            String columnName,
            String columnDescription,
            List<DirtyDataDetector.ValueCount> values,
            List<CleaningRule> existingRules) {
        
        try {
            // 构建提示词
            String prompt = buildPrompt(tableName, columnName, columnDescription, values, existingRules);
            
            // 调用大模型
            String response = llmService.chat(prompt);
            log.debug("LLM 响应: {}", response);
            
            // 解析响应
            return parseResponse(tableName, columnName, columnDescription, response);
            
        } catch (Exception e) {
            log.error("批量发现失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(
            String tableName,
            String columnName,
            String columnDescription,
            List<DirtyDataDetector.ValueCount> values,
            List<CleaningRule> existingRules) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("# 任务\n");
        prompt.append("分析以下数据库字段的值，识别出可能的脏数据（变体、简称、错别字、不规范写法等），并给出标准值。\n\n");
        
        prompt.append("# 字段信息\n");
        prompt.append("- 表名: ").append(tableName).append("\n");
        prompt.append("- 字段名: ").append(columnName).append("\n");
        if (columnDescription != null && !columnDescription.isEmpty()) {
            prompt.append("- 字段描述: ").append(columnDescription).append("\n");
        }
        prompt.append("\n");
        
        // 已有规则作为参考
        if (!existingRules.isEmpty()) {
            prompt.append("# 已有清洗规则（作为参考）\n");
            for (CleaningRule rule : existingRules) {
                prompt.append("- 标准值: ").append(rule.getStandardValue());
                prompt.append(" | 错误值: ").append(String.join("、", rule.getDirtyValues())).append("\n");
            }
            prompt.append("\n");
        }
        
        // 当前字段的唯一值
        prompt.append("# 字段当前的唯一值（值 | 出现次数）\n");
        for (DirtyDataDetector.ValueCount vc : values) {
            prompt.append("- ").append(vc.getValue()).append(" | ").append(vc.getCount()).append("\n");
        }
        prompt.append("\n");
        
        prompt.append("# 输出要求\n");
        prompt.append("请以 JSON 数组格式输出发现的清洗规则，每条规则包含：\n");
        prompt.append("- standardValue: 标准值（从上述值中选择最规范的）\n");
        prompt.append("- dirtyValues: 错误值数组（需要清洗的值）\n");
        prompt.append("- reason: 判断理由\n");
        prompt.append("- confidence: 置信度（0.0-1.0）\n\n");
        
        prompt.append("注意：\n");
        prompt.append("1. 只输出 JSON，不要其他文字\n");
        prompt.append("2. 不要重复已有规则\n");
        prompt.append("3. 置信度低于 0.7 的不要输出\n");
        prompt.append("4. 标准值必须是上述值中的一个\n\n");
        
        prompt.append("输出示例：\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"standardValue\": \"高级工程师\",\n");
        prompt.append("    \"dirtyValues\": [\"高工\", \"高级工程师（外聘）\"],\n");
        prompt.append("    \"reason\": \"高工是高级工程师的缩写，带括号的是非规范写法\",\n");
        prompt.append("    \"confidence\": 0.95\n");
        prompt.append("  }\n");
        prompt.append("]\n");
        
        return prompt.toString();
    }

    /**
     * 解析大模型响应
     */
    private List<CandidateRule> parseResponse(String tableName, String columnName, String columnDescription, String response) {
        List<CandidateRule> candidates = new ArrayList<>();
        
        try {
            // 提取 JSON 部分（可能包含其他文字）
            String json = extractJson(response);
            
            JsonNode root = objectMapper.readTree(json);
            
            if (root.isArray()) {
                for (JsonNode node : root) {
                    CandidateRule candidate = new CandidateRule();
                    candidate.setTableName(tableName);
                    candidate.setColumnName(columnName);
                    candidate.setColumnDescription(columnDescription);
                    candidate.setStandardValue(node.get("standardValue").asText());
                    
                    // 解析 dirtyValues
                    JsonNode dirtyValuesNode = node.get("dirtyValues");
                    List<String> dirtyValues = new ArrayList<>();
                    if (dirtyValuesNode.isArray()) {
                        for (JsonNode valueNode : dirtyValuesNode) {
                            dirtyValues.add(valueNode.asText());
                        }
                    }
                    candidate.setDirtyValues(dirtyValues.toArray(new String[0]));
                    
                    candidate.setReason(node.get("reason").asText());
                    candidate.setConfidence(BigDecimal.valueOf(node.get("confidence").asDouble()));
                    candidate.setSource("ai_discovery");
                    candidate.setStatus("pending");
                    candidate.setCreatedAt(LocalDateTime.now());
                    
                    candidates.add(candidate);
                }
            }
            
        } catch (Exception e) {
            log.error("解析 LLM 响应失败: {}", response, e);
        }
        
        return candidates;
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJson(String response) {
        // 查找 JSON 数组的开始和结束
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        
        return response;
    }

    /**
     * 获取目标数据库的所有表和字段
     */
    private List<TableColumn> getTargetTableColumns() {
        List<TableColumn> result = new ArrayList<>();
        
        try (Connection conn = targetDatabaseService.getTargetConnection()) {
            // 查询所有用户表的文本类型字段
            String sql = """
                SELECT 
                    t.table_name,
                    c.column_name,
                    pgd.description as column_comment
                FROM information_schema.tables t
                JOIN information_schema.columns c ON t.table_name = c.table_name
                LEFT JOIN pg_catalog.pg_statio_all_tables st ON st.relname = t.table_name
                LEFT JOIN pg_catalog.pg_description pgd ON pgd.objoid = st.relid 
                    AND pgd.objsubid = c.ordinal_position
                WHERE t.table_schema = 'public'
                    AND t.table_type = 'BASE TABLE'
                    AND c.data_type IN ('character varying', 'varchar', 'text', 'character')
                ORDER BY t.table_name, c.ordinal_position
                """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                while (rs.next()) {
                    TableColumn tc = new TableColumn();
                    tc.setTableName(rs.getString("table_name"));
                    tc.setColumnName(rs.getString("column_name"));
                    tc.setColumnComment(rs.getString("column_comment"));
                    result.add(tc);
                }
            }
            
        } catch (Exception e) {
            log.error("获取表字段信息失败", e);
            throw new RuntimeException("获取表字段信息失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 过滤掉已有规则覆盖的值
     */
    private List<DirtyDataDetector.ValueCount> filterExistingRules(
            List<DirtyDataDetector.ValueCount> values,
            List<CleaningRule> existingRules) {
        
        if (existingRules.isEmpty()) {
            return values;
        }
        
        // 收集所有已知的标准值和错误值
        Set<String> knownValues = new HashSet<>();
        for (CleaningRule rule : existingRules) {
            knownValues.add(rule.getStandardValue());
            knownValues.addAll(Arrays.asList(rule.getDirtyValues()));
        }
        
        // 过滤掉已知的值
        return values.stream()
                .filter(vc -> !knownValues.contains(vc.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取批处理大小
     */
    private int getBatchSize() {
        String value = systemConfigService.getConfig("llm.batch_size", "50");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 50;
        }
    }

    /**
     * 表字段信息
     */
    @Data
    public static class TableColumn {
        private String tableName;
        private String columnName;
        private String columnComment;
    }

    /**
     * 发现结果
     */
    @Data
    public static class DiscoveryResult {
        private String tableName;
        private String columnName;
        private boolean success;
        private String message;
        private int candidateCount;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
