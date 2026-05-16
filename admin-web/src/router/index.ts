import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '管理端登录', public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '仪表盘' } },
        { path: 'recipes', name: 'recipes', component: () => import('@/views/RecipeListView.vue'), meta: { title: '菜谱管理' } },
        { path: 'recipe-audits', name: 'recipe-audits', component: () => import('@/views/RecipeAuditView.vue'), meta: { title: '菜谱审核' } },
        { path: 'post-audits', name: 'post-audits', component: () => import('@/views/PostAuditView.vue'), meta: { title: '动态审核' } },
        { path: 'categories', name: 'categories', component: () => import('@/views/CategoryView.vue'), meta: { title: '菜谱分类' } },
        { path: 'ai/models', name: 'ai-models', component: () => import('@/views/AiModelsView.vue'), meta: { title: 'AI 模型配置' } },
        { path: 'ai/knowledge', name: 'ai-knowledge', component: () => import('@/views/AiKnowledgeView.vue'), meta: { title: 'AI 知识库' } },
        { path: 'ai/conversations', name: 'ai-conversations', component: () => import('@/views/AiConversationLogsView.vue'), meta: { title: 'AI 对话日志' } },
        { path: 'ai/recognitions', name: 'ai-recognitions', component: () => import('@/views/AiRecognitionLogsView.vue'), meta: { title: 'AI 识图日志' } },
        { path: 'users', name: 'users', component: () => import('@/views/UserManagementView.vue'), meta: { title: '用户管理' } },
        { path: 'groups', name: 'groups', component: () => import('@/views/GroupManagementView.vue'), meta: { title: '群组管理' } },
        { path: 'feedbacks', name: 'feedbacks', component: () => import('@/views/FeedbackManagementView.vue'), meta: { title: '用户反馈' } },
        { path: 'banners', name: 'banners', component: () => import('@/views/BannerManagementView.vue'), meta: { title: '轮播图管理' } },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  const appStore = useAppStore()

  if (to.meta.public) {
    return true
  }

  if (!authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!authStore.initialized) {
    try {
      await authStore.fetchProfile()
    }
    catch {
      authStore.clear()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  appStore.addVisitedTab({
    title: String(to.meta.title || to.name || '未命名页面'),
    path: to.fullPath,
  })
  return true
})

export default router
