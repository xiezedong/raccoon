<template>
  <Layout>
    <div class="page-header">
      <h2>规则管理</h2>
      <div class="actions">
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
    <el-table :data="rules" v-loading="loading" border>
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
        <el-form-item label="错误值" prop="dirtyValues">
          <el-input
            v-model="dirtyValuesInput"
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
import { Plus, Upload } from '@element-plus/icons-vue'
import { getRules, createRule, updateRule, deleteRule, importRules } from '@/api/rule'
import type { CleaningRule } from '@/types'
import Layout from '@/components/Layout.vue'

const loading = ref(false)
const rules = ref<CleaningRule[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增规则')
const formRef = ref()
const dirtyValuesInput = ref('')

const form = reactive<CleaningRule>({
  tableName: '',
  columnName: '',
  columnDescription: '',
  standardValue: '',
  dirtyValues: [],
  confidence: 1.0,
  source: 'manual',
  autoApply: false
})

const formRules = {
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  columnName: [{ required: true, message: '请输入字段名', trigger: 'blur' }],
  standardValue: [{ required: true, message: '请输入标准值', trigger: 'blur' }],
  dirtyValues: [{ required: true, message: '请输入错误值', trigger: 'blur' }]
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
  dirtyValuesInput.value = row.dirtyValues.join('、')
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

async function handleSubmit() {
  try {
    await formRef.value.validate()
    
    // 解析错误值
    form.dirtyValues = dirtyValuesInput.value
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

function resetForm() {
  form.id = undefined
  form.tableName = ''
  form.columnName = ''
  form.columnDescription = ''
  form.standardValue = ''
  form.dirtyValues = []
  form.confidence = 1.0
  form.source = 'manual'
  form.autoApply = false
  dirtyValuesInput.value = ''
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
