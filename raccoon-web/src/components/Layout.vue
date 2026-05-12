<template>
  <div class="layout-container">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside width="260px" class="sidebar">
        <div class="logo-section">
          <img src="@/assets/logo.png" alt="Raccoon" class="logo" />
        </div>
        
        <nav class="nav-menu">
          <router-link 
            v-for="item in menuItems" 
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: $route.path === item.path }"
          >
            <component :is="item.icon" class="nav-icon" />
            <span class="nav-text">{{ item.label }}</span>
          </router-link>
        </nav>

        <!-- 页脚 -->
        <div class="sidebar-footer">
          <div class="powered-by">
            Powered by
          </div>
          <div class="team-members">
            <span class="member">李佳潞</span>
            <span class="separator">·</span>
            <span class="member">谢泽东</span>
            <span class="separator">·</span>
            <span class="member">叶紫薇</span>
          </div>
          <div class="version">v1.0.0</div>
        </div>
      </el-aside>

      <!-- 主内容区 -->
      <el-container class="main-container">
        <!-- 顶部面包屑 -->
        <el-header height="60px" class="header">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </el-header>

        <!-- 内容区 -->
        <el-main class="content">
          <slot />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Document, Search, Operation, List, Setting } from '@element-plus/icons-vue'

const route = useRoute()

const menuItems = [
  { path: '/rules', label: '规则管理', icon: Document },
  { path: '/discovery', label: 'AI 发现', icon: Search },
  { path: '/execution', label: '清洗执行', icon: Operation },
  { path: '/logs', label: '清洗日志', icon: List },
  { path: '/settings', label: '系统设置', icon: Setting }
]

const currentPageTitle = computed(() => {
  const item = menuItems.find(m => m.path === route.path)
  return item?.label || '未知页面'
})

</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
}

.el-container {
  height: 100%;
}

/* 侧边栏样式 */
.sidebar {
  background: linear-gradient(180deg, #1a1d29 0%, #252936 100%);
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 200px;
  background: radial-gradient(circle at 50% 0%, rgba(99, 102, 241, 0.15), transparent);
  pointer-events: none;
}

/* Logo 区域 */
.logo-section {
  padding: 40px 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
  z-index: 1;
}

.logo {
  width: 100%;
  max-width: 200px;
  height: auto;
  object-fit: contain;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.3));
  transition: transform 0.3s ease;
}

.logo:hover {
  transform: scale(1.05);
}

/* 导航菜单 */
.nav-menu {
  flex: 1;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
  z-index: 1;
  overflow-y: auto;
}

.nav-menu::-webkit-scrollbar {
  width: 4px;
}

.nav-menu::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.nav-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.nav-menu::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.15);
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.3px;
}

.nav-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, #6366f1, #8b5cf6);
  transform: scaleY(0);
  transition: transform 0.3s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  transform: translateX(4px);
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(139, 92, 246, 0.15));
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.2);
}

.nav-item.active::before {
  transform: scaleY(1);
}

.nav-icon {
  width: 20px;
  height: 20px;
  margin-right: 12px;
  transition: transform 0.3s ease;
}

.nav-item:hover .nav-icon {
  transform: scale(1.1);
}

.nav-text {
  flex: 1;
}

/* 页脚 */
.sidebar-footer {
  padding: 20px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
  position: relative;
  z-index: 1;
}

.powered-by {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 8px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.team-members {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.member {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  transition: all 0.3s ease;
}

.separator {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.3);
}

.version {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.3);
  font-family: 'Courier New', monospace;
}

/* 主容器 */
.main-container {
  display: flex;
  flex-direction: column;
  background: transparent;
}

/* 顶部面包屑 */
.header {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  padding: 0 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

:deep(.el-breadcrumb) {
  font-size: 14px;
}

:deep(.el-breadcrumb__item) {
  font-weight: 500;
}

:deep(.el-breadcrumb__inner) {
  color: #606266;
  transition: color 0.2s;
}

:deep(.el-breadcrumb__inner:hover) {
  color: #6366f1;
}

:deep(.el-breadcrumb__inner.is-link) {
  color: #909399;
}

/* 内容区 */
.content {
  padding: 32px;
  overflow-y: auto;
}

/* 滚动条样式 */
.content::-webkit-scrollbar {
  width: 8px;
}

.content::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 4px;
}

.content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  transition: background 0.2s;
}

.content::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.15);
}
</style>
