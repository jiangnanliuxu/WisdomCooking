<script setup lang="ts">
import {
  CircleCheck,
  Collection,
  Document,
  Refresh,
  Search,
  Upload,
} from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createKnowledgeText,
  listKnowledgeDocuments,
  listKnowledgeJobs,
  offlineKnowledgeDocument,
  reindexKnowledgeDocument,
  scanKnowledgeDocuments,
  uploadKnowledgeDocument,
} from '@/api/ai'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { AiKnowledgeDocument, AiKnowledgeIndexJob } from '@/types/cook'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const jobLoading = ref(false)
const documents = ref<AiKnowledgeDocument[]>([])
const jobs = ref<AiKnowledgeIndexJob[]>([])
const page = reactive({ page: 1, pageSize: 10, total: 0 })
const jobPage = reactive({ page: 1, pageSize: 8, total: 0 })
const filters = reactive({ status: '', keyword: '' })
const fileInputRef = ref<HTMLInputElement>()

const textDialogVisible = ref(false)
const textSubmitting = ref(false)
const textForm = reactive({
  fileName: '',
  content: '',
})

const indexedCount = computed(() => documents.value.filter(item => item.status === 'indexed').length)
const failedCount = computed(() => documents.value.filter(item => item.status === 'failed').length)
const chunkCount = computed(() => documents.value.reduce((sum, item) => sum + Number(item.chunkCount || 0), 0))
const totalText = computed(() => `共 ${page.total} 份文档 / 第 ${page.page} 页`)
const jobTotalText = computed(() => `共 ${jobPage.total} 个任务 / 第 ${jobPage.page} 页`)

function formatSize(value?: number) {
  const size = Number(value || 0)
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function jobTypeLabel(value?: string) {
  const map: Record<string, string> = {
    startup_scan: '启动扫描',
    manual_scan: '手动扫描',
    reindex: '重建索引',
  }
  return map[value || ''] || value || '-'
}

function resetFilters() {
  filters.status = ''
  filters.keyword = ''
  page.page = 1
  loadDocuments()
}

async function loadDocuments() {
  loading.value = true
  try {
    const response = await listKnowledgeDocuments({
      ...filters,
      page: page.page,
      pageSize: page.pageSize,
    })
    documents.value = response.data?.items || []
    page.total = response.data?.total || 0
  }
  finally {
    loading.value = false
  }
}

async function loadJobs() {
  jobLoading.value = true
  try {
    const response = await listKnowledgeJobs({
      page: jobPage.page,
      pageSize: jobPage.pageSize,
    })
    jobs.value = response.data?.items || []
    jobPage.total = response.data?.total || 0
  }
  finally {
    jobLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadDocuments(), loadJobs()])
}

async function handleScan() {
  const response = await scanKnowledgeDocuments()
  ElMessage.success(`扫描任务已创建：#${response.data?.id || '-'}`)
  await loadJobs()
}

function openTextDialog() {
  textForm.fileName = ''
  textForm.content = ''
  textDialogVisible.value = true
}

async function submitTextDocument() {
  if (!textForm.fileName.trim() || !textForm.content.trim()) {
    ElMessage.warning('请填写文件名和正文')
    return
  }
  textSubmitting.value = true
  try {
    await createKnowledgeText({
      fileName: textForm.fileName.trim(),
      content: textForm.content,
    })
    ElMessage.success('知识库文本已入库')
    textDialogVisible.value = false
    await refreshAll()
  }
  finally {
    textSubmitting.value = false
  }
}

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!/\.(md|txt)$/i.test(file.name)) {
    ElMessage.warning('仅支持 .md 和 .txt 文件')
    return
  }
  await uploadKnowledgeDocument(file)
  ElMessage.success('文件已上传并开始索引')
  await refreshAll()
}

async function handleReindex(row: AiKnowledgeDocument) {
  await ElMessageBox.confirm(`确认重建 ${row.fileName} 的向量索引？`, '重建索引', { type: 'warning' })
  const response = await reindexKnowledgeDocument(row.id)
  ElMessage.success(`重建任务已创建：#${response.data?.id || '-'}`)
  await loadJobs()
}

async function handleOffline(row: AiKnowledgeDocument) {
  await ElMessageBox.confirm(`确认下线 ${row.fileName}？原始文件会继续保留在本地目录。`, '下线文档', { type: 'warning' })
  await offlineKnowledgeDocument(row.id)
  ElMessage.success('文档已下线')
  await refreshAll()
}

onMounted(refreshAll)
</script>

<template>
  <PageHeader title="AI 知识库" description="管理本地 embeddingDocs 文档、向量索引状态和扫描任务。">
    <template #actions>
      <input
        ref="fileInputRef"
        class="hidden-file-input"
        type="file"
        accept=".md,.txt,text/markdown,text/plain"
        @change="handleFileSelected"
      >
      <el-button @click="openTextDialog">
        <el-icon><Document /></el-icon>
        录入文本
      </el-button>
      <el-button @click="triggerUpload">
        <el-icon><Upload /></el-icon>
        上传文件
      </el-button>
      <el-button type="primary" @click="handleScan">
        <el-icon><Refresh /></el-icon>
        手动扫描
      </el-button>
    </template>
  </PageHeader>

  <div class="knowledge-stats">
    <article class="knowledge-stat">
      <span class="knowledge-stat__icon knowledge-stat__icon--blue">
        <el-icon><Collection /></el-icon>
      </span>
      <div>
        <strong>{{ page.total }}</strong>
        <span>纳管文档</span>
      </div>
    </article>
    <article class="knowledge-stat">
      <span class="knowledge-stat__icon knowledge-stat__icon--green">
        <el-icon><CircleCheck /></el-icon>
      </span>
      <div>
        <strong>{{ indexedCount }}</strong>
        <span>当前页已索引</span>
      </div>
    </article>
    <article class="knowledge-stat">
      <span class="knowledge-stat__icon knowledge-stat__icon--orange">
        <el-icon><Document /></el-icon>
      </span>
      <div>
        <strong>{{ chunkCount }}</strong>
        <span>当前页分片</span>
      </div>
    </article>
    <article class="knowledge-stat">
      <span class="knowledge-stat__icon knowledge-stat__icon--red">
        <el-icon><Refresh /></el-icon>
      </span>
      <div>
        <strong>{{ failedCount }}</strong>
        <span>当前页失败</span>
      </div>
    </article>
  </div>

  <div class="page-toolbar">
    <div class="page-toolbar__filters">
      <el-input v-model="filters.keyword" clearable style="width: 260px;" placeholder="搜索文件名、标题或路径" @keyup.enter="loadDocuments">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="filters.status" clearable style="width: 160px;" placeholder="索引状态" @change="loadDocuments">
        <el-option label="待处理" value="pending" />
        <el-option label="索引中" value="indexing" />
        <el-option label="已索引" value="indexed" />
        <el-option label="失败" value="failed" />
        <el-option label="已下线" value="offline" />
      </el-select>
      <el-button type="primary" @click="loadDocuments">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>
    <el-button @click="refreshAll">
      <el-icon><Refresh /></el-icon>
      刷新
    </el-button>
  </div>

  <el-card class="app-card">
    <el-table v-loading="loading" class="admin-table" :data="documents">
      <el-table-column label="文档" min-width="280">
        <template #default="{ row }">
          <div class="document-cell">
            <strong>{{ row.title || row.fileName }}</strong>
            <span>{{ row.relativePath }}</span>
            <em class="mono">{{ row.fileHash || '-' }}</em>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型 / 大小" width="130">
        <template #default="{ row }">
          <div class="stack-cell">
            <strong>{{ row.fileType || '-' }}</strong>
            <span>{{ formatSize(row.fileSize) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <StatusTag :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="分片" width="90">
        <template #default="{ row }">{{ row.chunkCount || 0 }}</template>
      </el-table-column>
      <el-table-column label="最近索引" width="180">
        <template #default="{ row }">{{ formatDateTime(row.lastIndexedAt || row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="错误" min-width="180">
        <template #default="{ row }">
          <span class="error-text">{{ row.errorMessage || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <div class="table-operations">
            <el-button link type="primary" @click="handleReindex(row)">重建</el-button>
            <el-button link type="warning" :disabled="row.status === 'offline'" @click="handleOffline(row)">下线</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <div class="admin-pagination">
      <div class="pagination-info">{{ totalText }}</div>
      <el-pagination v-model:current-page="page.page" background layout="prev, pager, next" :page-size="page.pageSize" :total="page.total" @current-change="loadDocuments" />
    </div>
  </el-card>

  <el-card class="app-card job-card">
    <template #header>
      <div class="admin-table-title">
        <strong>索引任务</strong>
        <span class="admin-empty-note">启动扫描、手动扫描和单文档重建记录</span>
      </div>
    </template>
    <el-table v-loading="jobLoading" class="admin-table" :data="jobs">
      <el-table-column label="任务" width="120">
        <template #default="{ row }">#{{ row.id }}</template>
      </el-table-column>
      <el-table-column label="类型" width="130">
        <template #default="{ row }">{{ jobTypeLabel(row.jobType) }}</template>
      </el-table-column>
      <el-table-column label="文档ID" width="110">
        <template #default="{ row }">{{ row.documentId || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <StatusTag :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="进度" width="170">
        <template #default="{ row }">
          {{ row.indexedDocuments || 0 }} / {{ row.totalDocuments || 0 }}
          <span v-if="row.failedDocuments" class="error-text">失败 {{ row.failedDocuments }}</span>
        </template>
      </el-table-column>
      <el-table-column label="消息" min-width="220">
        <template #default="{ row }">{{ row.message || '-' }}</template>
      </el-table-column>
      <el-table-column label="开始时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.startedAt || row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.finishedAt) }}</template>
      </el-table-column>
    </el-table>
    <div class="admin-pagination">
      <div class="pagination-info">{{ jobTotalText }}</div>
      <el-pagination v-model:current-page="jobPage.page" background layout="prev, pager, next" :page-size="jobPage.pageSize" :total="jobPage.total" @current-change="loadJobs" />
    </div>
  </el-card>

  <el-dialog v-model="textDialogVisible" class="admin-dialog" title="录入知识库文本" width="680px">
    <el-form label-position="top">
      <el-form-item label="文件名">
        <el-input v-model="textForm.fileName" placeholder="例如：减脂晚餐建议.md" />
      </el-form-item>
      <el-form-item label="正文">
        <el-input v-model="textForm.content" type="textarea" :rows="14" placeholder="# 标题" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="textDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="textSubmitting" @click="submitTextDocument">保存并索引</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.hidden-file-input {
  display: none;
}

.knowledge-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.knowledge-stat {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: var(--admin-card-shadow);
}

.knowledge-stat__icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.knowledge-stat__icon--blue {
  color: #1677ff;
  background: #e6f4ff;
}

.knowledge-stat__icon--green {
  color: #52c41a;
  background: #f6ffed;
}

.knowledge-stat__icon--orange {
  color: #fa8c16;
  background: #fff7e6;
}

.knowledge-stat__icon--red {
  color: #ff4d4f;
  background: #fff1f0;
}

.knowledge-stat strong {
  display: block;
  font-size: 22px;
  line-height: 1.2;
  color: var(--admin-text-main);
}

.knowledge-stat div > span,
.pagination-info,
.document-cell span,
.document-cell em,
.stack-cell span {
  font-size: 12px;
  color: var(--admin-text-light);
}

.document-cell,
.stack-cell {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.document-cell strong,
.stack-cell strong {
  color: var(--admin-text-main);
  font-weight: 600;
}

.document-cell span,
.document-cell em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.error-text {
  color: #ff4d4f;
  font-size: 12px;
}

.job-card {
  margin-top: 20px;
}
</style>
