<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { approvePostAudit, blockPostAudit, getPostAudit, listPostAudits, restorePostAudit } from '@/api/post'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { PostItem } from '@/types/cook'
import { formatDateTime } from '@/utils/format'
import { mediaIdsToUrls, resolveAssetUrl } from '@/utils/media'

const loading = ref(false)
const rows = ref<PostItem[]>([])
const page = reactive({ page: 1, pageSize: 10, total: 0 })
const filters = reactive({ status: 'pending_review', keyword: '' })
const detailVisible = ref(false)
const detail = ref<PostItem>()
const blockVisible = ref(false)
const blockForm = reactive({ id: 0, reason: '', action: 'block' })

const blockReasonOptions = [
  '内容存在广告引流',
  '图片或文案不适宜公开展示',
  '恶意灌水或重复发布',
  '攻击性言论',
]

const statCards = computed(() => {
  const pending = rows.value.filter(item => item.status === 'pending_review').length
  const published = rows.value.filter(item => item.status === 'published').length
  const blocked = rows.value.filter(item => item.status === 'blocked').length
  const engagement = rows.value.reduce((sum, item) => sum + (item.likeCount || 0) + (item.commentCount || 0), 0)
  return [
    { label: '待审核动态', value: pending, icon: '📝', tone: 'card-orange' },
    { label: '已通过动态', value: published, icon: '✅', tone: 'card-green' },
    { label: '已屏蔽动态', value: blocked, icon: '🚫', tone: 'card-red' },
    { label: '互动总量', value: engagement, icon: '💬', tone: 'card-blue' },
  ]
})

function gradientStyle(seed = 0) {
  const palette = [
    'linear-gradient(135deg,#ff6b35,#ff8f5e)',
    'linear-gradient(135deg,#722ed1,#b37feb)',
    'linear-gradient(135deg,#52c41a,#95de64)',
    'linear-gradient(135deg,#1890ff,#69c0ff)',
    'linear-gradient(135deg,#fa8c16,#ffc53d)',
  ]
  return { background: palette[Math.abs(seed) % palette.length] }
}

function avatarText(post: PostItem) {
  return (post.nickname || 'U').slice(0, 1).toUpperCase()
}

function avatarUrl(post?: PostItem) {
  return resolveAssetUrl(post?.avatarUrl)
}

function mediaUrls(post?: PostItem) {
  return mediaIdsToUrls(post?.mediaIds).slice(0, 3)
}

function statusClass(status?: string) {
  if (status === 'published') return 'status-chip--success'
  if (status === 'blocked') return 'status-chip--danger'
  return 'status-chip--warning'
}

function sourceLabel(source?: string) {
  if (source === 'normal') return '普通动态'
  if (source === 'recipe') return '菜谱关联'
  if (source === 'ugc') return '用户原创'
  if (source === 'repost') return '转发'
  return source || '普通动态'
}

async function loadData() {
  loading.value = true
  try {
    const response = await listPostAudits({ ...filters, page: page.page, pageSize: page.pageSize })
    rows.value = response.data?.items || []
    page.total = response.data?.total || 0
  }
  finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.status = 'pending_review'
  filters.keyword = ''
  loadData()
}

async function openDetail(id: number) {
  const response = await getPostAudit(id)
  detail.value = response.data
  detailVisible.value = true
}

async function approve(id: number) {
  await approvePostAudit(id)
  await loadData()
}

function openBlock(id: number) {
  blockForm.id = id
  blockForm.reason = ''
  blockForm.action = 'block'
  blockVisible.value = true
}

function appendBlockReason(reason: string) {
  blockForm.reason = blockForm.reason
    ? `${blockForm.reason}\n${reason}`
    : reason
}

async function submitBlock() {
  await blockPostAudit(blockForm.id, { reason: blockForm.reason, action: blockForm.action })
  blockVisible.value = false
  await loadData()
}

async function restore(id: number) {
  await restorePostAudit(id)
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <PageHeader title="动态审核" description="所有动态先审后发，支持审核通过、屏蔽及后续治理动作。" />

  <div class="stats-row">
    <div v-for="card in statCards" :key="card.label" class="stat-card">
      <div class="stat-card__icon" :class="card.tone">
        {{ card.icon }}
      </div>
      <div class="stat-card__info">
        <h4>{{ card.value }}</h4>
        <p>{{ card.label }}</p>
      </div>
    </div>
  </div>

  <div class="page-toolbar">
    <div class="page-toolbar__filters">
      <el-select v-model="filters.status" clearable style="width: 160px;" placeholder="动态状态">
        <el-option label="待审核" value="pending_review" />
        <el-option label="已发布" value="published" />
        <el-option label="已屏蔽" value="blocked" />
      </el-select>
      <el-input v-model="filters.keyword" style="width: 260px;" clearable placeholder="搜索内容或作者" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">
        查询
      </el-button>
      <el-button @click="resetFilters">
        重置
      </el-button>
    </div>
  </div>

  <div v-loading="loading" class="post-list">
    <article v-for="row in rows" :key="row.id" class="post-card">
      <div class="post-card__header">
        <div class="post-card__user">
          <div class="post-card__avatar" :style="gradientStyle(row.userId || row.id)">
            <el-image v-if="avatarUrl(row)" :src="avatarUrl(row)" fit="cover" class="post-card__avatar-image" />
            <span v-else>{{ avatarText(row) }}</span>
          </div>
          <div>
            <div class="post-card__name">{{ row.nickname || `用户${row.userId || row.id}` }}</div>
            <div class="post-card__time">{{ formatDateTime(row.publishedAt || row.createdAt) }} · ID {{ row.id }}</div>
          </div>
        </div>

        <div class="post-card__header-right">
          <span class="status-chip" :class="statusClass(row.status)">{{ row.status === 'published' ? '已通过' : row.status === 'blocked' ? '已屏蔽' : '待审核' }}</span>
        </div>
      </div>

      <div class="post-card__content">
        {{ row.content || '暂无动态内容' }}
      </div>

      <div v-if="mediaUrls(row).length" class="post-card__media">
        <el-image
          v-for="url in mediaUrls(row)"
          :key="url"
          :src="url"
          fit="cover"
          class="post-card__media-item"
        />
      </div>

      <div class="post-card__tags">
        <span class="post-tag">{{ sourceLabel(row.sourceType) }}</span>
        <span v-for="code in row.topicCodes || []" :key="code" class="post-tag"># {{ code }}</span>
        <span v-if="row.relatedRecipeId" class="post-tag">关联菜谱 {{ row.relatedRecipeId }}</span>
        <span v-if="row.location" class="post-tag">{{ row.location }}</span>
      </div>

      <div class="post-card__footer">
        <div class="post-card__stats">
          <span>❤️ {{ row.likeCount || 0 }}</span>
          <span>⭐ {{ row.favoriteCount || 0 }}</span>
          <span>💬 {{ row.commentCount || 0 }}</span>
        </div>
        <div class="post-card__actions">
          <el-button plain @click="openDetail(row.id)">详情</el-button>
          <el-button v-if="row.status === 'pending_review'" type="success" plain @click="approve(row.id)">通过</el-button>
          <el-button v-if="row.status !== 'blocked'" type="danger" plain @click="openBlock(row.id)">屏蔽</el-button>
          <el-button v-if="row.status === 'blocked'" plain @click="restore(row.id)">恢复</el-button>
        </div>
      </div>
    </article>

    <el-empty v-if="!rows.length && !loading" description="暂无动态审核数据" />
  </div>

  <div class="admin-pagination">
    <div class="pagination-info">共 {{ page.total }} 条记录</div>
    <el-pagination v-model:current-page="page.page" background layout="prev, pager, next" :total="page.total" @current-change="loadData" />
  </div>

  <el-drawer v-model="detailVisible" class="admin-drawer" title="动态详情" size="760px">
    <template v-if="detail">
      <div class="detail-layout">
        <div class="detail-hero">
          <div class="detail-hero__user">
            <div class="detail-hero__avatar" :style="gradientStyle(detail.userId || detail.id)">
              <el-image v-if="avatarUrl(detail)" :src="avatarUrl(detail)" fit="cover" class="detail-hero__avatar-image" />
              <span v-else>{{ avatarText(detail) }}</span>
            </div>
            <div>
              <div class="detail-hero__name">{{ detail.nickname || `用户${detail.userId || detail.id}` }}</div>
              <div class="detail-hero__time">{{ formatDateTime(detail.publishedAt || detail.createdAt) }}</div>
            </div>
          </div>
          <StatusTag :value="detail.status" />
        </div>

        <section class="detail-card">
          <div class="detail-card__title">动态内容</div>
          <div class="detail-card__text">{{ detail.content || '暂无动态内容' }}</div>
        </section>

        <section class="detail-card">
          <div class="detail-card__title">图片预览</div>
          <div class="detail-media-grid">
            <el-image
              v-for="url in mediaUrls(detail)"
              :key="url"
              :src="url"
              fit="cover"
              class="detail-media-grid__item"
            />
            <div v-if="!mediaUrls(detail).length" class="detail-media-grid__item detail-media-grid__item--placeholder">
              📷
            </div>
          </div>
        </section>

        <section class="detail-card">
          <div class="detail-card__title">基础信息</div>
          <div class="detail-meta-grid">
            <div class="detail-meta-item"><span>动态 ID</span><strong>{{ detail.id }}</strong></div>
            <div class="detail-meta-item"><span>来源</span><strong>{{ sourceLabel(detail.sourceType) }}</strong></div>
            <div class="detail-meta-item"><span>位置</span><strong>{{ detail.location || '-' }}</strong></div>
            <div class="detail-meta-item"><span>关联菜谱</span><strong>{{ detail.relatedRecipeId || '-' }}</strong></div>
            <div class="detail-meta-item"><span>屏蔽原因</span><strong>{{ detail.blockReason || '-' }}</strong></div>
            <div class="detail-meta-item"><span>处置动作</span><strong>{{ detail.blockAction || '-' }}</strong></div>
          </div>
        </section>

        <section v-if="(detail.topicCodes || []).length" class="detail-card">
          <div class="detail-card__title">话题标签</div>
          <div class="detail-topics">
            <span v-for="code in detail.topicCodes || []" :key="code" class="post-tag"># {{ code }}</span>
          </div>
        </section>
      </div>
    </template>
  </el-drawer>

  <el-dialog v-model="blockVisible" class="admin-dialog" title="屏蔽动态" width="560px">
    <div class="block-panel">
      <div class="block-panel__label">快捷原因</div>
      <div class="block-reasons">
        <button
          v-for="reason in blockReasonOptions"
          :key="reason"
          type="button"
          class="block-reason"
          @click="appendBlockReason(reason)"
        >
          {{ reason }}
        </button>
      </div>

      <el-form label-position="top">
        <el-form-item label="屏蔽原因">
          <el-input v-model="blockForm.reason" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="处理动作">
          <el-select v-model="blockForm.action">
            <el-option label="仅屏蔽" value="block" />
            <el-option label="屏蔽并警告" value="warn" />
            <el-option label="屏蔽并禁言" value="mute" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="blockVisible = false">取消</el-button>
      <el-button type="danger" @click="submitBlock">确认屏蔽</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: var(--admin-card-shadow);
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.card-blue { background: #e6f7ff; }
.card-green { background: #f6ffed; }
.card-orange { background: #fff7e6; }
.card-red { background: #fff1f0; }

.stat-card__info h4 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #333;
}

.stat-card__info p,
.pagination-info {
  margin: 2px 0 0;
  font-size: 12px;
  color: #999;
}

.post-list {
  display: grid;
  gap: 16px;
}

.post-card,
.detail-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: var(--admin-card-shadow);
}

.post-card {
  padding: 20px;
}

.post-card__header,
.post-card__footer,
.detail-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.post-card__user,
.detail-hero__user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.post-card__avatar,
.detail-hero__avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.post-card__avatar-image,
.detail-hero__avatar-image {
  width: 100%;
  height: 100%;
  border-radius: inherit;
}

.post-card__name,
.detail-hero__name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.post-card__time,
.detail-hero__time {
  font-size: 12px;
  color: #999;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-chip--success {
  color: #52c41a;
  background: #f6ffed;
}

.status-chip--danger {
  color: #ff4d4f;
  background: #fff1f0;
}

.status-chip--warning {
  color: #fa8c16;
  background: #fff7e6;
}

.post-card__content,
.detail-card__text {
  margin-top: 14px;
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
}

.post-card__media {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.post-card__media-item,
.detail-media-grid__item {
  border-radius: 10px;
  overflow: hidden;
  background: #f5f7fa;
}

.post-card__media-item {
  width: 120px;
  height: 90px;
}

.post-card__tags,
.detail-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.post-tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f5f5f5;
  color: #666;
  font-size: 12px;
}

.post-card__footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f5f5f5;
}

.post-card__stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #999;
}

.post-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-layout {
  display: grid;
  gap: 16px;
}

.detail-card {
  padding: 18px 20px;
}

.detail-card__title {
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.detail-media-grid,
.detail-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-media-grid__item {
  aspect-ratio: 4 / 3;
}

.detail-media-grid__item--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 24px;
}

.detail-meta-item {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fafafa;
}

.detail-meta-item span {
  font-size: 12px;
  color: #999;
}

.detail-meta-item strong {
  font-size: 14px;
  color: #333;
}

.block-panel__label {
  margin-bottom: 8px;
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.block-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.block-reason {
  border: 1px solid #d9d9d9;
  border-radius: 999px;
  background: #fff;
  color: #666;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.block-reason:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
  background: #fff1f0;
}
</style>
