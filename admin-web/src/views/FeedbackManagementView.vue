<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getFeedback, listFeedbacks } from '@/api/operation'
import PageHeader from '@/components/PageHeader.vue'
import type { FeedbackItem } from '@/types/cook'
import { formatDateTime } from '@/utils/format'
import { mediaIdsToUrls } from '@/utils/media'

const loading = ref(false)
const rows = ref<FeedbackItem[]>([])
const detail = ref<FeedbackItem>()
const detailVisible = ref(false)
const page = reactive({ page: 1, pageSize: 10, total: 0 })
const filters = reactive({ status: '', keyword: '' })

const totalText = computed(() => `共 ${page.total} 条记录 / 第 ${page.page} 页`)

const feedbackTypeMap: Record<string, string> = {
  bug: '功能异常',
  suggest: '功能建议',
  report: '内容举报',
  other: '其他问题',
}

const statusMap: Record<string, { label: string, type: 'warning' | 'success' | 'info' | 'danger' }> = {
  processing: { label: '处理中', type: 'warning' },
  resolved: { label: '已解决', type: 'success' },
  closed: { label: '已关闭', type: 'info' },
  rejected: { label: '已驳回', type: 'danger' },
}

function typeLabel(type?: string) {
  return feedbackTypeMap[String(type || '')] || type || '功能建议'
}

function statusLabel(status?: string) {
  return statusMap[String(status || '')]?.label || status || '处理中'
}

function statusType(status?: string) {
  return statusMap[String(status || '')]?.type || 'warning'
}

function feedbackImages(item?: FeedbackItem) {
  return mediaIdsToUrls(item?.mediaIds)
}

async function loadData() {
  loading.value = true
  try {
    const response = await listFeedbacks({ ...filters, page: page.page, pageSize: page.pageSize })
    rows.value = response.data?.items || []
    page.total = response.data?.total || 0
  }
  finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.status = ''
  filters.keyword = ''
  page.page = 1
  loadData()
}

async function openDetail(id: number) {
  const response = await getFeedback(id)
  detail.value = response.data
  detailVisible.value = true
}

onMounted(loadData)
</script>

<template>
  <PageHeader title="用户反馈" description="查看用户提交的问题、建议、联系方式和截图附件。" />

  <div class="page-toolbar">
    <div class="page-toolbar__filters">
      <el-input v-model="filters.keyword" style="width: 260px;" clearable placeholder="搜索用户昵称或反馈内容" @keyup.enter="loadData" />
      <el-select v-model="filters.status" clearable style="width: 150px;" placeholder="全部状态">
        <el-option label="处理中" value="processing" />
        <el-option label="已解决" value="resolved" />
        <el-option label="已关闭" value="closed" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>
  </div>

  <el-card class="app-card">
    <el-table v-loading="loading" class="admin-table" :data="rows">
      <el-table-column label="反馈用户" min-width="170">
        <template #default="{ row }">
          <div class="feedback-user">
            <div class="feedback-user__avatar">{{ (row.userNickname || 'U').slice(0, 1) }}</div>
            <div>
              <div class="feedback-user__name">{{ row.userNickname || `用户${row.userId || row.id}` }}</div>
              <div class="feedback-user__id">UID: {{ row.userId || '-' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ typeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column label="反馈内容" min-width="320">
        <template #default="{ row }">
          <div class="feedback-content">{{ row.content || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="截图" width="90">
        <template #default="{ row }">{{ feedbackImages(row).length }} 张</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="admin-pagination">
      <div class="pagination-info">{{ totalText }}</div>
      <el-pagination v-model:current-page="page.page" background layout="prev, pager, next" :total="page.total" @current-change="loadData" />
    </div>
  </el-card>

  <el-drawer v-model="detailVisible" class="admin-drawer" title="反馈详情" size="640px">
    <template v-if="detail">
      <div class="detail-header">
        <div class="feedback-user__avatar feedback-user__avatar--large">{{ (detail.userNickname || 'U').slice(0, 1) }}</div>
        <div>
          <div class="detail-title">{{ detail.userNickname || `用户${detail.userId || detail.id}` }}</div>
          <div class="detail-meta">{{ typeLabel(detail.type) }} · {{ formatDateTime(detail.createdAt) }}</div>
        </div>
      </div>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="反馈状态">
          <el-tag :type="statusType(detail.status)" effect="light">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ detail.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="反馈内容">
          <div class="detail-content">{{ detail.content || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.replyContent" label="后台回复">
          <div class="detail-content">{{ detail.replyContent }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="feedbackImages(detail).length" class="detail-images">
        <div class="detail-section-title">反馈截图</div>
        <div class="detail-image-grid">
          <el-image
            v-for="url in feedbackImages(detail)"
            :key="url"
            :src="url"
            :preview-src-list="feedbackImages(detail)"
            fit="cover"
            class="detail-image"
          />
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.feedback-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feedback-user__avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff6b35, #ff9b54);
  color: #fff;
  font-weight: 700;
}

.feedback-user__avatar--large {
  width: 56px;
  height: 56px;
  font-size: 20px;
}

.feedback-user__name,
.detail-title {
  color: var(--admin-text-main);
  font-weight: 600;
}

.feedback-user__id,
.detail-meta,
.pagination-info {
  color: var(--admin-text-light);
  font-size: 12px;
}

.feedback-content {
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.detail-content {
  line-height: 1.8;
  white-space: pre-wrap;
}

.detail-images {
  margin-top: 18px;
}

.detail-section-title {
  margin-bottom: 12px;
  color: var(--admin-text-main);
  font-weight: 600;
}

.detail-image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.detail-image {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: var(--admin-surface-subtle);
}
</style>
