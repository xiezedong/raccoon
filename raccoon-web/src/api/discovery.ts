import request from '@/utils/request'
import type { CandidateRule, DiscoveryResult, DiscoveryTask } from '@/types'

/**
 * 全自动 AI 发现（扫描所有表和字段）
 */
export function discoverAll() {
  return request.post<DiscoveryResult>('/candidate-rules/discover-all')
}

/**
 * 启动全自动 AI 发现，任务在后台执行
 */
export function discoverAllAsync() {
  return request.post<{ success: boolean; taskId: number; message: string }>(
    '/candidate-rules/discover-all-async'
  )
}

/**
 * 查询单个发现任务的状态与进度
 */
export function getTaskStatus(taskId: number) {
  return request.get<DiscoveryTask>(`/candidate-rules/task/${taskId}`)
}

/**
 * 最近 10 条发现任务
 */
export function getRecentTasks() {
  return request.get<DiscoveryTask[]>('/candidate-rules/tasks')
}

/**
 * 取消发现任务，在下一个批次边界生效
 */
export function cancelTask(taskId: number) {
  return request.post<{ success: boolean; message: string }>(
    `/candidate-rules/task/${taskId}/cancel`
  )
}

/**
 * 触发 AI 发现（指定表和字段）
 */
export function discover(data: {
  tableName: string
  columnName: string
  columnDescription?: string
}) {
  return request.post<DiscoveryResult>('/candidate-rules/discover', data)
}

/**
 * 获取待审核的候选规则
 */
export function getPendingRules(minConfidence: number = 0.0) {
  return request.get<CandidateRule[]>('/candidate-rules/pending', {
    params: { minConfidence }
  })
}

/**
 * 批量审核候选规则
 */
export function reviewRules(data: {
  approvedIds: number[]
  rejectedIds: number[]
  reviewedBy: string
}) {
  return request.post('/candidate-rules/review', data)
}

/**
 * 获取候选规则统计
 */
export function getStats() {
  return request.get<{
    pending: number
    approved: number
    rejected: number
    total: number
  }>('/candidate-rules/stats')
}

/**
 * 获取定时任务状态
 */
export function getScheduleStatus() {
  return request.get<{
    enabled: boolean
    running: boolean
    scheduled: boolean
    lastExecutionTime: string
    cronExpression: string
  }>('/scheduled-discovery/status')
}

/**
 * 手动触发定时任务
 */
export function triggerSchedule() {
  return request.post('/scheduled-discovery/trigger')
}
