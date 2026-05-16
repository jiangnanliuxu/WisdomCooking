<script setup lang="ts">
import {
  Bell,
  Collection,
  ChatDotRound,
  Cpu,
  DataAnalysis,
  Files,
  Grid,
  MessageBox,
  Picture,
  Setting,
  SwitchButton,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDashboardSummary } from '@/api/operation'
import { useAuthStore } from '@/stores/auth'
import type { AdminDashboardSummary } from '@/types/cook'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const summary = ref<AdminDashboardSummary>()

const menuGroups = computed(() => [
  {
    title: '概览',
    items: [
      { title: '仪表盘', path: '/dashboard', icon: DataAnalysis },
    ],
  },
  {
    title: '内容管理',
    items: [
      { title: '菜谱管理', path: '/recipes', icon: Files },
      { title: '菜谱分类', path: '/categories', icon: Setting },
      { title: '轮播图管理', path: '/banners', icon: Picture },
    ],
  },
  {
    title: '社区社交',
    items: [
      { title: '菜谱审核', path: '/recipe-audits', icon: Grid, badge: summary.value?.pendingRecipeCount || 0 },
      { title: '动态审核', path: '/post-audits', icon: ChatDotRound, badge: summary.value?.pendingPostCount || 0 },
      { title: '群组管理', path: '/groups', icon: UserFilled },
      { title: '用户管理', path: '/users', icon: User },
      { title: '用户反馈', path: '/feedbacks', icon: MessageBox, badge: summary.value?.processingFeedbackCount || 0 },
    ],
  },
  {
    title: 'AI运维',
    items: [
      { title: '模型配置', path: '/ai/models', icon: Cpu },
      { title: 'AI知识库', path: '/ai/knowledge', icon: Collection },
      { title: '对话日志', path: '/ai/conversations', icon: ChatDotRound },
      { title: 'AI识图日志', path: '/ai/recognitions', icon: Picture },
    ],
  },
])

const flatMenus = computed(() => menuGroups.value.flatMap(group => group.items))
const activeMenu = computed(() => route.path)
const currentGroup = computed(() => menuGroups.value.find(group => group.items.some(item => item.path === route.path)))
const currentMenu = computed(() => flatMenus.value.find(item => item.path === route.path))
const avatarText = computed(() => (authStore.profile?.nickName || authStore.profile?.userName || 'A').slice(0, 1).toUpperCase())
const pendingCount = computed(() => (summary.value?.pendingRecipeCount || 0) + (summary.value?.pendingPostCount || 0))

async function loadSummary() {
  try {
    const response = await getDashboardSummary()
    summary.value = response.data
  }
  catch {
    summary.value = undefined
  }
}

async function handleLogout() {
  await authStore.logoutCurrent()
  router.replace('/login')
}

onMounted(loadSummary)
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-sidebar__logo">
        <div class="admin-sidebar__logo-icon">
          🍳
        </div>
        <div class="admin-sidebar__logo-copy">
          <strong>码上智厨</strong>
          <span>Admin Console</span>
        </div>
      </div>

      <el-scrollbar class="admin-sidebar__scroll">
        <div
          v-for="group in menuGroups"
          :key="group.title"
          class="admin-sidebar__group"
        >
          <div class="admin-sidebar__group-title">
            {{ group.title }}
          </div>
          <button
            v-for="item in group.items"
            :key="item.path"
            type="button"
            class="admin-sidebar__item"
            :class="{ 'admin-sidebar__item--active': activeMenu === item.path }"
            @click="router.push(item.path)"
          >
            <span class="admin-sidebar__icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <span class="admin-sidebar__label">{{ item.title }}</span>
            <span v-if="item.badge" class="admin-sidebar__badge">{{ item.badge }}</span>
          </button>
        </div>
      </el-scrollbar>
    </aside>

    <section class="admin-main">
      <header class="admin-header">
        <div class="admin-header__left">
          <div class="admin-header__breadcrumb">
            {{ currentGroup?.title || '管理后台' }} /
            <span>{{ currentMenu?.title || route.meta.title || '控制台' }}</span>
          </div>
        </div>

        <div class="admin-header__right">
          <button type="button" class="admin-header__notice">
            <el-icon size="18">
              <Bell />
            </el-icon>
            <span v-if="pendingCount" class="admin-header__notice-dot" />
          </button>

          <div class="admin-header__profile">
            <div class="admin-header__avatar">
              {{ avatarText }}
            </div>
            <div class="admin-header__profile-copy">
              <strong>{{ authStore.profile?.nickName || authStore.profile?.userName || 'Admin' }}</strong>
              <span>超级管理员</span>
            </div>
          </div>

          <el-button class="admin-header__logout" text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出</span>
          </el-button>
        </div>
      </header>

      <div class="admin-content">
        <main class="admin-page">
          <router-view />
        </main>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background: var(--admin-bg);
}

.admin-sidebar {
  width: var(--admin-sidebar-width);
  min-height: 100vh;
  background: var(--admin-sidebar);
  color: #ffffff;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  box-shadow: 10px 0 30px rgba(0, 10, 30, 0.18);
}

.admin-sidebar__logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-sidebar__logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: linear-gradient(135deg, #ff6b35, #ff8f5e);
  box-shadow: 0 10px 24px rgba(255, 107, 53, 0.3);
}

.admin-sidebar__logo-copy {
  display: grid;
  gap: 2px;
}

.admin-sidebar__logo-copy strong {
  font-size: 16px;
  line-height: 1;
}

.admin-sidebar__logo-copy span {
  font-size: 11px;
  line-height: 1.2;
  color: rgba(255, 255, 255, 0.45);
}

.admin-sidebar__scroll {
  flex: 1;
}

.admin-sidebar__group {
  padding: 8px 0;
}

.admin-sidebar__group-title {
  padding: 16px 20px 6px;
  font-size: 11px;
  color: var(--admin-sidebar-muted);
  letter-spacing: 1px;
  text-transform: uppercase;
}

.admin-sidebar__item {
  width: 100%;
  border: none;
  background: transparent;
  color: var(--admin-sidebar-text);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  font-size: 14px;
  cursor: pointer;
  border-left: 3px solid transparent;
  text-align: left;
  transition: all 0.2s ease;
}

.admin-sidebar__item:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.06);
}

.admin-sidebar__item--active {
  color: #ffffff;
  background: rgba(255, 107, 53, 0.15);
  border-left-color: var(--admin-primary);
}

.admin-sidebar__icon {
  width: 20px;
  display: inline-flex;
  justify-content: center;
  font-size: 18px;
}

.admin-sidebar__label {
  flex: 1;
}

.admin-sidebar__badge {
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.admin-main {
  margin-left: var(--admin-sidebar-width);
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-header {
  height: var(--admin-header-height);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 50;
}

.admin-header__breadcrumb {
  font-size: 14px;
  color: var(--admin-text-light);
}

.admin-header__breadcrumb span {
  color: var(--admin-text-main);
  font-weight: 600;
}

.admin-header__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-header__notice {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: var(--admin-primary-soft);
  color: var(--admin-primary);
  cursor: pointer;
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.admin-header__notice-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff4d4f;
}

.admin-header__profile {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-header__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b35, #ff8f5e);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}

.admin-header__profile-copy {
  display: grid;
  gap: 2px;
}

.admin-header__profile-copy strong {
  font-size: 14px;
  color: var(--admin-text-main);
  line-height: 1;
}

.admin-header__profile-copy span {
  font-size: 12px;
  color: var(--admin-text-light);
}

.admin-header__logout {
  gap: 6px;
  color: var(--admin-text-secondary);
}

.admin-content {
  padding: 24px;
}

.admin-page {
  min-height: calc(100vh - var(--admin-header-height) - 48px);
}
</style>
