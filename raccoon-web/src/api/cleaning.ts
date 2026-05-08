import request from '@/utils/request'
import type { CleaningPreview, CleaningResult, DirtyDataRecord, ValueCount } from '@/types'

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
