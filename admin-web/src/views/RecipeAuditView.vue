<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { approveRecipeAudit, getRecipeAudit, listRecipeAudits, rejectRecipeAudit } from '@/api/recipe'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { RecipeDetail, RecipeListItem } from '@/types/cook'
import { formatDateTime } from '@/utils/format'
import { mediaIdToRawUrl, resolveAssetUrl } from '@/utils/media'

const loading = ref(false)
const rows = ref<RecipeListItem[]>([])
const page = reactive({ page: 1, pageSize: 10, total: 0 })
const filters = reactive({ status: 'pending_review', keyword: '' })
const detailVisible = ref(false)
const detail = ref<RecipeDetail>()
const rejectVisible = ref(false)
const rejectForm = reactive({ id: 0, reason: '' })

const rejectReasonOptions = [
  '标题与内容不符',
  '步骤说明不完整',
  '食材用量缺失',
  '图片或视频质量不足',
  '存在营销或违规信息',
]

const statCards = computed(() => {
  const pending = rows.value.filter(item => item.reviewStatus === 'pending_review').length
  const approved = rows.value.filter(item => item.reviewStatus === 'approved').length
  const rejected = rows.value.filter(item => item.reviewStatus === 'rejected').length
  const passRate = rows.value.length ? `${Math.round((approved / rows.value.length) * 100)}%` : '0%'
  return [
    { label: '待审核', value: pending, icon: '⏳', tone: 'card-orange' },
    { label: '已通过', value: approved, icon: '✅', tone: 'card-green' },
    { label: '已驳回', value: rejected, icon: '❌', tone: 'card-red' },
    { label: '当前通过率', value: passRate, icon: '📊', tone: 'card-blue' },
  ]
})

function gradientStyle(seed = 0) {
  const palette = [
    'linear-gradient(135deg,#ff6b35,#ff8f5e)',
    'linear-gradient(135deg,#52c41a,#95de64)',
    'linear-gradient(135deg,#722ed1,#b37feb)',
    'linear-gradient(135deg,#1890ff,#69c0ff)',
    'linear-gradient(135deg,#fa8c16,#ffc53d)',
  ]
  return { background: palette[Math.abs(seed) % palette.length] }
}

function asRecord(value: unknown) {
  return (value && typeof value === 'object' ? value : {}) as Record<string, unknown>
}

function readText(input: unknown, keys: string[], fallback = '-') {
  const record = asRecord(input)
  for (const key of keys) {
    const value = record[key]
    if (typeof value === 'string' && value.trim()) return value
    if (typeof value === 'number') return String(value)
  }
  return fallback
}

function formatVideoStatus(status: string) {
  const labelMap: Record<string, string> = {
    uploaded: '已上传，待处理',
    transcoding: '生成播放版本中',
    ready: '已就绪',
    failed: '处理失败',
  }
  return labelMap[status] || (status === '-' ? '未知' : status || '未知')
}

function coverUrl(recipe?: RecipeListItem | RecipeDetail) {
  const directUrl = resolveAssetUrl(readText(recipe, ['coverUrl', 'coverImageUrl', 'coverImage', 'imageUrl'], ''))
  return directUrl || mediaIdToRawUrl(recipe?.coverMediaId)
}

function categoryTone(category?: string) {
  const text = category || ''
  if (text.includes('川')) return 'tag-red'
  if (text.includes('粤')) return 'tag-orange'
  if (text.includes('减') || text.includes('轻食')) return 'tag-green'
  if (text.includes('烘焙') || text.includes('甜')) return 'tag-purple'
  return 'tag-blue'
}

function difficultyTone(difficulty?: string) {
  if (difficulty === '困难' || difficulty === 'hard') return 'tag-orange'
  if (difficulty === '中等' || difficulty === 'medium') return 'tag-blue'
  return 'tag-green'
}

function recipeInitial(recipe: RecipeListItem | RecipeDetail) {
  return (recipe.title || '菜').slice(0, 1)
}

function formatIngredients(value?: Array<Record<string, unknown>>) {
  return (value || []).map((item, index) => ({
    key: `${index}-${readText(item, ['name', 'ingredientName', 'material'], '食材')}`,
    name: readText(item, ['name', 'ingredientName', 'material'], '未命名食材'),
    amount: readText(item, ['amount', 'quantity', 'weight', 'value'], '-'),
  }))
}

function formatSteps(value?: Array<Record<string, unknown>>) {
  return (value || []).map((item, index) => ({
    key: `${index}-${readText(item, ['title', 'name'], `步骤 ${index + 1}`)}`,
    title: readText(item, ['title', 'name'], `步骤 ${index + 1}`),
    content: readText(item, ['content', 'description', 'text', 'instruction'], '暂无步骤说明'),
    imageUrl: resolveAssetUrl(readText(item, ['imageUrl', 'image', 'mediaUrl', 'url'], ''))
      || mediaIdToRawUrl(Number(readText(item, ['imageMediaId', 'mediaId'], '0')) || 0),
  }))
}

function videoMeta(value?: Record<string, unknown>) {
  const source = asRecord(value)
  const rawStatus = readText(source, ['transcodeStatus', 'status'], '')
  return {
    title: readText(source, ['title', 'name', 'fileName'], '菜谱视频'),
    duration: readText(source, ['durationText', 'duration'], '-'),
    status: formatVideoStatus(rawStatus),
  }
}

async function loadData() {
  loading.value = true
  try {
    const response = await listRecipeAudits({ ...filters, page: page.page, pageSize: page.pageSize })
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
  const response = await getRecipeAudit(id)
  detail.value = response.data
  detailVisible.value = true
}

async function approve(id: number) {
  await approveRecipeAudit(id)
  await loadData()
}

function openReject(id: number) {
  rejectForm.id = id
  rejectForm.reason = ''
  rejectVisible.value = true
}

function appendRejectReason(reason: string) {
  rejectForm.reason = rejectForm.reason
    ? `${rejectForm.reason}\n${reason}`
    : reason
}

async function submitReject() {
  await rejectRecipeAudit(rejectForm.id, rejectForm.reason)
  rejectVisible.value = false
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <PageHeader title="菜谱审核" description="优先处理待审核菜谱，审核通过后内容会进入可上架流程。" />

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
      <el-select v-model="filters.status" clearable style="width: 160px;" placeholder="审核状态">
        <el-option label="待审核" value="pending_review" />
        <el-option label="已通过" value="approved" />
        <el-option label="已驳回" value="rejected" />
      </el-select>
      <el-input v-model="filters.keyword" style="width: 240px;" clearable placeholder="搜索标题或作者" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">
        查询
      </el-button>
      <el-button @click="resetFilters">
        重置
      </el-button>
    </div>
  </div>

  <el-card class="app-card">
    <el-table v-loading="loading" class="admin-table" :data="rows">
      <el-table-column label="菜谱信息" min-width="280">
        <template #default="{ row }">
          <div class="recipe-cell">
            <el-image v-if="coverUrl(row)" :src="coverUrl(row)" fit="cover" class="recipe-cell__thumb" />
            <div v-else class="recipe-cell__thumb recipe-cell__thumb--placeholder" :style="gradientStyle(row.id)">
              {{ recipeInitial(row) }}
            </div>
            <div class="recipe-cell__content">
              <div class="recipe-cell__title">{{ row.title }}</div>
              <div class="recipe-cell__meta">作者：{{ row.authorNickname || '未知' }}</div>
              <div class="recipe-cell__sub">{{ row.intro || '暂无简介' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="分类 / 难度" width="180">
        <template #default="{ row }">
          <div class="stack-field">
            <span class="tag" :class="categoryTone(row.categoryCode)">{{ row.categoryCode || '未分类' }}</span>
            <span class="tag" :class="difficultyTone(row.difficulty)">{{ row.difficulty || '未设置' }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="烹饪信息" width="140">
        <template #default="{ row }">
          <div class="stack-field stack-field--tight">
            <span>{{ row.cookTime || '-' }}</span>
            <span>{{ row.serving || '-' }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="审核状态" width="110">
        <template #default="{ row }">
          <StatusTag :value="row.reviewStatus" />
        </template>
      </el-table-column>

      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt || row.createdAt) }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <div class="table-operations">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button v-if="row.reviewStatus === 'pending_review'" link type="success" @click="approve(row.id)">通过</el-button>
            <el-button v-if="row.reviewStatus === 'pending_review'" link type="danger" @click="openReject(row.id)">驳回</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="admin-pagination">
      <div class="pagination-info">共 {{ page.total }} 条记录</div>
      <el-pagination v-model:current-page="page.page" background layout="prev, pager, next" :total="page.total" @current-change="loadData" />
    </div>
  </el-card>

  <el-drawer v-model="detailVisible" class="admin-drawer" title="审核详情" size="760px">
    <template v-if="detail">
      <div class="detail-layout">
        <div class="detail-hero">
          <el-image v-if="coverUrl(detail)" :src="coverUrl(detail)" fit="cover" class="detail-hero__cover" />
          <div v-else class="detail-hero__cover detail-hero__cover--placeholder" :style="gradientStyle(detail.id)">
            {{ recipeInitial(detail) }}
          </div>

          <div class="detail-hero__content">
            <h2>{{ detail.title }}</h2>
            <div class="detail-hero__tags">
              <span class="tag" :class="categoryTone(detail.categoryCode)">{{ detail.categoryCode || '未分类' }}</span>
              <span class="tag" :class="difficultyTone(detail.difficulty)">{{ detail.difficulty || '未设置难度' }}</span>
              <StatusTag :value="detail.reviewStatus" />
            </div>
            <div class="detail-hero__meta">
              <span>作者：{{ detail.authorNickname || '未知' }}</span>
              <span>烹饪时间：{{ detail.cookTime || '-' }}</span>
              <span>份量：{{ detail.serving || '-' }}</span>
            </div>
            <p class="detail-hero__intro">{{ detail.intro || '暂无简介' }}</p>
          </div>
        </div>

        <section class="detail-card">
          <div class="detail-card__title">待审信息</div>
          <div class="detail-meta-grid">
            <div class="detail-meta-item"><span>菜谱 ID</span><strong>{{ detail.id }}</strong></div>
            <div class="detail-meta-item"><span>版本号</span><strong>{{ detail.versionNo || '-' }}</strong></div>
            <div class="detail-meta-item"><span>更新时间</span><strong>{{ formatDateTime(detail.updatedAt) }}</strong></div>
            <div class="detail-meta-item"><span>驳回原因</span><strong>{{ detail.rejectReason || '-' }}</strong></div>
          </div>
        </section>

        <section class="detail-card">
          <div class="detail-card__title">食材清单</div>
          <div v-if="formatIngredients(detail.ingredients).length" class="ingredient-grid">
            <div v-for="item in formatIngredients(detail.ingredients)" :key="item.key" class="ingredient-grid__item">
              <span>{{ item.name }}</span>
              <strong>{{ item.amount }}</strong>
            </div>
          </div>
          <div v-else class="admin-empty-note">暂无食材信息</div>
        </section>

        <section class="detail-card">
          <div class="detail-card__title">步骤说明</div>
          <div v-if="formatSteps(detail.steps).length" class="step-list">
            <div v-for="(item, index) in formatSteps(detail.steps)" :key="item.key" class="step-item">
              <div class="step-item__num">{{ index + 1 }}</div>
              <el-image v-if="item.imageUrl" :src="item.imageUrl" fit="cover" class="step-item__image" />
              <div v-else class="step-item__image step-item__image--placeholder">📷</div>
              <div class="step-item__content">
                <div class="step-item__title">{{ item.title }}</div>
                <div class="step-item__text">{{ item.content }}</div>
              </div>
            </div>
          </div>
          <div v-else class="admin-empty-note">暂无步骤说明</div>
        </section>

        <section v-if="Object.keys(detail.video || {}).length" class="detail-card">
          <div class="detail-card__title">视频信息</div>
          <div class="video-card">
            <div class="video-card__poster">🎬</div>
            <div>
              <div class="video-card__title">{{ videoMeta(detail.video).title }}</div>
              <div class="video-card__meta">时长 {{ videoMeta(detail.video).duration }} · 状态 {{ videoMeta(detail.video).status }}</div>
            </div>
          </div>
        </section>
      </div>
    </template>
  </el-drawer>

  <el-dialog v-model="rejectVisible" class="admin-dialog" title="填写驳回原因" width="560px">
    <div class="reject-panel">
      <div class="reject-panel__label">快捷原因</div>
      <div class="reject-reasons">
        <button
          v-for="reason in rejectReasonOptions"
          :key="reason"
          type="button"
          class="reject-reason"
          @click="appendRejectReason(reason)"
        >
          {{ reason }}
        </button>
      </div>
      <div class="reject-panel__label">详细说明</div>
      <el-input v-model="rejectForm.reason" type="textarea" :rows="6" placeholder="请输入用户可见的驳回原因" />
    </div>
    <template #footer>
      <el-button @click="rejectVisible = false">取消</el-button>
      <el-button type="danger" @click="submitReject">确认驳回</el-button>
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

.recipe-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recipe-cell__thumb {
  width: 64px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f0f0f0;
}

.recipe-cell__thumb--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  font-weight: 600;
}

.recipe-cell__content {
  display: grid;
  gap: 4px;
}

.recipe-cell__title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.recipe-cell__meta,
.recipe-cell__sub {
  font-size: 12px;
  color: #999;
}

.recipe-cell__sub {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.stack-field {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stack-field--tight {
  display: grid;
  gap: 4px;
  font-size: 13px;
  color: #666;
}

.tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.tag-blue {
  color: #1890ff;
  background: #e6f7ff;
}

.tag-orange {
  color: #fa8c16;
  background: #fff7e6;
}

.tag-green {
  color: #52c41a;
  background: #f6ffed;
}

.tag-red {
  color: #ff4d4f;
  background: #fff1f0;
}

.tag-purple {
  color: #722ed1;
  background: #f9f0ff;
}

.detail-layout {
  display: grid;
  gap: 16px;
}

.detail-hero,
.detail-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: var(--admin-card-shadow);
}

.detail-hero {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 20px;
  padding: 20px;
}

.detail-hero__cover {
  width: 100%;
  aspect-ratio: 16 / 11;
  border-radius: 12px;
  overflow: hidden;
  background: #f0f0f0;
}

.detail-hero__cover--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56px;
  color: #fff;
  font-weight: 700;
}

.detail-hero__content h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.detail-hero__tags,
.detail-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.detail-hero__meta {
  font-size: 13px;
  color: #666;
}

.detail-hero__intro {
  margin: 14px 0 0;
  font-size: 14px;
  line-height: 1.8;
  color: #666;
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

.detail-meta-grid,
.ingredient-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-meta-item,
.ingredient-grid__item {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fafafa;
}

.detail-meta-item span,
.ingredient-grid__item span {
  font-size: 12px;
  color: #999;
}

.detail-meta-item strong,
.ingredient-grid__item strong {
  font-size: 14px;
  color: #333;
}

.step-list {
  display: grid;
  gap: 14px;
}

.step-item {
  display: grid;
  grid-template-columns: 28px 92px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.step-item__num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--admin-primary);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.step-item__image {
  width: 92px;
  height: 68px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.step-item__image--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 20px;
}

.step-item__content {
  display: grid;
  gap: 6px;
}

.step-item__title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.step-item__text {
  font-size: 14px;
  line-height: 1.8;
  color: #666;
}

.video-card {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  padding: 14px;
  border-radius: 10px;
  background: #fafafa;
}

.video-card__poster {
  aspect-ratio: 16 / 10;
  border-radius: 10px;
  background: linear-gradient(135deg, #0f172a, #334155);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
}

.video-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.video-card__meta {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: #666;
}

.reject-panel__label {
  margin-bottom: 8px;
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.reject-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.reject-reason {
  border: 1px solid #d9d9d9;
  border-radius: 999px;
  background: #fff;
  color: #666;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.reject-reason:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
  background: #fff1f0;
}
</style>
