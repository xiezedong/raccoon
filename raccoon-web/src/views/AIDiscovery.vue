<template>
  <Layout>
    <div class="page-header">
      <h2>AI 发现</h2>
    </div>

    <el-alert
      title="AI 辅助发现功能"
      type="info"
      description="系统将利用大模型自动发现数据库中可能的脏数据，并生成候选规则供您审核。"
      :closable="false"
      style="margin-bottom: 20px"
    />

    <el-empty description="功能开发中..." />
  </Layout>
</template>

<script setup lang="ts">
import Layout from '@/components/Layout.vue'
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}
</style>
