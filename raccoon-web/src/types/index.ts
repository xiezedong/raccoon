/**
 * 清洗规则
 */
export interface CleaningRule {
  id?: number
  tableName: string
  columnName: string
  columnDescription?: string
  standardValue: string
  dirtyValues: string[]
  confidence: number
  source: string
  autoApply: boolean
  usageCount?: number
  successCount?: number
  lastUsedAt?: string
  createdAt?: string
  updatedAt?: string
  createdBy?: string
  updatedBy?: string
}

/**
 * 候选规则
 */
export interface CandidateRule {
  id?: number
  tableName: string
  columnName: string
  columnDescription?: string
  standardValue: string
  dirtyValues: string[]
  reason?: string
  confidence: number
  source: string
  status: 'pending' | 'approved' | 'rejected'
  isDuplicate?: boolean
  createdAt?: string
  reviewedAt?: string
  reviewedBy?: string
}

/**
 * AI 发现结果
 */
export interface DiscoveryResult {
  tableName: string
  columnName: string
  success: boolean
  message: string
  candidateCount: number
  startTime: string
  endTime: string
}

/**
 * 脏数据记录
 */
export interface DirtyDataRecord {
  id: number
  tableName: string
  columnName: string
  dirtyValue: string
  standardValue: string
  ruleId: number
}

/**
 * 清洗预览
 */
export interface CleaningPreview {
  ruleId: number
  tableName: string
  columnName: string
  standardValue: string
  affectedCount: number
  samples: DirtyDataRecord[]
}

/**
 * 清洗结果
 */
export interface CleaningResult {
  taskId: number
  totalRecords: number
  cleanedRecords: number
  success: boolean
  needConfirm?: boolean
  errorMessage?: string
}

/**
 * 批量清洗结果
 */
export interface BatchCleaningResult {
  totalRules: number
  successCount: number
  failedCount: number
  totalRecords: number
  totalCleaned: number
  results: CleaningResult[]
  startTime: string
  endTime: string
}

/**
 * 清洗任务
 */
export interface CleaningTask {
  id: number
  taskName: string
  tableName: string
  columnName: string
  totalRecords: number
  cleanedRecords: number
  status: 'pending' | 'pending_confirm' | 'running' | 'completed' | 'failed' | 'rolled_back'
  errorMessage?: string
  startedAt?: string
  completedAt?: string
  createdAt: string
  createdBy: string
}

/**
 * 清洗日志
 */
export interface CleaningLog {
  id: number
  tableName: string
  columnName: string
  oldValue: string
  newValue: string
  recordId: number
  ruleId: number
  executedAt: string
  executedBy: string
}

/**
 * 日志统计
 */
export interface LogStatistics {
  totalLogs: number
  totalTasks: number
  completedTasks: number
  failedTasks: number
  runningTasks: number
  totalCleanedRecords: number
  todayLogs: number
  weekLogs: number
}

/**
 * 值计数
 */
export interface ValueCount {
  value: string
  count: number
}

/**
 * 脏数据扫描结果
 */
export interface DirtyDataScan {
  id: number
  ruleId: number
  tableName: string
  columnName: string
  standardValue: string
  dirtyValues: string[]
  affectedCount: number
  status: 'pending' | 'cleaning' | 'completed'
  scannedAt: string
  scannedBy: string
  cleanedAt?: string
}

/**
 * 扫描结果
 */
export interface ScanResult {
  totalRules: number
  foundCount: number
  totalAffected: number
  message: string
}

/**
 * API 响应
 */
export interface ApiResponse<T = any> {
  success: boolean
  data?: T
  message?: string
  error?: string
}
