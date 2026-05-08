<template>
  <Layout>
    <div class="page-header">
      <h2>清洗执行</h2>
    </div>

    <el-alert
      title="数据清洗执行"
      type="warning"
      description="请谨慎执行数据清洗操作，建议先预览影响范围。"
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
