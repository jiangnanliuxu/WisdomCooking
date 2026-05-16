<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteRecognitionLog, getRecognitionLog, listRecognitionLogs } from '@/api/ai'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { AiRecognition } from '@/types/cook'
import { formatDateTime } from '@/utils/format'
import { resolveAssetUrl } from '@/utils/media'

const loading = ref(false)
const rows = ref<AiRecognition[]>([])
const page = reactive({ page: 1, pageSize: 10, total: 0 })
const filters = reactive({ keyword: '', status: '', userId: '' })
const detailVisible = ref(false)
const detail = ref<AiRecognition>()

const summaryCards = computed(() => {
  const successCount = rows.value.filter(item => item.status === 'success').length
  const failedCount = rows.value.filter(item => item.status === 'failed').length
  const unknownCount = rows.value.filter(item => !item.status || item.status === 'unknown').length
  const avgLatency = rows.value.length
    ? Math.round(rows.value.reduce((sum, item) => sum + (item.latencyMs || 0), 0) / rows.value.length)
    : 0

  return [
    { label: '当前筛选记录', value: page.total || rows.value.length, icon: '📷', tone: 'card-blue' },
    { label: '识别成功', value: successCount, icon: '✅', tone: 'card-green' },
    { label: '识别失败', value: failedCount, icon: '⚠️', tone: 'card-orange' },
    { label: '平均耗时', value: `${avgLatency} ms`, icon: '⏱️', tone: 'card-red' },
    { label: '待确认结果', value: unknownCount, icon: '❔', tone: 'card-purple' },
  ]
})

const detailNutritionStats = computed(() => buildNutritionList(detail.value?.nutrition))

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

function avatarText(row: AiRecognition) {
  return (row.userNickname || 'U').slice(0, 1).toUpperCase()
}

function userAvatarUrl(row?: AiRecognition) {
  return resolveAssetUrl(row?.userAvatarUrl)
}

function toNumber(value: unknown) {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function extractNutritionMetric(value: unknown) {
  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>
    return toNumber(record.grams ?? record.gram ?? record.value)
  }
  return toNumber(value)
}

function pickNutritionValue(input: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    if (key in input) return extractNutritionMetric(input[key])
  }
  return 0
}

function formatConfidence(value: unknown) {
  const num = toNumber(value)
  if (!num) return '0%'
  return `${Math.round(num <= 1 ? num * 100 : num)}%`
}

function buildNutritionList(input?: Record<string, unknown>) {
  const source = input || {}
  const items = [
    { label: '蛋白质', value: pickNutritionValue(source, ['protein', 'proteinG', 'protein_g', '蛋白质']), color: '#1890ff' },
    { label: '脂肪', value: pickNutritionValue(source, ['fat', 'fatG', 'fat_g', '脂肪']), color: '#fa8c16' },
    { label: '碳水', value: pickNutritionValue(source, ['carb', 'carbs', 'carbohydrate', 'carbohydrateG', '碳水', '碳水化合物']), color: '#52c41a' },
  ]
  const max = Math.max(...items.map(item => item.value), 1)
  return items.map(item => ({
    ...item,
    width: `${Math.max((item.value / max) * 100, item.value ? 18 : 0)}%`,
  }))
}

function calorieClass(value?: number) {
  const num = value || 0
  if (num >= 320) return 'calorie-badge--high'
  if (num >= 180) return 'calorie-badge--mid'
  return 'calorie-badge--low'
}

async function loadData() {
  loading.value = true
  try {
    const response = await listRecognitionLogs({
      keyword: filters.keyword,
      status: filters.status,
      userId: filters.userId || undefined,
      page: page.page,
      pageSize: page.pageSize,
    })
    rows.value = response.data?.items || []
    page.total = response.data?.total || 0
  }
  finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  const response = await getRecognitionLog(id)
  detail.value = response.data
  detailVisible.value = true
}

async function removeLog(id: number) {
  await ElMessageBox.confirm('确认删除这条识图日志？删除后无法恢复。', '删除日志', {
    type: 'warning',
  })
  await deleteRecognitionLog(id)
  ElMessage.success('日志已删除')
  await loadData()
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  filters.userId = ''
  loadData()
}

onMounted(loadData)
</script>

<template>
  <PageHeader title="AI 识图日志" description="查看图片识别结果、热量评估、营养结构和异常记录。" />

  <div class="stats-row">
    <div v-for="card in summaryCards" :key="card.label" class="stat-card">
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
      <el-input v-model="filters.keyword" style="width: 240px;" clearable placeholder="搜索用户名、识别菜品..." @keyup.enter="loadData" />
      <el-select v-model="filters.status" clearable style="width: 150px;" placeholder="识图状态">
        <el-option label="成功" value="success" />
        <el-option label="失败" value="failed" />
        <el-option label="未知" value="unknown" />
      </el-select>
      <el-input v-model="filters.userId" style="width: 160px;" clearable placeholder="用户 ID" @keyup.enter="loadData" />
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
      <el-table-column label="用户" min-width="170">
        <template #default="{ row }">
          <div class="user-cell">
            <div class="user-cell__avatar" :style="gradientStyle(row.userId || row.id)">
              <el-image v-if="userAvatarUrl(row)" :src="userAvatarUrl(row)" fit="cover" class="user-cell__avatar-image" />
              <span v-else>{{ avatarText(row) }}</span>
            </div>
            <div>
              <div class="user-cell__name">{{ row.userNickname || `用户${row.userId || '-'}` }}</div>
              <div class="user-cell__meta">UID: {{ row.userId || '-' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="上传图片" width="120">
        <template #default="{ row }">
          <el-image v-if="row.imageUrl" :src="row.imageUrl" fit="cover" class="food-thumb" />
          <div v-else class="food-thumb food-thumb--placeholder">
            📷
          </div>
        </template>
      </el-table-column>

      <el-table-column label="识别结果" min-width="220">
        <template #default="{ row }">
            <div class="food-info">
              <div class="food-info__name">{{ row.recognizedName || '未识别' }}</div>
            <div class="food-info__meta">置信度：{{ formatConfidence(row.confidence) }}</div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="预估卡路里" width="150">
        <template #default="{ row }">
          <span class="calorie-badge" :class="calorieClass(row.calories)">
            {{ row.calories || 0 }} kcal
          </span>
        </template>
      </el-table-column>

      <el-table-column label="主要营养成分" min-width="220">
        <template #default="{ row }">
          <div class="nutrient-bars">
            <div v-for="item in buildNutritionList(row.nutrition)" :key="item.label" class="nutrient-bar">
              <span>{{ item.label }}</span>
              <div class="nutrient-bar__track">
                <div class="nutrient-bar__fill" :style="{ width: item.width, background: item.color }" />
              </div>
              <span>{{ item.value }}g</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <StatusTag :value="row.status" />
        </template>
      </el-table-column>

      <el-table-column label="识别时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <div class="table-operations">
            <el-button link type="primary" @click="openDetail(row.id)">
              详情
            </el-button>
            <el-button link type="danger" @click="removeLog(row.id)">
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="admin-pagination">
      <div class="pagination-info">共 {{ page.total }} 条记录</div>
      <el-pagination v-model:current-page="page.page" background layout="prev, pager, next" :total="page.total" @current-change="loadData" />
    </div>
  </el-card>

  <el-drawer v-model="detailVisible" class="admin-drawer" title="识图日志详情" size="680px">
    <template v-if="detail">
      <div class="detail-layout">
        <el-image v-if="detail.imageUrl" :src="detail.imageUrl" fit="cover" class="detail-photo" />
        <div v-else class="detail-photo detail-photo--placeholder">
          <div class="detail-photo__placeholder">
            <div class="detail-photo__icon">📷</div>
            <div>暂无原图</div>
          </div>
        </div>

        <div class="nutrition-grid">
          <div class="nutrition-item">
            <div class="nutrition-item__value">{{ detail.calories || 0 }}</div>
            <div class="nutrition-item__unit">kcal</div>
            <div class="nutrition-item__label">总热量</div>
          </div>
          <div v-for="item in detailNutritionStats" :key="item.label" class="nutrition-item">
            <div class="nutrition-item__value">{{ item.value }}</div>
            <div class="nutrition-item__unit">g</div>
            <div class="nutrition-item__label">{{ item.label }}</div>
          </div>
        </div>

        <div class="detail-section-grid">
          <section class="detail-card">
            <div class="detail-card__label">识别结果</div>
            <div class="detail-card__value">{{ detail.recognizedName || '未识别' }}</div>
            <div class="detail-card__sub">置信度 {{ formatConfidence(detail.confidence) }} · 耗时 {{ detail.latencyMs || 0 }} ms</div>
          </section>

          <section class="detail-card">
            <div class="detail-card__label">状态与模型</div>
            <div class="detail-card__value">
              <StatusTag :value="detail.status" />
            </div>
            <div class="detail-card__sub">{{ detail.modelName || '未记录模型' }}</div>
          </section>
        </div>

        <section class="detail-card">
          <div class="detail-card__label">营养建议</div>
          <div class="detail-card__text">{{ detail.suggestion || '暂无建议' }}</div>
        </section>

        <section class="detail-card">
          <div class="detail-card__label">免责声明</div>
          <div class="detail-card__text">{{ detail.disclaimer || '暂无免责声明内容' }}</div>
        </section>

        <section class="detail-card">
          <div class="detail-card__label">错误信息</div>
          <div class="detail-card__text">{{ detail.errorMessage || '无' }}</div>
        </section>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
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
.card-purple { background: #f9f0ff; }

.stat-card__info h4 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #333;
}

.stat-card__info p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #999;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell__avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}

.user-cell__avatar-image {
  width: 100%;
  height: 100%;
}

.user-cell__name,
.food-info__name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.user-cell__meta,
.food-info__meta,
.pagination-info {
  font-size: 12px;
  color: #999;
}

.food-thumb {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  overflow: hidden;
  background: #f0f0f0;
}

.food-thumb--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 20px;
}

.food-info {
  display: grid;
  gap: 4px;
}

.calorie-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.calorie-badge--low {
  background: #f6ffed;
  color: #52c41a;
}

.calorie-badge--mid {
  background: #fff7e6;
  color: #fa8c16;
}

.calorie-badge--high {
  background: #fff1f0;
  color: #ff4d4f;
}

.nutrient-bars {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nutrient-bar {
  display: grid;
  grid-template-columns: 40px 1fr 34px;
  gap: 8px;
  align-items: center;
  font-size: 11px;
  color: #666;
}

.nutrient-bar__track {
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background: #f0f0f0;
  overflow: hidden;
}

.nutrient-bar__fill {
  height: 100%;
  border-radius: 999px;
}

.detail-layout {
  display: grid;
  gap: 16px;
}

.detail-photo {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 12px;
  overflow: hidden;
  background: #f5f5f5;
}

.detail-photo--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-photo__placeholder {
  text-align: center;
  color: #999;
}

.detail-photo__icon {
  font-size: 40px;
  margin-bottom: 8px;
}

.nutrition-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.nutrition-item,
.detail-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: var(--admin-card-shadow);
}

.nutrition-item {
  padding: 14px 12px;
  text-align: center;
}

.nutrition-item__value {
  font-size: 22px;
  font-weight: 700;
  color: #333;
}

.nutrition-item__unit {
  font-size: 12px;
  color: #999;
}

.nutrition-item__label {
  margin-top: 3px;
  font-size: 12px;
  color: #666;
}

.detail-section-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.detail-card {
  padding: 16px;
}

.detail-card__label {
  margin-bottom: 8px;
  font-size: 13px;
  color: #999;
}

.detail-card__value {
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.detail-card__sub {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}

.detail-card__text {
  color: #333;
  font-size: 14px;
  line-height: 1.8;
}
</style>
