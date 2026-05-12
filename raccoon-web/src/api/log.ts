import request from '@/utils/request'
import type { CleaningLog, CleaningTask, LogStatistics } from '@/types'

/**
 * 获取最近的日志
 */
export function getRecentLogs(limit?: number) {
  return request.get<CleaningLog[]>('/logs/recent', {
    params: { limit }
  })
}

/**
 * 获取所有任务
 */
export function getAllTasks() {
  return request.get<CleaningTask[]>('/logs/tasks')
}

/**
 * 获取任务详情
 */
export function getTaskById(taskId: number) {
  return request.get<CleaningTask>(`/logs/tasks/${taskId}`)
}

/**
 * 获取任务的日志
 */
export function getLogsByTaskId(taskId: number) {
  return request.get<CleaningLog[]>(`/logs/tasks/${taskId}/logs`)
}

/**
 * 获取统计信息
 */
export function getStatistics() {
  return request.get<LogStatistics>('/logs/statistics')
}

/**
 * 按表统计
 */
export function getStatsByTable() {
  return request.get<Record<string, number>>('/logs/statistics/by-table')
}

/**
 * 回滚任务
 */
export function rollbackTask(taskId: number, executedBy?: string) {
  return request.post(`/logs/tasks/${taskId}/rollback`, null, {
    params: { executedBy }
  })
}

/**
 * 根据规则ID查询日志
 */
export function getLogsByRuleId(ruleId: number) {
  return request.get<CleaningLog[]>(`/logs/rule/${ruleId}`)
}
