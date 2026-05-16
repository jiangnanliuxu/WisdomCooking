<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getConversationLog, listConversationLogs, markConversation } from '@/api/ai'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { AiConversation, AiConversationDetail, AiMessage, AiRagSource } from '@/types/cook'
import { formatDateTime } from '@/utils/format'
import { resolveAssetUrl } from '@/utils/media'

const loading = ref(false)
const rows = ref<AiConversation[]>([])
const page = reactive({ page: 1, pageSize: 10, total: 0 })
const filters = reactive({ keyword: '', flag: '', modelType: '' })
const detailVisible = ref(false)
const detail = ref<AiConversationDetail>()
const markVisible = ref(false)
const markForm = reactive({ id: 0, flag: 'normal', reason: '' })

const markReasonOptions = [
  '输出内容需要人工复核',
  '涉及饮食风险建议',
  '命中违规表达',
  '对话主题偏离产品范围',
]

const totalText = computed(() => `共 ${page.total} 条记录 / 第 ${page.page} 页`)
const normalCount = computed(() => rows.value.filter(item => (item.flag || 'normal') === 'normal').length)
const warningCount = computed(() => rows.value.filter(item => item.flag === 'warning').length)
const violationCount = computed(() => rows.value.filter(item => item.flag === 'violation').length)
const tokenTotal = computed(() => rows.value.reduce((sum, item) => sum + Number(item.totalTokens || 0), 0))

function avatarStyle(index: number) {
  const palette = [
    'linear-gradient(135deg,#ff6b35,#ff8f5e)',
    'linear-gradient(135deg,#52c41a,#95de64)',
    'linear-gradient(135deg,#722ed1,#b37feb)',
    'linear-gradient(135deg,#1890ff,#69c0ff)',
    'linear-gradient(135deg,#faad14,#ffc53d)',
  ]
  return { background: palette[index % palette.length] }
}

function userInitial(name?: string) {
  return (name || 'U').slice(0, 1)
}

function userAvatarUrl(user?: { userAvatarUrl?: string }) {
  return resolveAssetUrl(user?.userAvatarUrl)
}

function modelTypeLabel(value?: string) {
  if (value === 'vision') return '视觉模型'
  if (value === 'chat') return '对话模型'
  return value || '未标注'
}

function ragSources(message: AiMessage): AiRagSource[] {
  if (!message.ragSourcesJson) return []
  try {
    const parsed = JSON.parse(message.ragSourcesJson) as unknown
    return Array.isArray(parsed) ? parsed as AiRagSource[] : []
  }
  catch {
    return []
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.flag = ''
  filters.modelType = ''
  page.page = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const response = await listConversationLogs({ ...filters, page: page.page, pageSize: page.pageSize })
    rows.value = response.data?.items || []
    page.total = response.data?.total || 0
  }
  finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  const response = await getConversationLog(id)
  detail.value = response.data
  detailVisible.value = true
}

function openMark(row: AiConversation) {
  markForm.id = row.conversationId
  markForm.flag = row.flag || 'normal'
  markForm.reason = row.flagReason || ''
  markVisible.value = true
}

async function submitMark() {
  if (markForm.flag !== 'normal' && !markForm.reason.trim()) {
    ElMessage.warning('请填写标记说明')
    return
  }
  await markConversation(markForm.id, {
    flag: markForm.flag,
    reason: markForm.reason.trim() || undefined,
  })
  markVisible.value = false
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <PageHeader title="AI 对话日志" description="按会话聚合查看问答记录，支持查看消息链、模型消耗和风控标记。" />

  <div class="stats-grid">
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--blue">💬</div>
      <div>
        <div class="stat-card__value">{{ page.total }}</div>
        <div class="stat-card__label">会话总数</div>
      </div>
    </article>
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--green">✅</div>
      <div>
        <div class="stat-card__value">{{ normalCount }}</div>
        <div class="stat-card__label">当前页正常会话</div>
      </div>
    </article>
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--orange">⚠️</div>
      <div>
        <div class="stat-card__value">{{ warningCount + violationCount }}</div>
        <div class="stat-card__label">当前页风险会话</div>
      </div>
    </article>
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--pink">🧠</div>
      <div>
        <div class="stat-card__value">{{ tokenTotal }}</div>
        <div class="stat-card__label">当前页 Token 总量</div>
      </div>
    </article>
  </div>

  <div class="page-toolbar">
    <div class="page-toolbar__filters">
      <el-input v-model="filters.keyword" style="width: 260px;" clearable placeholder="搜索用户、标题或摘要" @keyup.enter="loadData" />
      <el-select v-model="filters.flag" clearable style="width: 150px;" placeholder="标记">
        <el-option label="正常" value="normal" />
        <el-option label="警告" value="warning" />
        <el-option label="违规" value="violation" />
      </el-select>
      <el-select v-model="filters.modelType" clearable style="width: 150px;" placeholder="模型类型">
        <el-option label="对话模型" value="chat" />
        <el-option label="视觉模型" value="vision" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>
  </div>

  <el-card class="app-card">
    <el-table v-loading="loading" class="admin-table" :data="rows">
      <el-table-column label="用户 / 会话" min-width="250">
        <template #default="{ row, $index }">
          <div class="conversation-user">
            <div class="conversation-user__avatar" :style="avatarStyle($index)">
              <el-image v-if="userAvatarUrl(row)" :src="userAvatarUrl(row)" fit="cover" class="avatar-image" />
              <span v-else>{{ userInitial(row.userNickname) }}</span>
            </div>
            <div class="conversation-user__copy">
              <div class="conversation-user__name">{{ row.userNickname || `用户 ${row.userId}` }}</div>
              <div class="conversation-user__meta">CID {{ row.conversationId }} · {{ row.title || '未命名会话' }}</div>
              <div class="conversation-user__last">{{ row.lastMessage || '暂无消息内容' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="模型信息" min-width="180">
        <template #default="{ row }">
          <div class="model-info">
            <strong>{{ row.modelName || '-' }}</strong>
            <span>{{ modelTypeLabel(row.modelType) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="轮次" width="90">
        <template #default="{ row }">{{ row.rounds || 0 }}</template>
      </el-table-column>
      <el-table-column label="Token / 耗时" width="160">
        <template #default="{ row }">
          <div class="metric-cell">
            <strong>{{ row.totalTokens || 0 }}</strong>
            <span>{{ row.responseTimeMs || 0 }} ms</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="RAG" width="110">
        <template #default="{ row }">
          <StatusTag :value="row.ragHit ? 'indexed' : 'miss'" />
        </template>
      </el-table-column>
      <el-table-column label="标记" width="110">
        <template #default="{ row }">
          <StatusTag :value="row.flag || 'normal'" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <div class="table-operations">
            <el-button link type="primary" @click="openDetail(row.conversationId)">详情</el-button>
            <el-button link type="warning" @click="openMark(row)">标记</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <div class="admin-pagination">
      <div class="pagination-info">{{ totalText }}</div>
      <el-pagination v-model:current-page="page.page" background layout="prev, pager, next" :total="page.total" @current-change="loadData" />
    </div>
  </el-card>

  <el-drawer v-model="detailVisible" class="admin-drawer" title="对话详情" size="720px">
    <template v-if="detail">
      <div class="detail-hero">
        <div class="detail-hero__avatar">
          <el-image v-if="userAvatarUrl(detail.summary)" :src="userAvatarUrl(detail.summary)" fit="cover" class="avatar-image" />
          <span v-else>{{ userInitial(detail.summary.userNickname) }}</span>
        </div>
        <div class="detail-hero__copy">
          <div class="detail-hero__title-row">
            <h3>{{ detail.summary.title || '未命名会话' }}</h3>
            <StatusTag :value="detail.summary.flag || 'normal'" />
          </div>
          <div class="detail-hero__meta">
            {{ detail.summary.userNickname || `用户 ${detail.summary.userId}` }} · {{ detail.summary.modelName || '-' }} · {{ modelTypeLabel(detail.summary.modelType) }}
          </div>
          <p>{{ detail.summary.lastMessage || '暂无摘要' }}</p>
        </div>
      </div>

      <div class="detail-metrics">
        <article>
          <span>轮次</span>
          <strong>{{ detail.summary.rounds || 0 }}</strong>
        </article>
        <article>
          <span>总 Token</span>
          <strong>{{ detail.summary.totalTokens || 0 }}</strong>
        </article>
        <article>
          <span>平均响应</span>
          <strong>{{ detail.summary.responseTimeMs || 0 }} ms</strong>
        </article>
      </div>

      <div class="message-list">
        <article
          v-for="message in detail.messages"
          :key="message.id"
          class="message-card"
          :class="message.role === 'assistant' ? 'message-card--assistant' : 'message-card--user'"
        >
          <div class="message-card__head">
            <strong>{{ message.role === 'assistant' ? 'AI 回复' : '用户提问' }}</strong>
            <span>{{ formatDateTime(message.createdAt) }}</span>
          </div>
          <div class="message-card__body">{{ message.content }}</div>
          <div v-if="message.role === 'assistant'" class="message-card__rag">
            <StatusTag :value="message.ragHit ? 'indexed' : 'miss'" />
            <span v-if="message.fallbackReason">{{ message.fallbackReason }}</span>
          </div>
          <div v-if="ragSources(message).length" class="rag-source-list">
            <article v-for="source in ragSources(message)" :key="`${source.documentId}-${source.chunkId}`" class="rag-source">
              <strong>{{ source.title || source.fileName || '本地知识库' }}</strong>
            </article>
          </div>
          <div class="message-card__foot">
            <span>输入 {{ message.inputTokens || 0 }}</span>
            <span>输出 {{ message.outputTokens || 0 }}</span>
            <span>{{ message.responseTimeMs || 0 }} ms</span>
          </div>
        </article>
      </div>
    </template>
  </el-drawer>

  <el-dialog v-model="markVisible" class="admin-dialog" title="标记 AI 会话" width="520px">
    <div class="dialog-hint">
      标记结果会直接影响管理端风控筛选，警告和违规建议补充具体说明。
    </div>
    <div class="reason-list">
      <button
        v-for="item in markReasonOptions"
        :key="item"
        type="button"
        class="reason-chip"
        @click="markForm.reason = item"
      >
        {{ item }}
      </button>
    </div>
    <el-form label-position="top">
      <el-form-item label="标记状态">
        <el-radio-group v-model="markForm.flag">
          <el-radio label="normal">正常</el-radio>
          <el-radio label="warning">警告</el-radio>
          <el-radio label="violation">违规</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="markForm.reason" type="textarea" :rows="5" placeholder="请输入标记原因或人工复核备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="markVisible = false">取消</el-button>
      <el-button type="primary" @click="submitMark">保存</el-button>
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

.stat-card__icon--blue { background: #e6f7ff; }
.stat-card__icon--green { background: #f6ffed; }
.stat-card__icon--orange { background: #fff7e6; }
.stat-card__icon--pink { background: #fff0f6; }

.stat-card__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--admin-text-main);
}

.stat-card__label,
.pagination-info,
.conversation-user__meta,
.conversation-user__last,
.model-info span,
.metric-cell span,
.detail-hero__meta,
.detail-metrics span,
.message-card__head span,
.message-card__foot,
.dialog-hint {
  font-size: 12px;
  color: var(--admin-text-light);
}

.conversation-user,
.detail-hero,
.detail-hero__title-row,
.message-card__head,
.message-card__foot {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.conversation-user__avatar,
.detail-hero__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-image {
  width: 100%;
  height: 100%;
}

.conversation-user__copy,
.detail-hero__copy {
  min-width: 0;
  flex: 1;
}

.conversation-user__name,
.model-info strong,
.metric-cell strong,
.detail-hero__copy h3 {
  color: var(--admin-text-main);
}

.conversation-user__name,
.model-info strong {
  font-weight: 600;
}

.conversation-user__last,
.detail-hero__copy p,
.message-card__body {
  line-height: 1.7;
}

.conversation-user__last {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-info,
.metric-cell {
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

.detail-hero__copy h3 {
  margin: 0;
  font-size: 20px;
}

.detail-hero__copy p {
  margin: 8px 0 0;
  font-size: 13px;
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

.detail-metrics strong {
  font-size: 18px;
  color: var(--admin-text-main);
}

.message-list {
  display: grid;
  gap: 12px;
}

.message-card {
  padding: 16px;
  border-radius: 14px;
}

.message-card--assistant {
  background: #fafafa;
}

.message-card--user {
  background: #fff7e6;
}

.message-card__head strong {
  color: var(--admin-text-main);
}

.message-card__body {
  margin: 10px 0 12px;
  white-space: pre-wrap;
  color: var(--admin-text-secondary);
}

.message-card__foot {
  align-items: center;
  justify-content: flex-start;
}

.message-card__rag {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.message-card__rag span,
.rag-source span,
.rag-source p {
  font-size: 12px;
  color: var(--admin-text-light);
}

.rag-source-list {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.rag-source {
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--admin-border);
}

.rag-source strong {
  display: block;
  color: var(--admin-text-main);
  font-size: 13px;
}

.rag-source p {
  margin: 6px 0 0;
  line-height: 1.6;
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
