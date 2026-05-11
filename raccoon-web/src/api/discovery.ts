import request from '@/utils/request'
import type { CandidateRule, DiscoveryResult } from '@/types'

/**
 * 触发 AI 发现
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
