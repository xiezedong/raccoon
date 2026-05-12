<template>
  <Layout>
    <div class="page-header">
      <h2>清洗日志</h2>
      <div class="actions">
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="总清洗记录数" :value="statistics.totalCleanedRecords">
            <template #suffix>
              <span style="font-size: 14px">条</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="总任务数" :value="statistics.totalTasks">
            <template #suffix>
              <span style="font-size: 14px">个</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="今日清洗" :value="statistics.todayLogs">
            <template #suffix>
              <span style="font-size: 14px">条</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="本周清洗" :value="statistics.weekLogs">
            <template #suffix>
              <span style="font-size: 14px">条</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 标签页 -->
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 任务列表 -->
        <el-tab-pane label="任务列表" name="tasks">
          <el-table :data="tasks" v-loading="loading" border>
            <el-table-column prop="id" label="任务ID" width="80" />
            <el-table-column prop="taskName" label="任务名称" min-width="200" />
            <el-table-column prop="tableName" label="表名" width="150" />
            <el-table-column prop="columnName" label="字段名" width="150" />
            <el-table-column prop="totalRecords" label="总记录数" width="100" align="center" />
            <el-table-column prop="cleanedRecords" label="已清洗" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'completed'" type="success">
                  {{ row.cleanedRecords }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="createdBy" label="执行人" width="100" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" :icon="View" @click="handleViewLogs(row)">
                  查看日志
                </el-button>
                <el-button 
                  v-if="row.status === 'completed'"
                  size="small" 
                  type="warning"
                  :icon="RefreshLeft"
                  @click="handleRollback(row)"
                >
                  回滚
                </el-button>
                <el-tag v-if="row.status === 'rolled_back'" type="info" size="small">
                  已回滚
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 日志详情 -->
        <el-tab-pane label="日志详情" name="logs">
          <div style="margin-bottom: 15px">
            <el-alert
              title="显示最近 100 条清洗日志"
              type="info"
              :closable="false"
            />
          </div>
          
          <el-table :data="logs" v-loading="loading" border max-height="600">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="tableName" label="表名" width="150" />
            <el-table-column prop="columnName" label="字段名" width="150" />
            <el-table-column prop="recordId" label="记录ID" width="100" />
            <el-table-column prop="oldValue" label="原值" min-width="200">
              <template #default="{ row }">
                <el-tag type="danger" effect="plain">{{ row.oldValue }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="newValue" label="新值" min-width="200">
              <template #default="{ row }">
                <el-tag type="success" effect="plain">{{ row.newValue }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="executedAt" label="执行时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.executedAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="executedBy" label="执行人" width="100" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 任务日志详情对话框 -->
    <el-dialog
      v-model="logDialogVisible"
      :title="`任务日志 - ${currentTask?.taskName}`"
      width="1000px"
    >
      <div v-if="currentTask">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="任务ID">
            {{ currentTask.id }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentTask.status)">
              {{ getStatusLabel(currentTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="表名">
            {{ currentTask.tableName }}
          </el-descriptions-item>
          <el-descriptions-item label="字段名">
            {{ currentTask.columnName }}
          </el-descriptions-item>
          <el-descriptions-item label="总记录数">
            {{ currentTask.totalRecords }}
          </el-descriptions-item>
          <el-descriptions-item label="已清洗">
            <el-tag type="success">{{ currentTask.cleanedRecords }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">
            {{ currentTask.startedAt ? formatDateTime(currentTask.startedAt) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="完成时间">
            {{ currentTask.completedAt ? formatDateTime(currentTask.completedAt) : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">清洗记录</el-divider>

        <el-table :data="taskLogs" v-loading="taskLogsLoading" border max-height="400">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="recordId" label="记录ID" width="100" />
          <el-table-column prop="oldValue" label="原值" min-width="200">
            <template #default="{ row }">
              <el-tag type="danger" effect="plain">{{ row.oldValue }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="修改为" min-width="200">
            <template #default="{ row }">
              <el-icon style="margin-right: 5px"><Right /></el-icon>
              <el-tag type="success" effect="plain">{{ row.newValue }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="executedAt" label="执行时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.executedAt) }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="logDialogVisible = false">关闭</el-button>
        <el-button 
          v-if="currentTask?.status === 'completed'"
          type="warning" 
          :icon="RefreshLeft"
          @click="handleRollbackFromDialog"
        >
          回滚此任务
        </el-button>
      </template>
    </el-dialog>
  </Layout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, View, RefreshLeft, Right } from '@element-plus/icons-vue'
import { getAllTasks, getRecentLogs, getLogsByTaskId, getStatistics, rollbackTask } from '@/api/log'
import type { CleaningTask, CleaningLog, LogStatistics } from '@/types'
import Layout from '@/components/Layout.vue'

const loading = ref(false)
const activeTab = ref('tasks')
const tasks = ref<CleaningTask[]>([])
const logs = ref<CleaningLog[]>([])
const statistics = reactive<LogStatistics>({
  totalLogs: 0,
  totalTasks: 0,
  completedTasks: 0,
  failedTasks: 0,
  runningTasks: 0,
  totalCleanedRecords: 0,
  todayLogs: 0,
  weekLogs: 0
})

const logDialogVisible = ref(false)
const currentTask = ref<CleaningTask | null>(null)
const taskLogs = ref<CleaningLog[]>([])
const taskLogsLoading = ref(false)

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    // 加载统计信息
    const stats = await getStatistics() as LogStatistics
    Object.assign(statistics, stats)
    
    // 根据当前标签页加载数据
    if (activeTab.value === 'tasks') {
      await loadTasks()
    } else {
      await loadLogs()
    }
  } catch (error) {
    console.error('加载数据失败', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function loadTasks() {
  tasks.value = await getAllTasks() as CleaningTask[]
}

async function loadLogs() {
  logs.value = await getRecentLogs(100) as CleaningLog[]
}

function handleTabChange(tabName: string) {
  if (tabName === 'tasks') {
    loadTasks()
  } else {
    loadLogs()
  }
}

async function handleViewLogs(task: CleaningTask) {
  currentTask.value = task
  logDialogVisible.value = true
  taskLogsLoading.value = true
  
  try {
    taskLogs.value = await getLogsByTaskId(task.id) as CleaningLog[]
  } catch (error) {
    console.error('加载任务日志失败', error)
    ElMessage.error('加载任务日志失败')
  } finally {
    taskLogsLoading.value = false
  }
}

async function handleRollback(task: CleaningTask) {
  try {
    await ElMessageBox.confirm(
      `<div style="line-height: 1.8;">
        <p><strong>任务名称：</strong>${task.taskName}</p>
        <p><strong>清洗记录数：</strong>${task.cleanedRecords} 条</p>
        <p style="margin-top: 10px; color: #f56c6c;">此操作将恢复所有被清洗的数据，是否继续？</p>
      </div>`,
      '确认回滚',
      {
        type: 'warning',
        confirmButtonText: '确认回滚',
        cancelButtonText: '取消',
        dangerouslyUseHTMLString: true
      }
    )
    
    const result = await rollbackTask(task.id, 'user') as any
    
    if (result.success) {
      ElMessage.success(result.message || '回滚成功')
      loadData()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('回滚失败', error)
      ElMessage.error('回滚失败')
    }
  }
}

async function handleRollbackFromDialog() {
  if (!currentTask.value) return
  
  logDialogVisible.value = false
  await handleRollback(currentTask.value)
}

function getStatusType(status: string) {
  const types: Record<string, any> = {
    pending: 'info',
    pending_confirm: 'warning',
    running: 'primary',
    completed: 'success',
    failed: 'danger',
    rolled_back: 'warning'
  }
  return types[status] || 'info'
}

function getStatusLabel(status: string) {
  const labels: Record<string, string> = {
    pending: '待执行',
    pending_confirm: '待确认',
    running: '执行中',
    completed: '已完成',
    failed: '失败',
    rolled_back: '已回滚'
  }
  return labels[status] || status
}

function formatDateTime(dateStr: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.actions {
  display: flex;
  gap: 10px;
}

.stat-card {
  height: 120px;
}

.stat-card :deep(.el-card__body) {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-statistic__head) {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

:deep(.el-statistic__number) {
  font-size: 28px;
  font-weight: 600;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
}
</style>
