<template>
  <Layout>
    <div class="page-header">
      <h2>规则管理</h2>
      <div class="actions">
        <el-button 
          type="danger" 
          :icon="Delete"
          @click="handleBatchDelete"
          :disabled="selectedRules.length === 0"
          :loading="batchDeleting"
        >
          批量删除 ({{ selectedRules.length }})
        </el-button>
        <el-button :icon="Download" @click="downloadTemplate">下载模板</el-button>
        <el-upload
          :show-file-list="false"
          :before-upload="handleImport"
          accept=".xlsx,.xls"
        >
          <el-button type="primary" :icon="Upload">导入 Excel</el-button>
        </el-upload>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增规则</el-button>
      </div>
    </div>

    <!-- 规则列表 -->
    <el-table 
      :data="rules" 
      v-loading="loading" 
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="tableName" label="表名" width="150" />
      <el-table-column prop="columnName" label="字段名" width="150" />
      <el-table-column prop="columnDescription" label="字段描述" width="200" />
      <el-table-column prop="standardValue" label="标准值" width="150" />
      <el-table-column prop="dirtyValues" label="错误值" min-width="200">
        <template #default="{ row }">
          <el-tag
            v-for="(value, index) in row.dirtyValues"
            :key="index"
            size="small"
            style="margin-right: 5px"
          >
            {{ value }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="confidence" label="置信度" width="100">
        <template #default="{ row }">
          {{ (row.confidence * 100).toFixed(0) }}%
        </template>
      </el-table-column>
      <el-table-column prop="source" label="来源" width="100">
        <template #default="{ row }">
          <el-tag :type="getSourceType(row.source)" size="small">
            {{ getSourceLabel(row.source) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 规则表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="form.tableName" placeholder="请输入表名" />
        </el-form-item>
        <el-form-item label="字段名" prop="columnName">
          <el-input v-model="form.columnName" placeholder="请输入字段名" />
        </el-form-item>
        <el-form-item label="字段描述">
          <el-input v-model="form.columnDescription" placeholder="请输入字段描述" />
        </el-form-item>
        <el-form-item label="标准值" prop="standardValue">
          <el-input v-model="form.standardValue" placeholder="请输入标准值" />
        </el-form-item>
        <el-form-item label="错误值" prop="dirtyValuesInput">
          <el-input
            v-model="form.dirtyValuesInput"
            type="textarea"
            :rows="3"
            placeholder="请输入错误值，多个值用顿号、逗号或分号分隔"
          />
        </el-form-item>
        <el-form-item label="自动应用">
          <el-switch v-model="form.autoApply" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </Layout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Delete } from '@element-plus/icons-vue'
import { getRules, createRule, updateRule, deleteRule, importRules, batchDeleteRules } from '@/api/rule'
import type { CleaningRule } from '@/types'
import Layout from '@/components/Layout.vue'

const loading = ref(false)
const rules = ref<CleaningRule[]>([])
const selectedRules = ref<CleaningRule[]>([])
const batchDeleting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增规则')
const formRef = ref()

const form = reactive<CleaningRule & { dirtyValuesInput?: string }>({
  tableName: '',
  columnName: '',
  columnDescription: '',
  standardValue: '',
  dirtyValues: [],
  dirtyValuesInput: '',
  confidence: 1.0,
  source: 'manual',
  autoApply: false
})

const formRules = {
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  columnName: [{ required: true, message: '请输入字段名', trigger: 'blur' }],
  standardValue: [{ required: true, message: '请输入标准值', trigger: 'blur' }],
  dirtyValuesInput: [{ required: true, message: '请输入错误值', trigger: 'blur' }]
}

onMounted(() => {
  loadRules()
})

async function loadRules() {
  loading.value = true
  try {
    rules.value = await getRules() as any
  } catch (error) {
    console.error('加载规则失败', error)
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  dialogTitle.value = '新增规则'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: CleaningRule) {
  dialogTitle.value = '编辑规则'
  Object.assign(form, row)
  form.dirtyValuesInput = row.dirtyValues.join('、')
  dialogVisible.value = true
}

async function handleDelete(row: CleaningRule) {
  try {
    await ElMessageBox.confirm('确定要删除这条规则吗？', '提示', {
      type: 'warning'
    })
    
    await deleteRule(row.id!)
    ElMessage.success('删除成功')
    loadRules()
  } catch (error) {
    // 用户取消
  }
}

async function handleBatchDelete() {
  if (selectedRules.value.length === 0) {
    ElMessage.warning('请先选择要删除的规则')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRules.value.length} 条规则吗？删除后无法恢复。`,
      '批量删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        distinguishCancelAndClose: true
      }
    )
    
    batchDeleting.value = true
    const ruleIds = selectedRules.value.map(rule => rule.id!)
    
    const result = await batchDeleteRules(ruleIds) as any
    
    ElMessage.success(`批量删除成功：已删除 ${result.deletedCount} 条规则`)
    
    // 清空选择并重新加载
    selectedRules.value = []
    loadRules()
    
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('批量删除失败', error)
      ElMessage.error('批量删除失败')
    }
  } finally {
    batchDeleting.value = false
  }
}

function handleSelectionChange(selection: CleaningRule[]) {
  selectedRules.value = selection
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
    
    // 解析错误值
    form.dirtyValues = (form.dirtyValuesInput || '')
      .split(/[、,，;；]/)
      .map(v => v.trim())
      .filter(v => v)
    
    if (form.id) {
      await updateRule(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createRule(form)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadRules()
  } catch (error) {
    console.error('提交失败', error)
  }
}

async function handleImport(file: File) {
  try {
    const result = await importRules(file) as any
    ElMessage.success(result.message || '导入成功')
    loadRules()
  } catch (error) {
    console.error('导入失败', error)
  }
  return false
}

function downloadTemplate() {
  // 动态导入 xlsx 库
  import('xlsx').then((XLSX) => {
    // 创建模板数据
    const data = [
      ['表名', '字段名', '字段描述', '正确值', '错误值', '备注'],
      ['employees', 'job_title', '职位名称', '高级工程师', '高工、高级工程师（外聘）、高级工程师-外包', '示例数据'],
      ['employees', 'department', '部门名称', '技术部', '技术部门、tech dept', '示例数据'],
      ['projects', 'status', '项目状态', '进行中', '正在进行、进行中...', '示例数据']
    ]
    
    // 创建工作簿
    const ws = XLSX.utils.aoa_to_sheet(data)
    
    // 设置列宽
    ws['!cols'] = [
      { wch: 15 },  // 表名
      { wch: 15 },  // 字段名
      { wch: 20 },  // 字段描述
      { wch: 15 },  // 正确值
      { wch: 40 },  // 错误值
      { wch: 15 }   // 备注
    ]
    
    // 设置表头样式（加粗、背景色）
    const headerStyle = {
      font: { bold: true, color: { rgb: 'FFFFFF' } },
      fill: { fgColor: { rgb: '4472C4' } },
      alignment: { horizontal: 'center', vertical: 'center' }
    }
    
    // 应用表头样式
    const headerCells = ['A1', 'B1', 'C1', 'D1', 'E1', 'F1']
    headerCells.forEach(cell => {
      if (ws[cell]) {
        ws[cell].s = headerStyle
      }
    })
    
    // 创建工作簿
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '规则模板')
    
    // 下载文件
    XLSX.writeFile(wb, '数据清洗规则导入模板.xlsx')
    
    ElMessage.success('模板下载成功')
  }).catch(error => {
    console.error('下载模板失败', error)
    ElMessage.error('下载模板失败')
  })
}

function resetForm() {
  form.id = undefined
  form.tableName = ''
  form.columnName = ''
  form.columnDescription = ''
  form.standardValue = ''
  form.dirtyValues = []
  form.dirtyValuesInput = ''
  form.confidence = 1.0
  form.source = 'manual'
  form.autoApply = false
}

function getSourceType(source: string) {
  const types: Record<string, any> = {
    manual: '',
    ai_auto: 'warning',
    ai_confirmed: 'success'
  }
  return types[source] || ''
}

function getSourceLabel(source: string) {
  const labels: Record<string, string> = {
    manual: '手动',
    ai_auto: 'AI自动',
    ai_confirmed: 'AI确认'
  }
  return labels[source] || source
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
</style>
