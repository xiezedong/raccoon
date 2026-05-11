<template>
  <Layout>
    <div class="page-header">
      <h2>AI 发现</h2>
      <div class="actions">
        <el-badge :value="stats.pending" :hidden="stats.pending === 0" type="primary">
          <el-button :icon="List" @click="showCandidates">待审核规则</el-button>
        </el-badge>
        <el-button :icon="Refresh" @click="loadStats">刷新</el-button>
      </div>
    </div>

    <el-alert
      title="AI 辅助发现功能"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 20px"
    >
      系统将自动扫描目标数据库的所有表和字段，利用大模型发现可能的脏数据，并生成候选规则供您审核。
    </el-alert>

    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.pending }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.approved }}</div>
            <div class="stat-label">已通过</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.rejected }}</div>
            <div class="stat-label">已拒绝</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">总计</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 发现操作 -->
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>AI 智能发现</span>
          <el-button 
            type="primary" 
            :icon="Search" 
            @click="handleDiscoverAll"
            :loading="discovering"
            size="large"
          >
            开始全自动发现
          </el-button>
        </div>
      </template>

      <el-alert
        title="工作原理"
        type="info"
        :closable="false"
      >
        <div style="line-height: 1.8">
          <div>1. 自动扫描目标数据库的所有表和文本类型字段</div>
          <div>2. 获取每个字段的唯一值，排除已有规则覆盖的数据</div>
          <div>3. 利用大模型分析识别可能的脏数据（变体、简称、错别字等）</div>
          <div>4. 生成候选规则供您审核，通过后自动转为正式规则</div>
        </div>
      </el-alert>

      <!-- 发现进度 -->
      <el-alert
        v-if="discovering"
        title="AI 正在分析中..."
        type="warning"
        :closable="false"
        show-icon
        style="margin-top: 20px"
      >
        <template #default>
          <div>正在扫描数据库并分析，这可能需要较长时间，请耐心等待...</div>
          <el-progress 
            :percentage="100" 
            :indeterminate="true" 
            style="margin-top: 10px"
          />
        </template>
      </el-alert>

      <!-- 发现结果 -->
      <el-alert
        v-if="lastResult"
        :title="lastResult.success ? '发现完成' : '发现失败'"
        :type="lastResult.success ? 'success' : 'error'"
        :closable="false"
        show-icon
        style="margin-top: 20px"
      >
        <div>{{ lastResult.message }}</div>
        <div v-if="lastResult.success && lastResult.candidateCount > 0">
          发现了 <strong>{{ lastResult.candidateCount }}</strong> 条候选规则
        </div>
      </el-alert>
    </el-card>

    <!-- 候选规则审核对话框 -->
    <el-dialog
      v-model="showDialog"
      title="审核候选规则"
      width="90%"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div style="margin-bottom: 20px; display: flex; align-items: center; gap: 15px">
        <el-input
          v-model="searchText"
          placeholder="搜索表名、字段名或标准值"
          :prefix-icon="Search"
          clearable
          style="width: 300px"
        />
        <div style="display: flex; align-items: center; gap: 10px; flex: 1">
          <span style="color: #606266; white-space: nowrap">最低置信度:</span>
          <el-slider
            v-model="minConfidence"
            :min="0"
            :max="1"
            :step="0.1"
            show-input
            style="flex: 1; max-width: 300px"
            @change="loadCandidates"
          />
        </div>
      </div>

      <el-table
        :data="filteredCandidates"
        v-loading="loadingCandidates"
        @selection-change="handleSelectionChange"
        max-height="500"
        stripe
        border
      >
        <el-table-column type="selection" width="50" fixed />
        <el-table-column prop="tableName" label="表名" width="140" fixed />
        <el-table-column prop="columnName" label="字段名" width="120" />
        <el-table-column prop="standardValue" label="标准值" width="140" show-overflow-tooltip />
        <el-table-column prop="dirtyValues" label="错误值" min-width="250">
          <template #default="{ row }">
            <div style="display: flex; flex-wrap: wrap; gap: 4px">
              <el-tag
                v-for="(value, index) in row.dirtyValues"
                :key="index"
                size="small"
                type="info"
              >
                {{ value }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="理由" min-width="250" show-overflow-tooltip />
        <el-table-column prop="confidence" label="置信度" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getConfidenceType(row.confidence)" size="small">
              {{ (row.confidence * 100).toFixed(0) }}%
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="color: #909399">
            已选择 {{ selectedCandidates.length }} 条规则
          </span>
          <div>
            <el-button @click="showDialog = false">取消</el-button>
            <el-button 
              type="danger" 
              @click="handleReject"
              :disabled="selectedCandidates.length === 0"
            >
              拒绝选中
            </el-button>
            <el-button 
              type="primary" 
              @click="handleApprove"
              :disabled="selectedCandidates.length === 0"
            >
              通过选中
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </Layout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, List } from '@element-plus/icons-vue'
import { discoverAll, getPendingRules, reviewRules, getStats } from '@/api/discovery'
import type { CandidateRule, DiscoveryResult } from '@/types'
import Layout from '@/components/Layout.vue'

const discovering = ref(false)
const lastResult = ref<DiscoveryResult | null>(null)

const stats = ref({
  pending: 0,
  approved: 0,
  rejected: 0,
  total: 0
})

const showDialog = ref(false)
const loadingCandidates = ref(false)
const candidates = ref<CandidateRule[]>([])
const selectedCandidates = ref<CandidateRule[]>([])
const searchText = ref('')
const minConfidence = ref(0.7)

const filteredCandidates = computed(() => {
  if (!searchText.value) {
    return candidates.value
  }
  const keyword = searchText.value.toLowerCase()
  return candidates.value.filter(c => 
    c.tableName.toLowerCase().includes(keyword) ||
    c.columnName.toLowerCase().includes(keyword) ||
    c.standardValue.toLowerCase().includes(keyword)
  )
})

onMounted(() => {
  loadStats()
})

async function loadStats() {
  try {
    const data = await getStats() as any
    stats.value = data
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

async function handleDiscoverAll() {
  try {
    await ElMessageBox.confirm(
      '系统将自动扫描目标数据库的所有表和字段，这可能需要较长时间。确定开始吗？',
      '确认发现',
      {
        confirmButtonText: '开始发现',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  discovering.value = true
  lastResult.value = null

  try {
    const result = await discoverAll() as any
    lastResult.value = result

    if (result.success) {
      ElMessage.success(`发现完成！${result.message}`)
      await loadStats()
      
      if (result.candidateCount > 0) {
        // 询问是否立即审核
        ElMessageBox.confirm(
          `已发现 ${result.candidateCount} 条候选规则，是否立即审核？`,
          '提示',
          {
            confirmButtonText: '立即审核',
            cancelButtonText: '稍后审核',
            type: 'success'
          }
        ).then(() => {
          showCandidates()
        }).catch(() => {
          // 用户选择稍后审核
        })
      }
    } else {
      ElMessage.error(result.message || '发现失败')
    }
  } catch (error: any) {
    console.error('AI 发现失败', error)
    ElMessage.error(error.message || 'AI 发现失败')
  } finally {
    discovering.value = false
  }
}

async function showCandidates() {
  showDialog.value = true
  await loadCandidates()
}

async function loadCandidates() {
  loadingCandidates.value = true
  try {
    candidates.value = await getPendingRules(minConfidence.value) as any
  } catch (error) {
    console.error('加载候选规则失败', error)
    ElMessage.error('加载候选规则失败')
  } finally {
    loadingCandidates.value = false
  }
}

function handleSelectionChange(selection: CandidateRule[]) {
  selectedCandidates.value = selection
}

async function handleApprove() {
  if (selectedCandidates.value.length === 0) {
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定通过选中的 ${selectedCandidates.value.length} 条规则吗？通过后将自动转为正式清洗规则。`,
      '确认通过',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const approvedIds = selectedCandidates.value.map(c => c.id!)
    await reviewRules({
      approvedIds,
      rejectedIds: [],
      reviewedBy: 'admin'
    })

    ElMessage.success('审核完成')
    await loadCandidates()
    await loadStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('审核失败', error)
      ElMessage.error('审核失败')
    }
  }
}

async function handleReject() {
  if (selectedCandidates.value.length === 0) {
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定拒绝选中的 ${selectedCandidates.value.length} 条规则吗？`,
      '确认拒绝',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const rejectedIds = selectedCandidates.value.map(c => c.id!)
    await reviewRules({
      approvedIds: [],
      rejectedIds,
      reviewedBy: 'admin'
    })

    ElMessage.success('已拒绝')
    await loadCandidates()
    await loadStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('拒绝失败', error)
      ElMessage.error('拒绝失败')
    }
  }
}

function getConfidenceType(confidence: number) {
  if (confidence >= 0.9) return 'success'
  if (confidence >= 0.8) return 'primary'
  if (confidence >= 0.7) return 'warning'
  return 'info'
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
  text-align: center;
  padding: 10px 0;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}
</style>
