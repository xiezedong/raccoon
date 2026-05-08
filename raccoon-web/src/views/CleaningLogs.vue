<template>
  <Layout>
    <div class="page-header">
      <h2>清洗日志</h2>
    </div>

    <el-alert
      title="清洗日志"
      type="info"
      description="查看所有数据清洗操作的历史记录。"
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
