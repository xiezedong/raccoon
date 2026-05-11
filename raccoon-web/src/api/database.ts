import request from '@/utils/request'

export interface ConnectionConfig {
  host: string
  port: string
  database: string
  username: string
  password: string
}

/**
 * 测试数据库连接
 */
export function testConnection(config: ConnectionConfig) {
  return request.post('/target-database/test-connection', config)
}

/**
 * 获取数据库列表
 */
export function getDatabaseList(config: Omit<ConnectionConfig, 'database'>) {
  return request.post<string[]>('/target-database/databases', config)
}
