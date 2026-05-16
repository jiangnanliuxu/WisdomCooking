<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import { getDashboardSummary, getRecipeCategoryRatio, getUserGrowth, listRecentOperationLogs } from '@/api/operation'
import type { AdminDashboardSummary, CategoryRatio, RecentOperationLog, TrendPoint } from '@/types/cook'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const summary = ref<AdminDashboardSummary>()
const userGrowth = ref<TrendPoint[]>([])
const categoryRatios = ref<CategoryRatio[]>([])
const recentLogs = ref<RecentOperationLog[]>([])

const statCards = computed(() => {
  const data = summary.value
  return [
    { label: '累计用户', value: data?.totalUsers ?? 0, note: `待处理反馈 ${data?.processingFeedbackCount ?? 0}`, icon: '👤', className: 'icon-blue' },
    { label: '菜谱总量', value: data?.totalRecipes ?? 0, note: `待审菜谱 ${data?.pendingRecipeCount ?? 0}`, icon: '📖', className: 'icon-orange' },
    { label: '社区动态', value: data?.totalPosts ?? 0, note: `待审动态 ${data?.pendingPostCount ?? 0}`, icon: '📝', className: 'icon-green' },
    { label: '在线轮播', value: data?.onlineBannerCount ?? 0, note: `待处理举报 ${data?.pendingReportCount ?? 0}`, icon: '🖼️', className: 'icon-purple' },
  ]
})

const quickActions = [
  { title: '菜谱管理', icon: '📖', path: '/recipes' },
  { title: '轮播图管理', icon: '🖼️', path: '/banners' },
  { title: '动态审核', icon: '📝', path: '/post-audits' },
  { title: '用户管理', icon: '👤', path: '/users' },
  { title: 'AI 配置', icon: '🤖', path: '/ai/models' },
  { title: '菜谱分类', icon: '🏷️', path: '/categories' },
  { title: '识图日志', icon: '📷', path: '/ai/recognitions' },
  { title: '群组管理', icon: '👥', path: '/groups' },
]

const barMax = computed(() => Math.max(...userGrowth.value.map(item => item.value), 1))
const pieTotal = computed(() => categoryRatios.value.reduce((sum, item) => sum + item.count, 0) || 1)
const piePalette = ['#ff6b35', '#fa8c16', '#1890ff', '#52c41a', '#722ed1', '#13c2c2']
const logDotPalette = ['#52c41a', '#fa8c16', '#ff4d4f', '#1890ff', '#722ed1']

const pieStyle = computed(() => {
  let start = 0
  const segments = categoryRatios.value.slice(0, 6).map((item, index) => {
    const ratio = (item.count / pieTotal.value) * 100
    const end = start + ratio
    const segment = `${piePalette[index % piePalette.length]} ${start}% ${end}%`
    start = end
    return segment
  })
  if (start < 100) segments.push(`#d9d9d9 ${start}% 100%`)
  return { background: `conic-gradient(${segments.join(', ')})` }
})

async function loadData() {
  loading.value = true
  try {
    const [summaryResp, growthResp, ratioResp, logsResp] = await Promise.all([
      getDashboardSummary(),
      getUserGrowth(),
      getRecipeCategoryRatio(),
      listRecentOperationLogs(8),
    ])
    summary.value = summaryResp.data
    userGrowth.value = growthResp.data || []
    categoryRatios.value = ratioResp.data || []
    recentLogs.value = logsResp.data || []
  }
  finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <PageHeader title="仪表盘" description="查看后台核心指标、菜谱分类分布和近期管理动作。" />

  <div v-loading="loading">
    <div class="stat-cards">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-card__icon" :class="card.className">
          {{ card.icon }}
        </div>
        <div class="stat-card__info">
          <h3>{{ card.value }}</h3>
          <p>{{ card.label }}</p>
          <div class="stat-card__trend">{{ card.note }}</div>
        </div>
      </div>
    </div>

    <div class="chart-row">
      <section class="dashboard-panel">
        <div class="dashboard-panel__header">
          <h3>近 7 天用户增长趋势</h3>
        </div>
        <div v-if="userGrowth.length" class="bar-chart">
          <div v-for="item in userGrowth" :key="item.date" class="bar-item">
            <div class="bar-item__value">{{ item.value }}</div>
            <div class="bar" :style="{ height: `${Math.max((item.value / barMax) * 100, 10)}%` }" />
            <div class="label">{{ item.date.slice(5) }}</div>
          </div>
        </div>
        <div v-else class="admin-empty-note">暂无用户增长数据</div>
      </section>

      <section class="dashboard-panel">
        <div class="dashboard-panel__header">
          <h3>菜谱分类占比</h3>
        </div>
        <div v-if="categoryRatios.length" class="pie-chart">
          <div class="pie-visual" :style="pieStyle" />
          <div class="pie-legend">
            <div v-for="(item, index) in categoryRatios.slice(0, 6)" :key="item.categoryCode" class="legend-item">
              <div class="color" :style="{ background: piePalette[index % piePalette.length] }" />
              {{ item.categoryName }} {{ Math.round((item.count / pieTotal) * 100) }}%
            </div>
          </div>
        </div>
        <div v-else class="admin-empty-note">暂无菜谱分类数据</div>
      </section>
    </div>

    <section class="dashboard-panel dashboard-panel--spaced">
      <div class="dashboard-panel__header">
        <h3>快捷入口</h3>
      </div>
      <div class="action-grid">
        <button v-for="item in quickActions" :key="item.path" type="button" class="quick-action" @click="router.push(item.path)">
          <span class="quick-action__icon">{{ item.icon }}</span>
          <span>{{ item.title }}</span>
        </button>
      </div>
    </section>

    <section class="dashboard-panel">
      <div class="dashboard-panel__header">
        <h3>最近操作</h3>
      </div>
      <div v-if="recentLogs.length">
        <div v-for="(item, index) in recentLogs" :key="item.id" class="list-item">
          <div class="dot" :style="{ background: logDotPalette[index % logDotPalette.length] }" />
          <div class="info">
            <strong>{{ item.bizType || 'system' }}</strong>
            <span>{{ item.action || '-' }}</span>
            <span>{{ item.remark || '-' }}</span>
          </div>
          <div class="time">{{ formatDateTime(item.createdAt) }}</div>
        </div>
      </div>
      <div v-else class="admin-empty-note">暂无操作日志</div>
    </section>
  </div>
</template>

<style scoped>
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card,
.dashboard-panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: var(--admin-card-shadow);
}

.stat-card {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card__icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.icon-blue { background: #e6f7ff; }
.icon-orange { background: #fff7e6; }
.icon-green { background: #f6ffed; }
.icon-purple { background: #f9f0ff; }

.stat-card__info h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.stat-card__info p {
  margin: 2px 0 0;
  font-size: 13px;
  color: #999;
}

.stat-card__trend {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}

.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.dashboard-panel {
  padding: 20px;
}

.dashboard-panel--spaced {
  margin-bottom: 24px;
}

.dashboard-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.dashboard-panel__header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 180px;
  padding: 0 8px;
}

.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  height: 100%;
  justify-content: flex-end;
}

.bar-item__value,
.label {
  font-size: 11px;
  color: #999;
}

.bar {
  width: 100%;
  border-radius: 4px 4px 0 0;
  background: linear-gradient(180deg, #ff6b35, #ff8f5e);
}

.pie-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  min-height: 180px;
}

.pie-visual {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  flex-shrink: 0;
}

.pie-legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.legend-item .color {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.quick-action {
  border: none;
  border-radius: 10px;
  background: #fafafa;
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-action:hover {
  background: #fff7e6;
  transform: translateY(-2px);
}

.quick-action__icon {
  font-size: 28px;
}

.quick-action span:last-child {
  font-size: 13px;
  color: #333;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.list-item:last-child {
  border-bottom: none;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.info {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 14px;
  color: #333;
}

.time {
  font-size: 12px;
  color: #ccc;
}
</style>
