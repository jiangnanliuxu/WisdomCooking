<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { banUser, getAdminUser, listAdminUsers, muteUser, unblockUser } from '@/api/operation'
import PageHeader from '@/components/PageHeader.vue'
import type { AdminUser } from '@/types/cook'
import { formatDateTime } from '@/utils/format'
import { resolveAssetUrl } from '@/utils/media'

const loading = ref(false)
const rows = ref<AdminUser[]>([])
const detail = ref<AdminUser>()
const detailVisible = ref(false)
const page = reactive({ page: 1, pageSize: 10, total: 0 })
const filters = reactive({ status: '', keyword: '' })

const totalText = computed(() => `共 ${page.total} 条记录 / 第 ${page.page} 页`)

function avatarStyle(index: number) {
  const palette = [
    'linear-gradient(135deg,#ff6b35,#ff8f5e)',
    'linear-gradient(135deg,#722ed1,#b37feb)',
    'linear-gradient(135deg,#52c41a,#95de64)',
    'linear-gradient(135deg,#faad14,#ffc53d)',
    'linear-gradient(135deg,#1890ff,#69c0ff)',
  ]
  return { background: palette[index % palette.length] }
}

function avatarUrl(user?: AdminUser) {
  return resolveAssetUrl(user?.avatarUrl)
}

function statusClass(status?: string) {
  if (status === 'banned') return 'status-banned'
  if (status === 'muted') return 'status-muted'
  return 'status-normal'
}

function statusLabel(status?: string) {
  if (status === 'banned') return '已封禁'
  if (status === 'muted') return '已禁言'
  return '正常'
}

async function loadData() {
  loading.value = true
  try {
    const response = await listAdminUsers({ ...filters, page: page.page, pageSize: page.pageSize })
    rows.value = response.data?.items || []
    page.total = response.data?.total || 0
  }
  finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  const response = await getAdminUser(id)
  detail.value = response.data
  detailVisible.value = true
}

async function changeStatus(id: number, action: 'mute' | 'ban' | 'unblock') {
  const { value } = await ElMessageBox.prompt('请输入处理原因', '用户状态变更', { confirmButtonText: '确认' })
  if (action === 'mute') await muteUser(id, value)
  if (action === 'ban') await banUser(id, value)
  if (action === 'unblock') await unblockUser(id, value)
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <PageHeader title="用户管理" description="查看普通用户资料、内容统计，并执行禁言、封禁、解封等治理操作。" />
  <div class="page-toolbar">
    <div class="page-toolbar__filters">
      <el-input v-model="filters.keyword" style="width: 240px;" clearable placeholder="搜索用户名、手机号..." @keyup.enter="loadData" />
      <el-select v-model="filters.status" clearable style="width: 140px;" placeholder="全部状态">
        <el-option label="正常" value="normal" />
        <el-option label="禁言" value="muted" />
        <el-option label="封禁" value="banned" />
      </el-select>
      <el-button @click="loadData">搜索</el-button>
      <el-button @click="filters.keyword = ''; filters.status = ''; loadData()">重置</el-button>
    </div>
  </div>

  <el-card class="app-card">
    <el-table v-loading="loading" class="admin-table" :data="rows">
      <el-table-column label="用户信息" min-width="240">
        <template #default="{ row, $index }">
          <div class="user-info">
            <div class="user-avatar" :style="avatarStyle($index)">
              <el-image v-if="avatarUrl(row)" :src="avatarUrl(row)" fit="cover" class="user-avatar__image" />
              <span v-else>{{ (row.nickname || 'U').slice(0, 1) }}</span>
            </div>
            <div>
              <div class="user-name">{{ row.nickname || `用户${row.id}` }}</div>
              <div class="user-id">UID: {{ row.id }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column label="作品数" width="100">
        <template #default="{ row }">{{ (row.recipeCount || 0) + (row.postCount || 0) }}</template>
      </el-table-column>
      <el-table-column prop="followerCount" label="粉丝数" width="100" />
      <el-table-column label="注册时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <span class="status" :class="statusClass(row.status)">
            <span class="status-dot" />
            {{ statusLabel(row.status) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <div class="table-operations">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button link type="warning" @click="changeStatus(row.id, 'mute')">禁言</el-button>
            <el-button link type="danger" @click="changeStatus(row.id, 'ban')">封禁</el-button>
            <el-button link type="success" @click="changeStatus(row.id, 'unblock')">解封</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <div class="admin-pagination">
      <div class="pagination-info">{{ totalText }}</div>
      <el-pagination v-model:current-page="page.page" background layout="prev, pager, next" :total="page.total" @current-change="loadData" />
    </div>
  </el-card>

  <el-drawer v-model="detailVisible" class="admin-drawer" title="用户详情" size="560px">
    <template v-if="detail">
      <div class="detail-user">
        <div class="detail-user__avatar">
          <el-image v-if="avatarUrl(detail)" :src="avatarUrl(detail)" fit="cover" class="user-avatar__image" />
          <span v-else>{{ (detail.nickname || 'U').slice(0, 1) }}</span>
        </div>
        <div>
          <div class="detail-user__name">{{ detail.nickname || `用户${detail.id}` }}</div>
          <div class="detail-user__meta">{{ detail.phone }} · {{ statusLabel(detail.status) }}</div>
        </div>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="地区">{{ detail.region || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="菜谱数">{{ detail.recipeCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="动态数">{{ detail.postCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="粉丝数">{{ detail.followerCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
      </el-descriptions>
      <el-card class="app-card" style="margin-top: 16px;">
        <div style="font-weight: 600; margin-bottom: 8px;">个性签名</div>
        <div style="line-height: 1.8;">{{ detail.bio || '-' }}</div>
      </el-card>
    </template>
  </el-drawer>
</template>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar,
.detail-user__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
  font-weight: 600;
}

.user-avatar__image {
  width: 100%;
  height: 100%;
  border-radius: inherit;
}

.user-name,
.detail-user__name {
  font-weight: 600;
  color: #333;
}

.user-id,
.detail-user__meta,
.pagination-info {
  font-size: 12px;
  color: #999;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-normal .status-dot { background: #52c41a; }
.status-normal { color: #52c41a; }
.status-banned .status-dot { background: #ff4d4f; }
.status-banned { color: #ff4d4f; }
.status-muted .status-dot { background: #faad14; }
.status-muted { color: #faad14; }

.detail-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-user__avatar {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #ff6b35, #ff8f5e);
  box-shadow: 0 12px 24px rgba(255, 107, 53, 0.2);
}
</style>
