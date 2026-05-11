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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            
            // 3. 批量处理，避免上下文过大
            int batchSize = getBatchSize();
            List<CandidateRule> allCandidates = new ArrayList<>();
            
            for (int i = 0; i < uniqueValues.size(); i += batchSize) {
                int end = Math.min(i + batchSize, uniqueValues.size());
                List<DirtyDataDetector.ValueCount> batch = uniqueValues.subList(i, end);
                
                log.info("处理批次 {}/{}", (i / batchSize + 1), (uniqueValues.size() + batchSize - 1) / batchSize);
                
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
