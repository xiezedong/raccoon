package com.raccoon.datacleaning.service;

import com.raccoon.datacleaning.model.CleaningRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final CleaningRuleService cleaningRuleService;

    /**
     * 导入 Excel 规则文件
     * 
     * Excel 格式：表名 | 字段名 | 字段描述 | 正确值 | 错误值 | 备注
     * 
     * @param file Excel 文件
     * @return 导入的规则数量
     */
    @Transactional
    public int importRules(MultipartFile file) throws IOException {
        log.info("开始导入 Excel 规则文件: {}", file.getOriginalFilename());
        
        List<CleaningRule> rules = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 跳过表头，从第二行开始
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                
                try {
                    CleaningRule rule = parseRow(row);
                    if (rule != null) {
                        rules.add(rule);
                    }
                } catch (Exception e) {
                    log.error("解析第 {} 行失败", i + 1, e);
                }
            }
        }
        
        // 批量保存
        if (!rules.isEmpty()) {
            cleaningRuleService.createRules(rules);
            log.info("成功导入 {} 条规则", rules.size());
        }
        
        return rules.size();
    }

    /**
     * 解析 Excel 行
     */
    private CleaningRule parseRow(Row row) {
        // 表名（第1列）
        String tableName = getCellValue(row, 0);
        if (tableName == null || tableName.trim().isEmpty()) {
            log.warn("跳过空表名的行");
            return null;
        }
        
        // 字段名（第2列）
        String columnName = getCellValue(row, 1);
        if (columnName == null || columnName.trim().isEmpty()) {
            log.warn("跳过空字段名的行");
            return null;
        }
        
        // 字段描述（第3列）
        String columnDescription = getCellValue(row, 2);
        
        // 正确值（第4列）
        String standardValue = getCellValue(row, 3);
        if (standardValue == null || standardValue.trim().isEmpty()) {
            log.warn("跳过空标准值的行");
            return null;
        }
        
        // 错误值（第5列，支持顿号、逗号分隔）
        String dirtyValuesStr = getCellValue(row, 4);
        if (dirtyValuesStr == null || dirtyValuesStr.trim().isEmpty()) {
            log.warn("跳过空错误值的行");
            return null;
        }
        
        // 解析错误值（支持多种分隔符）
        String[] dirtyValues = dirtyValuesStr.split("[、,，;；]");
        for (int i = 0; i < dirtyValues.length; i++) {
            dirtyValues[i] = dirtyValues[i].trim();
        }
        
        // 创建规则对象
        CleaningRule rule = new CleaningRule();
        rule.setTableName(tableName.trim());
        rule.setColumnName(columnName.trim());
        rule.setColumnDescription(columnDescription);
        rule.setStandardValue(standardValue.trim());
        rule.setDirtyValues(dirtyValues);
        rule.setConfidence(BigDecimal.ONE);
        rule.setSource("manual");
        rule.setAutoApply(false);
        rule.setCreatedBy("excel_import");
        
        return rule;
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    /**
     * 验证 Excel 格式
     */
    public boolean validateExcelFormat(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            if (headerRow == null) {
                return false;
            }
            
            // 检查表头
            String[] expectedHeaders = {"表名", "字段名", "字段描述", "正确值", "错误值"};
            for (int i = 0; i < expectedHeaders.length; i++) {
                String cellValue = getCellValue(headerRow, i);
                if (cellValue == null || !cellValue.contains(expectedHeaders[i])) {
                    log.error("表头格式错误，期望: {}, 实际: {}", expectedHeaders[i], cellValue);
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("验证 Excel 格式失败", e);
            return false;
        }
    }
}
