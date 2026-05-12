<template>
  <Layout>
    <div class="page-header">
      <h2>系统设置</h2>
      <div class="actions">
        <el-button :icon="Refresh" @click="loadConfigs">刷新</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave" :loading="saving">保存配置</el-button>
      </div>
    </div>

    <el-alert
      title="配置说明"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 20px"
    >
      这里的配置会覆盖 application.yml 中的默认配置。
    </el-alert>

    <el-card v-loading="loading" shadow="never" class="config-card">
      <el-tabs v-model="activeTab">
        <!-- 数据库配置 -->
        <el-tab-pane label="目标数据库配置" name="database">
          <el-alert
            title="说明"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 20px"
          >
            数据清洗的目标数据库配置
          </el-alert>

          <el-form :model="configs" label-width="180px" class="config-form">
            <el-form-item label="主机地址">
              <el-input
                v-model="configs['target.db.host']"
                placeholder="localhost"
              />
            </el-form-item>

            <el-form-item label="端口">
              <el-input-number
                v-model="targetDbPort"
                :min="1"
                :max="65535"
                style="width: 200px"
              />
            </el-form-item>

            <el-form-item label="数据库名">
              <el-input
                v-model="configs['target.db.database']"
                placeholder="请输入数据库名"
                style="width: 300px"
              >
                <template #append>
                  <el-button
                    :icon="Search"
                    @click="handleLoadDatabases"
                    :loading="loadingDatabases"
                  >
                    选择
                  </el-button>
                </template>
              </el-input>
              <span class="form-tip">点击"选择"按钮可从服务器获取数据库列表</span>
            </el-form-item>

            <el-form-item label="用户名">
              <el-input
                v-model="configs['target.db.username']"
                placeholder="postgres"
              />
            </el-form-item>

            <el-form-item label="密码">
              <el-input
                v-model="configs['target.db.password']"
                type="password"
                show-password
                placeholder="请输入数据库密码"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :icon="Connection"
                @click="handleTestConnection"
                :loading="testing"
              >
                测试连接
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 大模型配置 -->
        <el-tab-pane label="大模型配置" name="llm">
          <el-form :model="configs" label-width="180px" class="config-form">
            <el-form-item label="API地址">
              <el-input
                v-model="configs['llm.api_url']"
                placeholder="https://api.openai.com/v1/chat/completions"
              />
              <span class="form-tip">支持 OpenAI 及兼容格式的 API（通义千问、文心一言等）</span>
            </el-form-item>

            <el-form-item label="API密钥">
              <el-input
                v-model="configs['llm.api_key']"
                type="password"
                show-password
                placeholder="请输入API密钥"
              />
            </el-form-item>

            <el-form-item label="模型名称">
              <el-input
                v-model="configs['llm.model']"
                placeholder="gpt-4"
              />
              <span class="form-tip">如: gpt-4, gpt-3.5-turbo, qwen-max, ernie-bot-4</span>
            </el-form-item>

            <el-form-item label="温度">
              <el-slider
                v-model="temperatureValue"
                :min="0"
                :max="1"
                :step="0.1"
                show-input
                :input-size="'small'"
              />
              <span class="form-tip">控制输出的随机性，0-1之间，越低越确定</span>
            </el-form-item>

            <el-form-item label="最大Token数">
              <el-input-number
                v-model="maxTokensValue"
                :min="100"
                :max="8000"
                :step="100"
              />
            </el-form-item>

            <el-form-item label="批处理大小">
              <el-input-number
                v-model="batchSizeValue"
                :min="10"
                :max="200"
                :step="10"
              />
              <span class="form-tip">每批发送给大模型的数据量</span>
            </el-form-item>

            <el-alert
              title="常用 API 地址"
              type="info"
              :closable="false"
            >
              <div style="line-height: 1.8;">
                <div><strong>OpenAI:</strong> https://api.openai.com/v1/chat/completions</div>
                <div><strong>通义千问:</strong> https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions</div>
                <div><strong>文心一言:</strong> https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions</div>
              </div>
            </el-alert>
          </el-form>
        </el-tab-pane>

        <!-- 自学习配置 -->
        <el-tab-pane label="自学习配置" name="learning">
          <el-form :model="configs" label-width="220px" class="config-form">
            <el-form-item label="启用自学习">
              <el-switch v-model="selfLearningEnabled" />
              <span class="form-tip">系统自动发现和学习新规则</span>
            </el-form-item>

            <el-form-item label="自动添加规则阈值">
              <el-slider
                v-model="autoAddThresholdValue"
                :min="0.7"
                :max="1"
                :step="0.05"
                show-input
                :input-size="'small'"
              />
              <span class="form-tip">置信度超过此值自动添加规则</span>
            </el-form-item>

            <el-form-item label="自动应用清洗阈值">
              <el-slider
                v-model="autoApplyThresholdValue"
                :min="0.8"
                :max="1"
                :step="0.05"
                show-input
                :input-size="'small'"
              />
              <span class="form-tip">置信度超过此值自动执行清洗</span>
            </el-form-item>

            <el-divider content-position="left">定时 AI 发现</el-divider>

            <el-form-item label="启用定时 AI 发现">
              <el-switch v-model="scheduledDiscoveryEnabled" />
              <span class="form-tip">启用后系统将自动定期扫描发现脏数据</span>
            </el-form-item>

            <el-form-item label="执行时间" v-if="scheduledDiscoveryEnabled">
              <el-select v-model="scheduledPreset" placeholder="请选择执行时间" style="width: 300px">
                <el-option label="每天凌晨 2:00" value="daily_2am" />
                <el-option label="每天凌晨 3:00" value="daily_3am" />
                <el-option label="每周日凌晨 2:00" value="weekly_sunday" />
                <el-option label="每周一凌晨 2:00" value="weekly_monday" />
                <el-option label="每月1号凌晨 2:00" value="monthly_1st" />
                <el-option label="自定义 Cron 表达式" value="custom" />
              </el-select>
            </el-form-item>

            <el-form-item label="Cron 表达式" v-if="scheduledDiscoveryEnabled && scheduledPreset === 'custom'">
              <el-input 
                v-model="configs['ai_discovery.scheduled.cron']" 
                placeholder="0 0 2 * * ?"
                style="width: 300px"
              />
              <span class="form-tip">
                <a href="https://cron.qqe2.com/" target="_blank" style="color: #409eff">Cron 表达式生成器</a>
              </span>
            </el-form-item>

            <el-form-item label="最小执行间隔" v-if="scheduledDiscoveryEnabled">
              <el-input-number
                v-model="minIntervalHoursValue"
                :min="1"
                :max="24"
              />
              <span style="margin-left: 10px">小时</span>
              <span class="form-tip">防止频繁执行，保护系统资源</span>
            </el-form-item>

            <el-form-item label="单次最大扫描字段数" v-if="scheduledDiscoveryEnabled">
              <el-input-number
                v-model="maxFieldsPerRunValue"
                :min="10"
                :max="200"
                :step="10"
              />
              <span class="form-tip">限制单次扫描范围，避免执行时间过长</span>
            </el-form-item>

            <el-alert
              v-if="scheduledDiscoveryEnabled"
              title="注意事项"
              type="warning"
              :closable="false"
            >
              <div style="line-height: 1.8;">
                <div>• 定时任务会消耗大模型 API 配额，请合理设置执行频率</div>
                <div>• 建议在业务低峰期执行（如凌晨）</div>
                <div>• 首次使用建议先手动执行测试</div>
              </div>
            </el-alert>
          </el-form>
        </el-tab-pane>

        <!-- 安全配置 -->
        <el-tab-pane label="安全配置" name="safety">
          <el-form :model="configs" label-width="220px" class="config-form">
            <el-form-item label="单次自动清洗最大记录数">
              <el-input-number
                v-model="maxAutoCleanValue"
                :min="100"
                :max="10000"
                :step="100"
              />
              <span class="form-tip">防止误操作影响过多数据</span>
            </el-form-item>

            <el-form-item label="人工确认阈值">
              <el-input-number
                v-model="manualConfirmValue"
                :min="10"
                :max="1000"
                :step="10"
              />
              <span class="form-tip">影响记录数超过此值需人工确认</span>
            </el-form-item>

            <el-divider content-position="left">定时扫描脏数据</el-divider>

            <el-form-item label="启用定时扫描">
              <el-switch v-model="scheduledScanEnabled" />
              <span class="form-tip">启用后系统将自动定期扫描脏数据</span>
            </el-form-item>

            <el-form-item label="执行时间" v-if="scheduledScanEnabled">
              <el-select v-model="scheduledScanPreset" placeholder="请选择执行时间" style="width: 300px">
                <el-option label="每天凌晨 2:00" value="daily_2am" />
                <el-option label="每天凌晨 3:00" value="daily_3am" />
                <el-option label="每周日凌晨 2:00" value="weekly_sunday" />
                <el-option label="每周一凌晨 2:00" value="weekly_monday" />
                <el-option label="每月1号凌晨 2:00" value="monthly_1st" />
                <el-option label="自定义 Cron 表达式" value="custom" />
              </el-select>
            </el-form-item>

            <el-form-item label="Cron 表达式" v-if="scheduledScanEnabled && scheduledScanPreset === 'custom'">
              <el-input 
                v-model="configs['scan.scheduled.cron']" 
                placeholder="0 0 2 * * ?"
                style="width: 300px"
              />
              <span class="form-tip">
                <a href="https://cron.qqe2.com/" target="_blank" style="color: #409eff">Cron 表达式生成器</a>
              </span>
            </el-form-item>

            <el-form-item label="最小执行间隔" v-if="scheduledScanEnabled">
              <el-input-number
                v-model="scanMinIntervalHoursValue"
                :min="1"
                :max="24"
              />
              <span style="margin-left: 10px">小时</span>
              <span class="form-tip">防止频繁执行，保护系统资源</span>
            </el-form-item>

            <el-alert
              v-if="scheduledScanEnabled"
              title="注意事项"
              type="warning"
              :closable="false"
            >
              <div style="line-height: 1.8;">
                <div>• 定时扫描会查询目标数据库，请合理设置执行频率</div>
                <div>• 建议在业务低峰期执行（如凌晨）</div>
                <div>• 扫描结果会保存到数据库，可在清洗执行页面查看</div>
              </div>
            </el-alert>
          </el-form>
        </el-tab-pane>

        <!-- AI 提示词配置 -->
        <el-tab-pane label="AI 提示词" name="prompt">
          <el-alert
            title="说明"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 20px"
          >
            自定义 AI 发现功能的提示词模板，留空则使用默认提示词。支持占位符：{{tableName}}, {{columnName}}, {{columnDescription}}, {{existingRules}}, {{values}}
          </el-alert>

          <el-form :model="configs" label-width="120px" class="config-form">
            <el-form-item label="提示词模板">
              <el-input
                v-model="configs['ai_discovery.prompt_template']"
                type="textarea"
                :rows="20"
                placeholder="留空使用默认提示词"
                style="font-family: monospace"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleGetDefaultPrompt" :loading="loadingPrompt">
                查看默认提示词
              </el-button>
              <el-button @click="handleResetPrompt" :loading="resettingPrompt">
                重置为默认
              </el-button>
            </el-form-item>
          </el-form>

          <el-alert
            title="占位符说明"
            type="info"
            :closable="false"
          >
            <div style="line-height: 1.8;">
              <div><strong>{{tableName}}</strong> - 表名</div>
              <div><strong>{{columnName}}</strong> - 字段名</div>
              <div><strong>{{columnDescription}}</strong> - 字段描述</div>
              <div><strong>{{existingRules}}</strong> - 已有清洗规则列表</div>
              <div><strong>{{values}}</strong> - 字段的唯一值列表</div>
            </div>
          </el-alert>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 数据库选择对话框 -->
    <el-dialog
      v-model="showDatabaseDialog"
      title="选择数据库"
      width="500px"
    >
      <div class="database-list">
        <div
          v-for="db in databaseList"
          :key="db"
          class="database-item"
          @click="selectDatabase(db)"
        >
          <el-icon><Coin /></el-icon>
          <span>{{ db }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDatabaseDialog = false">取消</el-button>
      </template>
    </el-dialog>
  </Layout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh, Search, Connection, Coin } from '@element-plus/icons-vue'
import { getAllConfigs, batchUpdateConfigs, initDefaultConfigs } from '@/api/config'
import { testConnection, getDatabaseList } from '@/api/database'
import type { SystemConfig } from '@/api/config'
import Layout from '@/components/Layout.vue'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const loadingDatabases = ref(false)
const loadingPrompt = ref(false)
const resettingPrompt = ref(false)
const showDatabaseDialog = ref(false)
const activeTab = ref('database')
const configList = ref<SystemConfig[]>([])
const configs = ref<Record<string, string>>({})
const databaseList = ref<string[]>([])

// 数值型配置的计算属性
const targetDbPort = computed({
  get: () => parseInt(configs.value['target.db.port'] || '5432'),
  set: (val) => configs.value['target.db.port'] = val.toString()
})

const temperatureValue = computed({
  get: () => parseFloat(configs.value['llm.temperature'] || '0.3'),
  set: (val) => configs.value['llm.temperature'] = val.toString()
})

const maxTokensValue = computed({
  get: () => parseInt(configs.value['llm.max_tokens'] || '2000'),
  set: (val) => configs.value['llm.max_tokens'] = val.toString()
})

const batchSizeValue = computed({
  get: () => parseInt(configs.value['llm.batch_size'] || '50'),
  set: (val) => configs.value['llm.batch_size'] = val.toString()
})

const selfLearningEnabled = computed({
  get: () => configs.value['self_learning.enabled'] === 'true',
  set: (val) => configs.value['self_learning.enabled'] = val.toString()
})

const autoAddThresholdValue = computed({
  get: () => parseFloat(configs.value['self_learning.auto_add_threshold'] || '0.95'),
  set: (val) => configs.value['self_learning.auto_add_threshold'] = val.toString()
})

const autoApplyThresholdValue = computed({
  get: () => parseFloat(configs.value['self_learning.auto_apply_threshold'] || '0.98'),
  set: (val) => configs.value['self_learning.auto_apply_threshold'] = val.toString()
})

const maxAutoCleanValue = computed({
  get: () => parseInt(configs.value['safety.max_auto_clean_records'] || '1000'),
  set: (val) => configs.value['safety.max_auto_clean_records'] = val.toString()
})

const manualConfirmValue = computed({
  get: () => parseInt(configs.value['safety.manual_confirm_threshold'] || '100'),
  set: (val) => configs.value['safety.manual_confirm_threshold'] = val.toString()
})

// 定时扫描配置
const scheduledScanEnabled = computed({
  get: () => configs.value['scan.scheduled.enabled'] === 'true',
  set: (val) => configs.value['scan.scheduled.enabled'] = val.toString()
})

const scheduledScanPreset = computed({
  get: () => configs.value['scan.scheduled.preset'] || 'disabled',
  set: (val) => configs.value['scan.scheduled.preset'] = val
})

const scanMinIntervalHoursValue = computed({
  get: () => parseInt(configs.value['scan.scheduled.min_interval_hours'] || '6'),
  set: (val) => configs.value['scan.scheduled.min_interval_hours'] = val.toString()
})

// 定时 AI 发现配置
const scheduledDiscoveryEnabled = computed({
  get: () => configs.value['ai_discovery.scheduled.enabled'] === 'true',
  set: (val) => configs.value['ai_discovery.scheduled.enabled'] = val.toString()
})

const scheduledPreset = computed({
  get: () => configs.value['ai_discovery.scheduled.preset'] || 'disabled',
  set: (val) => configs.value['ai_discovery.scheduled.preset'] = val
})

const minIntervalHoursValue = computed({
  get: () => parseInt(configs.value['ai_discovery.scheduled.min_interval_hours'] || '6'),
  set: (val) => configs.value['ai_discovery.scheduled.min_interval_hours'] = val.toString()
})

const maxFieldsPerRunValue = computed({
  get: () => parseInt(configs.value['ai_discovery.scheduled.max_fields_per_run'] || '50'),
  set: (val) => configs.value['ai_discovery.scheduled.max_fields_per_run'] = val.toString()
})

onMounted(async () => {
  await loadConfigs()
  // 如果配置为空，初始化默认配置
  if (configList.value.length === 0) {
    await initConfigs()
  }
})

async function loadConfigs() {
  loading.value = true
  try {
    configList.value = await getAllConfigs() as any

    // 转换为 key-value 对象
    configs.value = {}
    configList.value.forEach(config => {
      configs.value[config.configKey] = config.configValue || ''
    })
  } catch (error) {
    console.error('加载配置失败', error)
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

async function initConfigs() {
  try {
    await initDefaultConfigs()
    ElMessage.success('默认配置初始化成功')
    await loadConfigs()
  } catch (error) {
    console.error('初始化配置失败', error)
  }
}

async function handleSave() {
  saving.value = true
  try {
    await batchUpdateConfigs(configs.value)
    ElMessage.success('配置保存成功')
    await loadConfigs()
  } catch (error) {
    console.error('保存配置失败', error)
    ElMessage.error('保存配置失败')
  } finally {
    saving.value = false
  }
}

async function handleTestConnection() {
  const host = configs.value['target.db.host'] || 'localhost'
  const port = configs.value['target.db.port'] || '5432'
  const database = configs.value['target.db.database']
  const username = configs.value['target.db.username']
  const password = configs.value['target.db.password']

  if (!database || !username || !password) {
    ElMessage.warning('请填写完整的数据库连接信息')
    return
  }

  testing.value = true
  try {
    const result = await testConnection({
      host,
      port,
      database,
      username,
      password
    }) as any

    if (result.success) {
      ElMessage.success('数据库连接成功！')
    } else {
      ElMessage.error('数据库连接失败')
    }
  } catch (error) {
    console.error('测试连接失败', error)
    ElMessage.error('数据库连接失败，请检查配置')
  } finally {
    testing.value = false
  }
}

async function handleLoadDatabases() {
  const host = configs.value['target.db.host'] || 'localhost'
  const port = configs.value['target.db.port'] || '5432'
  const username = configs.value['target.db.username']
  const password = configs.value['target.db.password']

  if (!username || !password) {
    ElMessage.warning('请先填写用户名和密码')
    return
  }

  loadingDatabases.value = true
  try {
    databaseList.value = await getDatabaseList({
      host,
      port,
      username,
      password
    }) as any

    if (databaseList.value.length > 0) {
      showDatabaseDialog.value = true
    } else {
      ElMessage.warning('未找到任何数据库')
    }
  } catch (error) {
    console.error('获取数据库列表失败', error)
    ElMessage.error('获取数据库列表失败，请检查连接信息')
  } finally {
    loadingDatabases.value = false
  }
}

function selectDatabase(dbName: string) {
  configs.value['target.db.database'] = dbName
  showDatabaseDialog.value = false
  ElMessage.success(`已选择数据库: ${dbName}`)
}

async function handleGetDefaultPrompt() {
  loadingPrompt.value = true
  try {
    const response = await fetch('/api/system/config/default-prompt')
    const data = await response.json()
    
    ElMessage.success('已加载默认提示词')
    configs.value['ai_discovery.prompt_template'] = data.template
  } catch (error) {
    console.error('获取默认提示词失败', error)
    ElMessage.error('获取默认提示词失败')
  } finally {
    loadingPrompt.value = false
  }
}

async function handleResetPrompt() {
  resettingPrompt.value = true
  try {
    const response = await fetch('/api/system/config/reset-prompt', {
      method: 'POST'
    })
    const data = await response.json()
    
    if (data.success) {
      ElMessage.success('提示词已重置为默认值')
      configs.value['ai_discovery.prompt_template'] = ''
    }
  } catch (error) {
    console.error('重置提示词失败', error)
    ElMessage.error('重置提示词失败')
  } finally {
    resettingPrompt.value = false
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

.config-card {
  border-radius: 8px;
}

.config-form {
  max-width: 800px;
  padding: 20px 0;
}

.form-tip {
  display: block;
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
}

:deep(.el-form-item) {
  margin-bottom: 28px;
}

:deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
}

:deep(.el-alert) {
  margin-top: 20px;
}

.database-list {
  max-height: 400px;
  overflow-y: auto;
}

.database-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #eee;
  transition: background-color 0.2s;
}

.database-item:hover {
  background-color: #f5f7fa;
}

.database-item:last-child {
  border-bottom: none;
}

.database-item .el-icon {
  color: #409eff;
  font-size: 18px;
}
</style>
