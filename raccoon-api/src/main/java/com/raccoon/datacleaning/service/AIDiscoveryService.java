package com.raccoon.datacleaning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raccoon.datacleaning.model.CandidateRule;
import com.raccoon.datacleaning.model.CleaningRule;
import com.raccoon.datacleaning.model.DiscoveryTask;
import com.raccoon.datacleaning.repository.CandidateRuleRepository;
import com.raccoon.datacleaning.repository.DiscoveryTaskRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final SystemConfigService systemConfigService;
    private final TargetDatabaseService targetDatabaseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 全自动发现 - 扫描所有表和字段（同步版本，用于兼容）
     */
    @Transactional
    public DiscoveryResult discoverAll() {
        log.info("开始全自动 AI 发现（同步）");
        
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
     * 全自动发现 - 异步版本
     */
    public Long discoverAllAsync(String createdBy) {
        // 创建任务记录
        DiscoveryTask task = new DiscoveryTask();
        task.setTaskName("全自动 AI 发现");
        task.setStatus("pending");
        task.setCreatedBy(createdBy);
        task = discoveryTaskRepository.save(task);
        
        // 异步执行
        Long taskId = task.getId();
        executeDiscoveryAsync(taskId);
        
        return taskId;
    }
    
    /**
     * 异步执行发现任务
     */
    @Async
    public void executeDiscoveryAsync(Long taskId) {
        DiscoveryTask task = discoveryTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("任务不存在: {}", taskId);
            return;
        }
        
        log.info("开始执行异步发现任务: {}", taskId);
        
        try {
            // 更新任务状态为运行中
            task.setStatus("running");
            task.setStartedAt(LocalDateTime.now());
            discoveryTaskRepository.save(task);
            
            // 1. 获取目标数据库的所有表和字段
            List<TableColumn> tableColumns = getTargetTableColumns();
            log.info("扫描到 {} 个表字段", tableColumns.size());
            
            if (tableColumns.isEmpty()) {
                task.setStatus("completed");
                task.setCompletedAt(LocalDateTime.now());
                task.setErrorMessage("目标数据库中没有找到可分析的表");
                discoveryTaskRepository.save(task);
                return;
            }
            
            task.setTotalFields(tableColumns.size());
            discoveryTaskRepository.save(task);
            
            // 2. 逐个字段进行发现
            int totalCandidates = 0;
            int processedCount = 0;
            
            for (TableColumn tc : tableColumns) {
                try {
                    // 更新当前处理的字段
                    task.setCurrentTable(tc.getTableName());
                    task.setCurrentColumn(tc.getColumnName());
                    task.setProcessedFields(processedCount);
                    discoveryTaskRepository.save(task);
                    
                    log.info("分析字段 [{}/{}]: {}.{}", 
                        processedCount + 1, tableColumns.size(), 
                        tc.getTableName(), tc.getColumnName());
                    
                    DiscoveryResult fieldResult = discover(tc.getTableName(), tc.getColumnName(), tc.getColumnComment());
                    if (fieldResult.isSuccess()) {
                        totalCandidates += fieldResult.getCandidateCount();
                    }
                    
                    processedCount++;
                    
                } catch (Exception e) {
                    log.warn("分析字段失败: {}.{}", tc.getTableName(), tc.getColumnName(), e);
                    processedCount++;
                    // 继续处理下一个字段
                }
            }
            
            // 任务完成
            task.setStatus("completed");
            task.setCompletedAt(LocalDateTime.now());
            task.setProcessedFields(processedCount);
            task.setCandidatesFound(totalCandidates);
            discoveryTaskRepository.save(task);
            
            log.info("异步发现任务完成: taskId={}, 处理字段数={}, 发现候选规则数={}", 
                taskId, processedCount, totalCandidates);
            
        } catch (Exception e) {
            log.error("异步发现任务失败: taskId={}", taskId, e);
            task.setStatus("failed");
            task.setCompletedAt(LocalDateTime.now());
            task.setErrorMessage(e.getMessage());
            discoveryTaskRepository.save(task);
        }
    }
    
    /**
     * 获取任务状态
     */
    public DiscoveryTask getTaskStatus(Long taskId) {
        return discoveryTaskRepository.findById(taskId).orElse(null);
    }
    
    /**
     * 获取最近的任务列表
     */
    public List<DiscoveryTask> getRecentTasks() {
        return discoveryTaskRepository.findTop10ByOrderByCreatedAtDesc();
    }
    
    /**
     * 取消任务
     */
    @Transactional
    public boolean cancelTask(Long taskId) {
        DiscoveryTask task = discoveryTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return false;
        }
        
        if ("pending".equals(task.getStatus()) || "running".equals(task.getStatus())) {
            task.setStatus("cancelled");
            task.setCompletedAt(LocalDateTime.now());
            discoveryTaskRepository.save(task);
            return true;
        }
        
        return false;
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
            
            // 5. 标记与已有规则重复的候选规则
            markDuplicateCandidates(allCandidates, existingRules);
            
            // 6. 保存候选规则
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
            log.info("=== 开始批量发现 ===");
            log.info("表名: {}, 字段名: {}, 值数量: {}", tableName, columnName, values.size());
            
            // 构建提示词
            String prompt = buildPrompt(tableName, columnName, columnDescription, values, existingRules);
            log.debug("提示词长度: {} 字符", prompt.length());
            
            // 调用大模型
            log.info("调用大模型分析...{}",prompt);
            String response = llmService.chat(prompt);
            log.info("大模型响应长度: {} 字符", response != null ? response.length() : 0);
            log.debug("大模型完整响应: {}", response);
            
            // 解析响应
            List<CandidateRule> candidates = parseResponse(tableName, columnName, columnDescription, response);
            log.info("解析到 {} 条候选规则", candidates.size());
            
            return candidates;
            
        } catch (Exception e) {
            log.error("批量发现失败: 表={}, 字段={}", tableName, columnName, e);
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
        
        // 从配置中读取提示词模板，如果没有则使用默认模板
        String promptTemplate = systemConfigService.getConfig("ai_discovery.prompt_template");
        
        if (promptTemplate != null && !promptTemplate.trim().isEmpty()) {
            // 使用用户配置的提示词模板
            return buildPromptFromTemplate(promptTemplate, tableName, columnName, columnDescription, values, existingRules);
        }
        
        // 使用默认提示词
        return buildDefaultPrompt(tableName, columnName, columnDescription, values, existingRules);
    }
    
    /**
     * 从模板构建提示词
     */
    private String buildPromptFromTemplate(
            String template,
            String tableName,
            String columnName,
            String columnDescription,
            List<DirtyDataDetector.ValueCount> values,
            List<CleaningRule> existingRules) {
        
        // 构建已有规则文本
        StringBuilder existingRulesText = new StringBuilder();
        if (!existingRules.isEmpty()) {
            for (CleaningRule rule : existingRules) {
                existingRulesText.append("- 标准值: ").append(rule.getStandardValue());
                existingRulesText.append(" | 错误值: ").append(String.join("、", rule.getDirtyValues())).append("\n");
            }
        }
        
        // 构建唯一值文本
        StringBuilder valuesText = new StringBuilder();
        for (DirtyDataDetector.ValueCount vc : values) {
            valuesText.append("- ").append(vc.getValue()).append(" | ").append(vc.getCount()).append("\n");
        }
        
        // 替换占位符
        String prompt = template
                .replace("{{tableName}}", tableName)
                .replace("{{columnName}}", columnName)
                .replace("{{columnDescription}}", columnDescription != null ? columnDescription : "")
                .replace("{{existingRules}}", existingRulesText.toString())
                .replace("{{values}}", valuesText.toString());
        
        return prompt;
    }
    
    /**
     * 构建默认提示词
     */
    private String buildDefaultPrompt(
            String tableName,
            String columnName,
            String columnDescription,
            List<DirtyDataDetector.ValueCount> values,
            List<CleaningRule> existingRules) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("# 任务\n");
        prompt.append("你是一个数据质量专家。分析以下数据库字段的值，识别出可能的脏数据（变体、简称、错别字、不规范写法等），并给出标准值。\n\n");
        
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
        
        prompt.append("# 分析要求\n");
        prompt.append("1. 仔细分析上述值，找出可能的脏数据模式\n");
        prompt.append("2. 脏数据包括：同一概念的不同写法、缩写、错别字、多余空格/符号、不规范格式等\n");
        prompt.append("3. 如果所有值都是规范的、没有明显的脏数据，则返回空数组 []\n");
        prompt.append("4. 不要为了完成任务而强行创建规则\n");
        prompt.append("5. 只有在确实发现脏数据时才输出规则\n\n");
        
        prompt.append("# 输出格式\n");
        prompt.append("以 JSON 数组格式输出发现的清洗规则，每条规则包含：\n");
        prompt.append("- standardValue: 标准值（从上述值中选择最规范的）\n");
        prompt.append("- dirtyValues: 错误值数组（需要清洗的值，不包含标准值本身）\n");
        prompt.append("- reason: 判断理由（说明为什么这些是脏数据）\n");
        prompt.append("- confidence: 置信度（0.0-1.0）\n\n");
        
        prompt.append("# 重要约束\n");
        prompt.append("1. 只输出 JSON 数组，不要其他文字\n");
        prompt.append("2. 不要重复已有规则\n");
        prompt.append("3. 置信度低于 0.7 的不要输出\n");
        prompt.append("4. 标准值必须是上述值中的一个\n");
        prompt.append("5. dirtyValues 数组中不能包含 standardValue\n");
        prompt.append("6. 如果没有发现脏数据，返回空数组 []\n");
        prompt.append("7. 理由中不能出现\"无需清洗\"、\"数据规范\"等表示不需要清洗的词语\n\n");
        
        prompt.append("# 输出示例\n");
        prompt.append("发现脏数据时：\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"standardValue\": \"高级工程师\",\n");
        prompt.append("    \"dirtyValues\": [\"高工\", \"高级工程师（外聘）\"],\n");
        prompt.append("    \"reason\": \"'高工'是'高级工程师'的缩写，带括号的是非规范写法\",\n");
        prompt.append("    \"confidence\": 0.95\n");
        prompt.append("  }\n");
        prompt.append("]\n\n");
        
        prompt.append("没有发现脏数据时：\n");
        prompt.append("[]\n");
        
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
            
            if (json == null || json.trim().isEmpty() || json.equals("[]")) {
                log.info("AI 未发现脏数据，返回空结果");
                return candidates;
            }
            
            JsonNode root = objectMapper.readTree(json);
            
            if (root.isArray()) {
                for (JsonNode node : root) {
                    // 验证必需字段
                    if (!node.has("standardValue") || !node.has("dirtyValues") || 
                        !node.has("reason") || !node.has("confidence")) {
                        log.warn("跳过缺少必需字段的候选规则: {}", node);
                        continue;
                    }
                    
                    String standardValue = node.get("standardValue").asText();
                    String reason = node.get("reason").asText();
                    
                    // 过滤掉"无需清洗"的规则
                    if (reason.contains("无需清洗") || reason.contains("无需清理") || 
                        reason.contains("数据规范") || reason.contains("不需要清洗") ||
                        reason.contains("已经规范") || reason.contains("格式统一")) {
                        log.info("跳过无需清洗的候选规则: {} - {}", standardValue, reason);
                        continue;
                    }
                    
                    // 解析 dirtyValues
                    JsonNode dirtyValuesNode = node.get("dirtyValues");
                    List<String> dirtyValues = new ArrayList<>();
                    if (dirtyValuesNode.isArray()) {
                        for (JsonNode valueNode : dirtyValuesNode) {
                            String dirtyValue = valueNode.asText();
                            // 确保错误值不等于标准值
                            if (!dirtyValue.equals(standardValue)) {
                                dirtyValues.add(dirtyValue);
                            }
                        }
                    }
                    
                    // 如果没有有效的错误值，跳过
                    if (dirtyValues.isEmpty()) {
                        log.info("跳过没有错误值的候选规则: {}", standardValue);
                        continue;
                    }
                    
                    CandidateRule candidate = new CandidateRule();
                    candidate.setTableName(tableName);
                    candidate.setColumnName(columnName);
                    candidate.setColumnDescription(columnDescription);
                    candidate.setStandardValue(standardValue);
                    candidate.setDirtyValues(dirtyValues.toArray(new String[0]));
                    candidate.setReason(reason);
                    candidate.setConfidence(BigDecimal.valueOf(node.get("confidence").asDouble()));
                    candidate.setSource("ai_discovery");
                    candidate.setStatus("pending");
                    candidate.setCreatedAt(LocalDateTime.now());
                    
                    candidates.add(candidate);
                    log.info("解析到候选规则: {} -> {}", standardValue, dirtyValues);
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
     * 标记与已有规则重复的候选规则
     */
    private void markDuplicateCandidates(List<CandidateRule> candidates, List<CleaningRule> existingRules) {
        if (existingRules.isEmpty()) {
            return;
        }
        
        for (CandidateRule candidate : candidates) {
            // 检查是否与已有规则的标准值和错误值重复
            for (CleaningRule existingRule : existingRules) {
                // 检查标准值是否相同
                if (candidate.getStandardValue().equals(existingRule.getStandardValue())) {
                    // 检查错误值是否有重叠
                    Set<String> candidateDirtySet = new HashSet<>(Arrays.asList(candidate.getDirtyValues()));
                    Set<String> existingDirtySet = new HashSet<>(Arrays.asList(existingRule.getDirtyValues()));
                    
                    candidateDirtySet.retainAll(existingDirtySet);
                    
                    // 如果有重叠，标记为重复
                    if (!candidateDirtySet.isEmpty()) {
                        candidate.setIsDuplicate(true);
                        log.info("候选规则与已有规则重复: {} -> {}", 
                            candidate.getStandardValue(), candidateDirtySet);
                        break;
                    }
                }
            }
        }
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
