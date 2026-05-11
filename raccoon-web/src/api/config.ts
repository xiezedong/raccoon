import request from '@/utils/request'

export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  description: string
  updatedAt: string
  updatedBy: string
}

/**
 * 获取所有配置
 */
export function getAllConfigs() {
  return request.get<SystemConfig[]>('/system/config')
}

/**
 * 获取配置（Map格式）
 */
export function getConfigMap() {
  return request.get<Record<string, string>>('/system/config/map')
}

/**
 * 更新单个配置
 */
export function updateConfig(key: string, value: string) {
  return request.put(`/system/config/${key}`, { value })
}

/**
 * 批量更新配置
 */
export function batchUpdateConfigs(configs: Record<string, string>) {
  return request.put('/system/config/batch', configs)
}

/**
 * 初始化默认配置
 */
export function initDefaultConfigs() {
  return request.post('/system/config/init')
}
