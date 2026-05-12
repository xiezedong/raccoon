<template>
  <Layout>
    <div class="page-header">
      <h2>清洗执行</h2>
      <div class="actions">
        <el-button :icon="Refresh" @click="scanDirtyData" :loading="scanning">
          {{ scanning ? '扫描中...' : '扫描脏数据' }}
        </el-button>
      </div>
    </div>

    <el-alert
      title="数据清洗执行"
      type="warning"
      :closable="false"
      style="margin-bottom: 20px"
    >
      点击"扫描脏数据"按钮，系统会根据规则扫描目标数据库，只显示命中错误值的规则。
    </el-alert>

    <!-- 统计信息 -->
    <el-row :gutter="20" style="margin-bottom: 20px" v-if="dirtyDataList.length > 0">
      <el-col :span="5">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="待清洗规则数" :value="dirtyDataList.length">
            <template #suffix>
              <span style="font-size: 14px">条</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="总影响记录数" :value="totalAffectedCount">
            <template #suffix>
              <span style="font-size: 14px">条</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-time">
            <div class="stat-title">
              <el-icon><Clock /></el-icon>
              <span>最后扫描时间</span>
            </div>
            <div class="stat-value">{{ lastScanTime || '-' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="hover" class="stat-card">
          <div style="text-align: center">
            <el-button 
              type="primary" 
              :icon="Operation"
              @click="handleBatchExecute"
              :disabled="selectedRules.length === 0"
              :loading="batchExecuting"
              size="large"
            >
              批量执行 ({{ selectedRules.length }})
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div style="text-align: center">
            <el-button 
              type="danger" 
              :icon="Delete"
              @click="handleBatchDelete"
              :disabled="selectedRules.length === 0"
              :loading="batchDeleting"
              size="large"
            >
              批量删除 ({{ selectedRules.length }})
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 脏数据列表 -->
    <el-card shadow="never" v-loading="loading">
      <template v-if="dirtyDataList.length > 0">
        <el-table 
          :data="dirtyDataList" 
          border
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="tableName" label="表名" width="150" />
          <el-table-column prop="columnName" label="字段名" width="150" />
          <el-table-column prop="columnDescription" label="字段描述" width="180" />
          <el-table-column prop="standardValue" label="标准值" width="150">
            <template #default="{ row }">
              <el-tag type="success">{{ row.standardValue }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="错误值" min-width="200">
            <template #default="{ row }">
              <el-tag
                v-for="(value, index) in row.dirtyValues.slice(0, 3)"
                :key="index"
                type="danger"
                size="small"
                style="margin-right: 5px; margin-bottom: 5px"
              >
                {{ value }}
              </el-tag>
              <el-tag v-if="row.dirtyValues.length > 3" size="small" type="info">
                +{{ row.dirtyValues.length - 3 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="affectedCount" label="影响记录数" width="120" align="center">
            <template #default="{ row }">
              <el-tag 
                :type="getCountType(row.affectedCount)" 
                effect="dark"
              >
                {{ row.affectedCount }} 条
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button 
                size="small" 
                :icon="View"
                @click="handlePreview(row)"
              >
                预览
              </el-button>
              <el-button 
                size="small" 
                type="primary"
                :icon="Check"
                @click="handleExecute(row)"
                :loading="executingRuleId === row.ruleId"
              >
                执行
              </el-button>
              <el-button 
                size="small" 
                type="danger"
                :icon="Delete"
                @click="handleDelete(row)"
                :loading="deletingId === row.id"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
      
      <el-empty 
        v-else-if="!loading && !scanning"
        description="暂无脏数据，点击【扫描脏数据】按钮开始扫描"
        :image-size="120"
      />
    </el-card>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="清洗预览"
      width="900px"
    >
      <div v-if="preview" v-loading="previewLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="表名">
            {{ preview.tableName }}
          </el-descriptions-item>
          <el-descriptions-item label="字段名">
            {{ preview.columnName }}
          </el-descriptions-item>
          <el-descriptions-item label="标准值">
            <el-tag type="success" size="large">{{ preview.standardValue }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="影响记录数">
            <el-tag :type="getCountType(preview.affectedCount)" size="large">
              {{ preview.affectedCount }} 条
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">
          样本数据（显示前 {{ Math.min(preview.samples.length, 50) }} 条）
        </el-divider>

        <el-table :data="preview.samples.slice(0, 50)" border max-height="500">
          <el-table-column prop="id" label="记录ID" width="100" />
          <el-table-column prop="dirtyValue" label="当前值（错误）" min-width="250">
            <template #default="{ row }">
              <el-tag type="danger" effect="dark">{{ row.dirtyValue }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="将修改为" min-width="250">
            <template #default="{ row }">
              <el-icon style="margin-right: 5px"><Right /></el-icon>
              <el-tag type="success" effect="dark">{{ row.standardValue }}</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-alert
          v-if="preview.affectedCount > preview.samples.length"
          :title="`注意：实际影响 ${preview.affectedCount} 条记录，此处仅显示前 ${preview.samples.length} 条样本`"
          type="info"
          :closable="false"
          style="margin-top: 15px"
        />

        <el-alert
          v-if="preview.affectedCount > 100"
          title="警告：影响记录数较多，执行前请仔细确认！"
          type="warning"
          :closable="false"
          style="margin-top: 15px"
        />
      </div>

      <template #footer>
        <el-button @click="previewVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          :icon="Check"
          @click="confirmExecute"
          :loading="executing"
        >
          确认执行清洗
        </el-button>
      </template>
    </el-dialog>

    <!-- 执行结果对话框 -->
    <el-dialog
      v-model="resultVisible"
      :title="result?.success ? '执行成功' : '执行失败'"
      width="600px"
    >
      <el-result
        :icon="result?.success ? 'success' : 'error'"
        :title="result?.success ? '数据清洗完成' : '数据清洗失败'"
      >
        <template #sub-title>
          <div v-if="result?.success">
            <p>总记录数: <strong>{{ result.totalRecords }}</strong></p>
            <p>成功清洗: <strong style="color: #67c23a">{{ result.cleanedRecords }}</strong></p>
            <p>任务ID: {{ result.taskId }}</p>
          </div>
          <div v-else>
            <p style="color: #f56c6c">{{ result?.errorMessage }}</p>
          </div>
        </template>
        <template #extra>
          <el-button type="primary" @click="handleResultClose">确定</el-button>
          <el-button v-if="result?.success" @click="goToLogs">查看日志</el-button>
        </template>
      </el-result>
    </el-dialog>

    <!-- 批量执行结果对话框 -->
    <el-dialog
      v-model="batchResultVisible"
      title="批量执行结果"
      width="800px"
    >
      <div v-if="batchResult">
        <!-- 统计摘要 -->
        <el-descriptions :column="3" border style="margin-bottom: 20px">
          <el-descriptions-item label="总规则数">
            {{ batchResult.totalRules }}
          </el-descriptions-item>
          <el-descriptions-item label="成功">
            <el-tag type="success">{{ batchResult.successCount }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="失败">
            <el-tag type="danger">{{ batchResult.failedCount }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总记录数">
            {{ batchResult.totalRecords }}
          </el-descriptions-item>
          <el-descriptions-item label="已清洗">
            <el-tag type="success">{{ batchResult.totalCleaned }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="耗时">
            {{ calculateDuration(batchResult.startTime, batchResult.endTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 详细结果 -->
        <el-divider content-position="left">详细结果</el-divider>
        
        <el-table :data="batchResult.results" border max-height="400">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.success" color="#67c23a" :size="20">
                <CircleCheck />
              </el-icon>
              <el-icon v-else color="#f56c6c" :size="20">
                <CircleClose />
              </el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="totalRecords" label="影响记录数" width="120" align="center" />
          <el-table-column prop="cleanedRecords" label="已清洗" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.success" type="success">{{ row.cleanedRecords }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="taskId" label="任务ID" width="100" />
          <el-table-column prop="errorMessage" label="错误信息" min-width="200">
            <template #default="{ row }">
              <span v-if="row.errorMessage" style="color: #f56c6c">
                {{ row.errorMessage }}
              </span>
              <span v-else style="color: #67c23a">成功</span>
            </template>
          </el-table-column>
        </el-table>

        <el-alert
          v-if="batchResult.failedCount > 0"
          :title="`有 ${batchResult.failedCount} 条规则执行失败，请查看详细信息`"
          type="warning"
          :closable="false"
          style="margin-top: 15px"
        />
      </div>

      <template #footer>
        <el-button type="primary" @click="handleBatchResultClose">确定</el-button>
        <el-button @click="goToLogs">查看日志</el-button>
      </template>
    </el-dialog>
  </Layout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, View, Check, Delete, Clock, Right, CircleCheck, CircleClose, Operation } from '@element-plus/icons-vue'
import { getRules } from '@/api/rule'
import { previewClean, executeClean, executeBatchClean, scanAllRules, getPendingScans, deleteScan, batchDeleteScans } from '@/api/cleaning'
import type { CleaningRule, CleaningPreview, CleaningResult, DirtyDataScan, ScanResult } from '@/types'
import Layout from '@/components/Layout.vue'

interface DirtyDataItem {
  id: number
  ruleId: number
  tableName: string
  columnName: string
  columnDescription?: string
  standardValue: string
  dirtyValues: string[]
  affectedCount: number
  scannedAt: string
}

interface BatchCleaningResult {
  totalRules: number
  successCount: number
  failedCount: number
  totalRecords: number
  totalCleaned: number
  results: CleaningResult[]
  startTime: string
  endTime: string
}

const router = useRouter()
const loading = ref(false)
const scanning = ref(false)
const dirtyDataList = ref<DirtyDataItem[]>([])
const previewVisible = ref(false)
const previewLoading = ref(false)
const preview = ref<CleaningPreview | null>(null)
const executing = ref(false)
const executingRuleId = ref<number | null>(null)
const deletingId = ref<number | null>(null)
const resultVisible = ref(false)
const result = ref<CleaningResult | null>(null)
const currentRule = ref<DirtyDataItem | null>(null)
const selectedRules = ref<DirtyDataItem[]>([])
const lastScanTime = ref<string>('')
const batchExecuting = ref(false)
const batchDeleting = ref(false)
const batchResult = ref<BatchCleaningResult | null>(null)
const batchResultVisible = ref(false)

const totalAffectedCount = computed(() => {
  return dirtyDataList.value.reduce((sum, item) => sum + item.affectedCount, 0)
})

onMounted(() => {
  // 页面加载时自动加载扫描结果
  loadPendingScans()
})

async function loadPendingScans() {
  loading.value = true
  try {
    const scans = await getPendingScans() as DirtyDataScan[]
    
    dirtyDataList.value = scans.map(scan => ({
      id: scan.id,
      ruleId: scan.ruleId,
      tableName: scan.tableName,
      columnName: scan.columnName,
      standardValue: scan.standardValue,
      dirtyValues: scan.dirtyValues,
      affectedCount: scan.affectedCount,
      scannedAt: scan.scannedAt
    }))
    
    if (scans.length > 0) {
      lastScanTime.value = new Date(scans[0].scannedAt).toLocaleString('zh-CN')
    }
  } catch (error) {
    console.error('加载扫描结果失败', error)
  } finally {
    loading.value = false
  }
}

async function scanDirtyData() {
  scanning.value = true
  
  try {
    ElMessage.info('开始扫描...')
    
    const result = await scanAllRules('user') as ScanResult
    
    ElMessage.success(result.message)
    
    // 重新加载扫描结果
    await loadPendingScans()
    
  } catch (error) {
    console.error('扫描失败', error)
    ElMessage.error('扫描失败')
  } finally {
    scanning.value = false
  }
}

async function handlePreview(item: DirtyDataItem) {
  currentRule.value = item
  previewVisible.value = true
  previewLoading.value = true
  
  try {
    preview.value = await previewClean(item.ruleId) as CleaningPreview
  } catch (error) {
    console.error('预览失败', error)
    ElMessage.error('预览失败')
    previewVisible.value = false
  } finally {
    previewLoading.value = false
  }
}

async function handleExecute(item: DirtyDataItem) {
  try {
    // 二次确认
    await ElMessageBox.confirm(
      `<div style="line-height: 1.8;">
        <p><strong>表名：</strong>${item.tableName}</p>
        <p><strong>字段名：</strong>${item.columnName}</p>
        <p><strong>标准值：</strong><span style="color: #67c23a">${item.standardValue}</span></p>
        <p><strong>影响记录数：</strong><span style="color: ${item.affectedCount > 100 ? '#f56c6c' : '#e6a23c'}">${item.affectedCount} 条</span></p>
        <p style="margin-top: 10px; color: #f56c6c;">此操作将修改数据库数据，是否继续？</p>
      </div>`,
      '确认执行清洗',
      {
        type: 'warning',
        confirmButtonText: '确认执行',
        cancelButtonText: '取消',
        dangerouslyUseHTMLString: true,
        distinguishCancelAndClose: true
      }
    )
    
    // 执行清洗
    executingRuleId.value = item.ruleId
    const cleanResult = await executeClean(item.ruleId, 'user') as CleaningResult
    
    // 检查是否需要二次确认
    if (cleanResult.needConfirm) {
      ElMessage.warning(cleanResult.errorMessage || '影响记录数超过安全阈值，请联系管理员调整配置')
      return
    }
    
    result.value = cleanResult
    resultVisible.value = true
    
    if (cleanResult.success) {
      ElMessage.success('清洗执行成功')
    }
    
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('执行失败', error)
      ElMessage.error('执行失败')
    }
  } finally {
    executingRuleId.value = null
  }
}

async function confirmExecute() {
  if (!currentRule.value) return
  
  executing.value = true
  try {
    const cleanResult = await executeClean(currentRule.value.ruleId, 'user') as CleaningResult
    
    previewVisible.value = false
    result.value = cleanResult
    resultVisible.value = true
    
    if (cleanResult.success) {
      ElMessage.success('清洗执行成功')
    }
  } catch (error) {
    console.error('执行失败', error)
    ElMessage.error('执行失败')
  } finally {
    executing.value = false
  }
}

async function handleBatchExecute() {
  if (selectedRules.value.length === 0) {
    ElMessage.warning('请先选择要执行的规则')
    return
  }
  
  const totalCount = selectedRules.value.reduce((sum, item) => sum + item.affectedCount, 0)
  
  try {
    await ElMessageBox.confirm(
      `将批量执行 ${selectedRules.value.length} 条规则，共影响 ${totalCount} 条记录，是否继续？`,
      '批量执行确认',
      {
        type: 'warning',
        confirmButtonText: '确认执行',
        cancelButtonText: '取消',
        distinguishCancelAndClose: true
      }
    )
    
    batchExecuting.value = true
    const ruleIds = selectedRules.value.map(item => item.ruleId)
    
    ElMessage.info('批量执行中，请稍候...')
    
    const result = await executeBatchClean(ruleIds, 'user') as BatchCleaningResult
    
    batchResult.value = result
    batchResultVisible.value = true
    
    if (result.successCount > 0) {
      ElMessage.success(`批量执行完成：成功 ${result.successCount} 条，失败 ${result.failedCount} 条`)
    } else {
      ElMessage.error('批量执行失败')
    }
    
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('批量执行失败', error)
      ElMessage.error('批量执行失败')
    }
  } finally {
    batchExecuting.value = false
  }
}

function handleSelectionChange(selection: DirtyDataItem[]) {
  selectedRules.value = selection
}

function handleResultClose() {
  resultVisible.value = false
  // 重新加载扫描结果
  loadPendingScans()
}

function handleBatchResultClose() {
  batchResultVisible.value = false
  // 清空选择并重新加载
  selectedRules.value = []
  loadPendingScans()
}

function goToLogs() {
  resultVisible.value = false
  router.push('/logs')
}

function getCountType(count: number) {
  if (count > 1000) return 'danger'
  if (count > 100) return 'warning'
  return 'success'
}

function calculateDuration(startTime: string, endTime: string) {
  const start = new Date(startTime).getTime()
  const end = new Date(endTime).getTime()
  const duration = Math.round((end - start) / 1000)
  
  if (duration < 60) {
    return `${duration} 秒`
  } else {
    const minutes = Math.floor(duration / 60)
    const seconds = duration % 60
    return `${minutes} 分 ${seconds} 秒`
  }
}

async function handleDelete(item: DirtyDataItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除这条扫描记录吗？删除后需要重新扫描才能再次显示。`,
      '确认删除',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }
    )
    
    deletingId.value = item.id
    
    await deleteScan(item.id)
    
    ElMessage.success('删除成功')
    
    // 从列表中移除
    dirtyDataList.value = dirtyDataList.value.filter(d => d.id !== item.id)
    
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
      ElMessage.error('删除失败')
    }
  } finally {
    deletingId.value = null
  }
}

async function handleBatchDelete() {
  if (selectedRules.value.length === 0) {
    ElMessage.warning('请先选择要删除的记录')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRules.value.length} 条扫描记录吗？删除后需要重新扫描才能再次显示。`,
      '批量删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        distinguishCancelAndClose: true
      }
    )
    
    batchDeleting.value = true
    const scanIds = selectedRules.value.map(item => item.id)
    
    const result = await batchDeleteScans(scanIds) as any
    
    ElMessage.success(`批量删除成功：已删除 ${result.deletedCount} 条记录`)
    
    // 从列表中移除已删除的记录
    dirtyDataList.value = dirtyDataList.value.filter(
      item => !scanIds.includes(item.id)
    )
    
    // 清空选择
    selectedRules.value = []
    
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('批量删除失败', error)
      ElMessage.error('批量删除失败')
    }
  } finally {
    batchDeleting.value = false
  }
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

:deep(.el-descriptions__label) {
  font-weight: 600;
}

:deep(.el-result__title) {
  font-size: 20px;
  margin-top: 10px;
}

:deep(.el-result__subtitle) {
  margin-top: 10px;
}

:deep(.el-result__subtitle p) {
  margin: 8px 0;
  font-size: 15px;
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

:deep(.el-card__body) {
  padding: 20px;
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

.stat-time {
  text-align: center;
  width: 100%;
}

.stat-time .stat-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.stat-time .stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.stat-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}
</style>
