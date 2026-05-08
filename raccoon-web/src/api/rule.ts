import request from '@/utils/request'
import type { CleaningRule } from '@/types'

/**
 * 获取所有规则
 */
export function getRules() {
  return request.get<CleaningRule[]>('/rules')
}

/**
 * 获取规则详情
 */
export function getRule(id: number) {
  return request.get<CleaningRule>(`/rules/${id}`)
}

/**
 * 创建规则
 */
export function createRule(data: CleaningRule) {
  return request.post<CleaningRule>('/rules', data)
}

/**
 * 更新规则
 */
export function updateRule(id: number, data: CleaningRule) {
  return request.put<CleaningRule>(`/rules/${id}`, data)
}

/**
 * 删除规则
 */
export function deleteRule(id: number) {
  return request.delete(`/rules/${id}`)
}

/**
 * 搜索规则
 */
export function searchRules(tableName: string, columnName: string) {
  return request.get<CleaningRule[]>('/rules/search', {
    params: { tableName, columnName }
  })
}

/**
 * 导入 Excel 规则
 */
export function importRules(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request.post('/rules/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 添加错误值
 */
export function addDirtyValue(id: number, dirtyValue: string) {
  return request.post<CleaningRule>(`/rules/${id}/dirty-values`, { dirtyValue })
}
