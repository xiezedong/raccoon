import request from '@/utils/request'
import type { CleaningPreview, CleaningResult, DirtyDataRecord, ValueCount, DirtyDataScan, ScanResult } from '@/types'

/**
 * 预览清洗
 */
export function previewClean(ruleId: number) {
  return request.get<CleaningPreview>(`/cleaning/preview/${ruleId}`)
}

/**
 * 执行清洗
 */
export function executeClean(ruleId: number, executedBy?: string) {
  return request.post<CleaningResult>(`/cleaning/execute/${ruleId}`, null, {
    params: { executedBy }
  })
}

/**
 * 批量执行清洗
 */
export function executeBatchClean(ruleIds: number[], executedBy?: string) {
  return request.post<any>('/cleaning/execute/batch', ruleIds, {
    params: { executedBy }
  })
}

/**
 * 回滚清洗
 */
export function rollbackClean(taskId: number, executedBy?: string) {
  return request.post(`/cleaning/rollback/${taskId}`, null, {
    params: { executedBy }
  })
}

/**
 * 扫描所有表
 */
export function scanAllTables() {
  return request.get<Record<string, DirtyDataRecord[]>>('/cleaning/scan')
}

/**
 * 获取唯一值
 */
export function getUniqueValues(tableName: string, columnName: string) {
  return request.get<ValueCount[]>('/cleaning/unique-values', {
    params: { tableName, columnName }
  })
}

/**
 * 扫描所有规则的脏数据（保存到数据库）
 */
export function scanAllRules(scannedBy?: string) {
  return request.post<ScanResult>('/cleaning/scan', null, {
    params: { scannedBy }
  })
}

/**
 * 获取待处理的扫描结果
 */
export function getPendingScans() {
  return request.get<DirtyDataScan[]>('/cleaning/scans/pending')
}

/**
 * 获取最近的扫描结果
 */
export function getRecentScans() {
  return request.get<DirtyDataScan[]>('/cleaning/scans/recent')
}

/**
 * 删除扫描结果
 */
export function deleteScan(scanId: number) {
  return request.delete(`/cleaning/scans/${scanId}`)
}
