<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dissolveGroup, getAdminGroup, listAdminGroups } from '@/api/operation'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { AdminGroup } from '@/types/cook'
import { formatDateTime } from '@/utils/format'
import { mediaIdToRawUrl, resolveAssetUrl } from '@/utils/media'

const loading = ref(false)
const rows = ref<AdminGroup[]>([])
const detail = ref<AdminGroup>()
const detailVisible = ref(false)
const page = reactive({ page: 1, pageSize: 10, total: 0 })
const filters = reactive({ status: '', keyword: '' })
const dissolveVisible = ref(false)
const dissolveForm = reactive({ id: 0, name: '', reason: '' })

const dissolveReasonOptions = [
  '群内违规内容频发',
  '群聊长期无人维护',
  '涉嫌导流或广告传播',
  '多次警告后未整改',
]

const totalText = computed(() => `共 ${page.total} 条记录 / 第 ${page.page} 页`)
const warnedCount = computed(() => rows.value.filter(item => item.status === 'warned').length)
const dissolvedCount = computed(() => rows.value.filter(item => item.status === 'dissolved').length)
const memberTotal = computed(() => rows.value.reduce((sum, item) => sum + Number(item.memberCount || 0), 0))
const messageTotal = computed(() => rows.value.reduce((sum, item) => sum + Number(item.messageCount || 0), 0))

function avatarStyle(index: number) {
  const palette = [
    'linear-gradient(135deg,#ff6b35,#ff8f5e)',
    'linear-gradient(135deg,#722ed1,#b37feb)',
    'linear-gradient(135deg,#52c41a,#95de64)',
    'linear-gradient(135deg,#1890ff,#69c0ff)',
    'linear-gradient(135deg,#faad14,#ffc53d)',
  ]
  return { background: palette[index % palette.length] }
}

function groupInitial(name?: string) {
  return (name || '群').slice(0, 1)
}

function groupAvatarUrl(group?: AdminGroup) {
  return resolveAssetUrl(group?.avatarUrl) || mediaIdToRawUrl(group?.avatarMediaId)
}

function noticeSummary(notice?: string) {
  if (!notice) return '暂无群公告'
  return notice.length > 16 ? `${notice.slice(0, 16)}...` : notice
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function resetFilters() {
  filters.status = ''
  filters.keyword = ''
  page.page = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const response = await listAdminGroups({ ...filters, page: page.page, pageSize: page.pageSize })
    rows.value = response.data?.items || []
    page.total = response.data?.total || 0
  }
  finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  const response = await getAdminGroup(id)
  detail.value = response.data
  detailVisible.value = true
}

function openDissolve(row: AdminGroup) {
  dissolveForm.id = row.id
  dissolveForm.name = row.name || `群组 ${row.id}`
  dissolveForm.reason = ''
  dissolveVisible.value = true
}

async function submitDissolve() {
  if (!dissolveForm.reason.trim()) {
    ElMessage.warning('请输入解散原因')
    return
  }
  await dissolveGroup(dissolveForm.id, dissolveForm.reason.trim())
  dissolveVisible.value = false
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <PageHeader title="群组管理" description="查看群聊规模、风险状态和运营信息，并在需要时执行解散治理。" />

  <div class="stats-grid">
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--orange">👥</div>
      <div>
        <div class="stat-card__value">{{ page.total }}</div>
        <div class="stat-card__label">群组总数</div>
      </div>
    </article>
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--blue">🧑‍🤝‍🧑</div>
      <div>
        <div class="stat-card__value">{{ formatNumber(memberTotal) }}</div>
        <div class="stat-card__label">当前页成员总量</div>
      </div>
    </article>
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--pink">⚠️</div>
      <div>
        <div class="stat-card__value">{{ warnedCount }}</div>
        <div class="stat-card__label">已警告群组</div>
      </div>
    </article>
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--slate">💬</div>
      <div>
        <div class="stat-card__value">{{ formatNumber(messageTotal) }}</div>
        <div class="stat-card__label">当前页消息量</div>
      </div>
    </article>
  </div>

  <div class="page-toolbar">
    <div class="page-toolbar__filters">
      <el-select v-model="filters.status" clearable style="width: 160px;" placeholder="群组状态">
        <el-option label="正常" value="normal" />
        <el-option label="警告" value="warned" />
        <el-option label="已解散" value="dissolved" />
      </el-select>
      <el-input v-model="filters.keyword" style="width: 260px;" clearable placeholder="搜索群名或群主昵称" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>
    <div class="toolbar-note">
      <span>已解散</span>
      <strong>{{ dissolvedCount }}</strong>
    </div>
  </div>

  <el-card class="app-card">
    <el-table v-loading="loading" class="admin-table" :data="rows">
      <el-table-column label="群组信息" min-width="280">
        <template #default="{ row, $index }">
          <div class="group-info">
            <div class="group-avatar" :style="avatarStyle($index)">
              <el-image v-if="groupAvatarUrl(row)" :src="groupAvatarUrl(row)" fit="cover" class="group-avatar__image" />
              <span v-else>{{ groupInitial(row.name) }}</span>
            </div>
            <div class="group-copy">
              <div class="group-name">{{ row.name || `群组 ${row.id}` }}</div>
              <div class="group-meta">G{{ row.id }} · 会话 {{ row.conversationId || '-' }}</div>
              <div class="group-intro">{{ row.intro || '暂无群简介' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="群主 / 公告" min-width="210">
        <template #default="{ row }">
          <div class="owner-cell">
            <div class="owner-cell__name">{{ row.ownerNickname || '-' }}</div>
            <div class="owner-cell__notice">{{ noticeSummary(row.notice) }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="成员数" width="110">
        <template #default="{ row }">{{ formatNumber(row.memberCount) }}</template>
      </el-table-column>
      <el-table-column label="消息数" width="110">
        <template #default="{ row }">{{ formatNumber(row.messageCount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <StatusTag :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <div class="table-operations">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button link type="danger" :disabled="row.status === 'dissolved'" @click="openDissolve(row)">解散</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <div class="admin-pagination">
      <div class="pagination-info">{{ totalText }}</div>
      <el-pagination v-model:current-page="page.page" background layout="prev, pager, next" :total="page.total" @current-change="loadData" />
    </div>
  </el-card>

  <el-drawer v-model="detailVisible" class="admin-drawer" title="群组详情" size="620px">
    <template v-if="detail">
      <div class="detail-hero">
        <div class="detail-hero__avatar">
          <el-image v-if="groupAvatarUrl(detail)" :src="groupAvatarUrl(detail)" fit="cover" class="group-avatar__image" />
          <span v-else>{{ groupInitial(detail.name) }}</span>
        </div>
        <div class="detail-hero__copy">
          <div class="detail-hero__title-row">
            <h3>{{ detail.name || `群组 ${detail.id}` }}</h3>
            <StatusTag :value="detail.status" />
          </div>
          <div class="detail-hero__meta">
            G{{ detail.id }} · 群主 {{ detail.ownerNickname || '-' }} · 创建于 {{ formatDateTime(detail.createdAt) }}
          </div>
          <p>{{ detail.intro || '暂无群简介' }}</p>
        </div>
      </div>

      <div class="detail-metrics">
        <article>
          <span>成员规模</span>
          <strong>{{ formatNumber(detail.memberCount) }}</strong>
        </article>
        <article>
          <span>消息规模</span>
          <strong>{{ formatNumber(detail.messageCount) }}</strong>
        </article>
        <article>
          <span>会话 ID</span>
          <strong>{{ detail.conversationId || '-' }}</strong>
        </article>
      </div>

      <el-card class="app-card detail-card">
        <div class="detail-card__title">群公告</div>
        <div class="detail-card__content">{{ detail.notice || '暂无群公告' }}</div>
      </el-card>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="群主昵称">{{ detail.ownerNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="群主 ID">{{ detail.ownerId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="群组状态"><StatusTag :value="detail.status" /></el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </el-drawer>

  <el-dialog v-model="dissolveVisible" class="admin-dialog" title="解散群组" width="520px">
    <div class="dialog-hint">
      该操作会将群组状态更新为已解散，建议记录具体原因，便于后续申诉和审计。
    </div>
    <div class="reason-list">
      <button
        v-for="item in dissolveReasonOptions"
        :key="item"
        type="button"
        class="reason-chip"
        @click="dissolveForm.reason = item"
      >
        {{ item }}
      </button>
    </div>
    <el-form label-position="top">
      <el-form-item label="群组名称">
        <el-input :model-value="dissolveForm.name" disabled />
      </el-form-item>
      <el-form-item label="解散原因">
        <el-input v-model="dissolveForm.reason" type="textarea" :rows="5" placeholder="请输入后台记录的解散原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dissolveVisible = false">取消</el-button>
      <el-button type="primary" @click="submitDissolve">确认解散</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.stat-card__icon--orange { background: #fff7e6; }
.stat-card__icon--blue { background: #e6f7ff; }
.stat-card__icon--pink { background: #fff0f6; }
.stat-card__icon--slate { background: #f5f7fa; }

.stat-card__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--admin-text-main);
}

.stat-card__label,
.pagination-info,
.toolbar-note span {
  font-size: 12px;
  color: var(--admin-text-light);
}

.toolbar-note {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.toolbar-note strong {
  color: var(--admin-text-main);
}

.group-info,
.detail-hero,
.detail-hero__title-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.group-avatar,
.detail-hero__avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}

.group-avatar__image {
  width: 100%;
  height: 100%;
}

.group-copy {
  min-width: 0;
}

.group-name,
.owner-cell__name {
  font-weight: 600;
  color: var(--admin-text-main);
}

.group-meta,
.group-intro,
.owner-cell__notice,
.detail-hero__meta {
  font-size: 12px;
  line-height: 1.7;
  color: var(--admin-text-light);
}

.owner-cell {
  display: grid;
  gap: 4px;
}

.detail-hero {
  margin-bottom: 18px;
}

.detail-hero__avatar {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #ff6b35, #ff8f5e);
  box-shadow: 0 14px 28px rgba(255, 107, 53, 0.18);
}

.detail-hero__copy {
  min-width: 0;
  flex: 1;
}

.detail-hero__copy h3 {
  margin: 0;
  font-size: 20px;
  color: var(--admin-text-main);
}

.detail-hero__copy p,
.detail-card__content {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--admin-text-secondary);
}

.detail-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.detail-metrics article {
  padding: 14px 16px;
  border-radius: 12px;
  background: #fafafa;
  display: grid;
  gap: 6px;
}

.detail-metrics span,
.detail-card__title,
.dialog-hint {
  font-size: 12px;
  color: var(--admin-text-light);
}

.detail-metrics strong {
  font-size: 18px;
  color: var(--admin-text-main);
}

.detail-card {
  margin-bottom: 18px;
}

.reason-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0;
}

.reason-chip {
  padding: 6px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
  color: var(--admin-text-secondary);
  cursor: pointer;
}

.reason-chip:hover {
  border-color: var(--admin-primary);
  color: var(--admin-primary);
}
</style>
