import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/rules'
  },
  {
    path: '/rules',
    name: 'RuleManagement',
    component: () => import('@/views/RuleManagement.vue'),
    meta: { title: '规则管理' }
  },
  {
    path: '/discovery',
    name: 'AIDiscovery',
    component: () => import('@/views/AIDiscovery.vue'),
    meta: { title: 'AI 发现' }
  },
  {
    path: '/execution',
    name: 'CleaningExecution',
    component: () => import('@/views/CleaningExecution.vue'),
    meta: { title: '清洗执行' }
  },
  {
    path: '/logs',
    name: 'CleaningLogs',
    component: () => import('@/views/CleaningLogs.vue'),
    meta: { title: '清洗日志' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
