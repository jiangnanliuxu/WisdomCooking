<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import {
  createBanner,
  deleteBanner,
  getMediaAsset,
  listBanners,
  listMediaAssets,
  moveBanner,
  offlineBanner,
  onlineBanner,
  updateBanner,
  uploadAdminImage,
} from '@/api/operation'
import PageHeader from '@/components/PageHeader.vue'
import type { BannerItem, MediaAsset } from '@/types/cook'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const dialogVisible = ref(false)
const resourceDialogVisible = ref(false)
const mediaLoading = ref(false)
const mediaUploading = ref(false)
const rows = ref<BannerItem[]>([])
const mediaRows = ref<MediaAsset[]>([])
const selectedMedia = ref<MediaAsset | null>(null)
const editingId = ref<number>()

const page = reactive({ page: 1, pageSize: 10, total: 0 })
const mediaPage = reactive({ page: 1, pageSize: 9, total: 0 })
const filters = reactive({ status: '', keyword: '' })
const mediaFilters = reactive({ fileType: 'image', keyword: '' })

const form = reactive({
  title: '',
  subtitle: '',
  imageMediaId: undefined as number | undefined,
  jumpType: 'recipe',
  jumpTarget: '',
  sortNo: 0,
  status: 'offline',
  startAt: '',
  endAt: '',
})

const summaryCards = computed(() => {
  const onlineCount = rows.value.filter(item => item.status === 'online').length
  const totalClicks = rows.value.reduce((sum, item) => sum + (item.clickCount || 0), 0)
  const scheduledCount = rows.value.filter(item => item.startAt || item.endAt).length
  return [
    { label: '轮播总数', value: page.total || rows.value.length, icon: '🖼️', tone: 'card-blue' },
    { label: '已上架', value: onlineCount, icon: '✅', tone: 'card-green' },
    { label: '累计点击', value: totalClicks, icon: '👆', tone: 'card-orange' },
    { label: '有投放时间', value: scheduledCount, icon: '📅', tone: 'card-purple' },
  ]
})

const previewTitle = computed(() => form.title || '轮播图标题')
const previewSubtitle = computed(() => form.subtitle || '这里显示轮播图描述文案')
const previewLink = computed(() => `${jumpTypeLabel(form.jumpType)} · ${form.jumpTarget || '未设置跳转目标'}`)
const selectedMediaUrl = computed(() => resolveMediaUrl(selectedMedia.value))
const selectedMediaName = computed(() => selectedMedia.value?.originalName || (form.imageMediaId ? `已选择资源 #${form.imageMediaId}` : '未选择图片资源'))
const selectedMediaMeta = computed(() => {
  if (!selectedMedia.value) {
    return '建议上传 750 × 330 的 PNG / JPG / WEBP 图片。'
  }

  const segments = [`资源 ID ${selectedMedia.value.id}`]
  if (selectedMedia.value.sizeBytes) {
    segments.push(formatFileSize(selectedMedia.value.sizeBytes))
  }
  if (selectedMedia.value.createdAt) {
    segments.push(`上传于 ${formatDateTime(selectedMedia.value.createdAt)}`)
  }
  return segments.join(' · ')
})

function gradientStyle(seed = 0) {
  const palette = [
    'linear-gradient(135deg,#ff6b35,#ff8f5e)',
    'linear-gradient(135deg,#1890ff,#69c0ff)',
    'linear-gradient(135deg,#52c41a,#95de64)',
    'linear-gradient(135deg,#722ed1,#b37feb)',
    'linear-gradient(135deg,#fa8c16,#ffc53d)',
  ]
  return { background: palette[Math.abs(seed) % palette.length] }
}

function imageUrl(row: BannerItem) {
  return row.imageUrl
}

function resolveMediaUrl(asset?: Pick<MediaAsset, 'url'> | null) {
  return asset?.url || ''
}

function jumpTypeLabel(type?: string) {
  if (type === 'recipe') return '跳转菜谱'
  if (type === 'post') return '跳转动态'
  if (type === 'url') return '外部链接'
  return type || '未设置类型'
}

function statusLabel(status?: string) {
  return status === 'online' ? '已上架' : '已下架'
}

function formatSchedule(row: BannerItem) {
  if (!row.startAt && !row.endAt) return '长期展示'
  return `${row.startAt ? formatDateTime(row.startAt) : '即时'} - ${row.endAt ? formatDateTime(row.endAt) : '长期'}`
}

function formatFileSize(sizeBytes?: number) {
  if (!sizeBytes) return '未知大小'
  if (sizeBytes < 1024) return `${sizeBytes} B`
  if (sizeBytes < 1024 * 1024) return `${(sizeBytes / 1024).toFixed(1)} KB`
  return `${(sizeBytes / 1024 / 1024).toFixed(1)} MB`
}

async function loadData() {
  loading.value = true
  try {
    const response = await listBanners({ ...filters, page: page.page, pageSize: page.pageSize })
    rows.value = response.data?.items || []
    page.total = response.data?.total || 0
  }
  finally {
    loading.value = false
  }
}

async function loadMediaData() {
  mediaLoading.value = true
  try {
    const response = await listMediaAssets({
      ...mediaFilters,
      page: mediaPage.page,
      pageSize: mediaPage.pageSize,
    })
    mediaRows.value = response.data?.items || []
    mediaPage.total = response.data?.total || 0
  }
  finally {
    mediaLoading.value = false
  }
}

function resetForm() {
  editingId.value = undefined
  dialogVisible.value = false
  resourceDialogVisible.value = false
  form.title = ''
  form.subtitle = ''
  form.imageMediaId = undefined
  form.jumpType = 'recipe'
  form.jumpTarget = ''
  form.sortNo = 0
  form.status = 'offline'
  form.startAt = ''
  form.endAt = ''
  selectedMedia.value = null
}

function resetFilters() {
  filters.status = ''
  filters.keyword = ''
  page.page = 1
  loadData()
}

function resetMediaFilters() {
  mediaFilters.keyword = ''
  mediaPage.page = 1
  loadMediaData()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: BannerItem) {
  resetForm()
  editingId.value = row.id
  form.title = row.title
  form.subtitle = row.subtitle || ''
  form.imageMediaId = row.imageMediaId
  form.jumpType = row.jumpType || 'recipe'
  form.jumpTarget = row.jumpTarget || ''
  form.sortNo = row.sortNo || 0
  form.status = row.status || 'offline'
  form.startAt = row.startAt || ''
  form.endAt = row.endAt || ''
  dialogVisible.value = true
  await hydrateSelectedMedia(row.imageMediaId, {
    id: row.imageMediaId,
    originalName: row.title,
    url: row.imageUrl,
    createdAt: row.createdAt,
  })
}

function openResourceDialog() {
  resourceDialogVisible.value = true
  mediaPage.page = 1
  loadMediaData()
}

function selectMedia(asset: MediaAsset) {
  form.imageMediaId = asset.id
  selectedMedia.value = asset
  resourceDialogVisible.value = false
}

function clearSelectedMedia() {
  form.imageMediaId = undefined
  selectedMedia.value = null
}

async function hydrateSelectedMedia(mediaId?: number, fallback?: Partial<MediaAsset>) {
  if (!mediaId) {
    selectedMedia.value = null
    return
  }

  try {
    const response = await getMediaAsset(mediaId)
    selectedMedia.value = response.data
      ? { ...fallback, ...response.data }
      : { id: mediaId, ...fallback } as MediaAsset
  }
  catch {
    selectedMedia.value = fallback ? { id: mediaId, ...fallback } as MediaAsset : null
  }
}

async function submitForm() {
  if (!form.imageMediaId) {
    ElMessage.warning('请先上传或选择轮播图图片')
    return
  }

  const payload = {
    title: form.title,
    subtitle: form.subtitle,
    imageMediaId: Number(form.imageMediaId),
    jumpType: form.jumpType,
    jumpTarget: form.jumpTarget,
    sortNo: form.sortNo,
    status: form.status,
    startAt: form.startAt || null,
    endAt: form.endAt || null,
  }

  if (editingId.value) {
    await updateBanner(editingId.value, payload)
  }
  else {
    await createBanner(payload)
  }

  dialogVisible.value = false
  await loadData()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该轮播图吗？删除后不可恢复。', '删除确认', { type: 'warning' })
  await deleteBanner(id)
  await loadData()
}

async function handleToggle(row: BannerItem) {
  if (row.status === 'online') {
    await offlineBanner(row.id)
  }
  else {
    await onlineBanner(row.id)
  }
  await loadData()
}

async function handleSort(row: BannerItem, step: number) {
  await moveBanner(row.id, (row.sortNo || 0) + step)
  await loadData()
}

function beforeImageUpload(file: File) {
  const isAllowedType = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  if (!isAllowedType) {
    ElMessage.error('请上传 JPG、PNG 或 WEBP 图片')
    return false
  }

  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }

  return true
}

async function handleImageUpload(options: UploadRequestOptions) {
  mediaUploading.value = true
  try {
    const response = await uploadAdminImage(options.file)
    const media = response.data
    if (!media) {
      throw new Error('上传结果为空')
    }
    form.imageMediaId = media.id
    selectedMedia.value = media
    resourceDialogVisible.value = false
    ElMessage.success('图片上传成功')
    options.onSuccess(media)
  }
  catch (error) {
    const uploadError = error instanceof Error ? error : new Error('上传失败')
    ElMessage.error(uploadError.message || '图片上传失败')
  }
  finally {
    mediaUploading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <PageHeader title="轮播图管理" description="管理首页轮播图内容、上下架状态、展示顺序和跳转配置。">
    <template #actions>
      <el-button type="primary" @click="openCreate">
        新增轮播图
      </el-button>
    </template>
  </PageHeader>

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

  <div class="tip-bar">
    建议尺寸 750 × 330px，支持 JPG/PNG/WEBP。图片可以直接上传，也可以从后台资源库选择已有素材。
  </div>

  <div class="page-toolbar">
    <div class="page-toolbar__filters">
      <el-select v-model="filters.status" clearable style="width: 160px;" placeholder="状态">
        <el-option label="已上架" value="online" />
        <el-option label="已下架" value="offline" />
      </el-select>
      <el-input v-model="filters.keyword" style="width: 240px;" clearable placeholder="搜索标题或副标题" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">
        查询
      </el-button>
      <el-button @click="resetFilters">
        重置
      </el-button>
    </div>
  </div>

  <div v-loading="loading" class="banner-grid">
    <article v-for="row in rows" :key="row.id" class="banner-card">
      <div class="banner-card__visual">
        <el-image v-if="imageUrl(row)" :src="imageUrl(row)" fit="cover" class="banner-card__image" />
        <div v-else class="banner-card__image banner-card__image--placeholder" :style="gradientStyle(row.id)">
          {{ row.title.slice(0, 1) }}
        </div>
        <div class="banner-card__sort">
          排序 {{ row.sortNo || 0 }}
        </div>
        <div class="banner-card__status" :class="row.status === 'online' ? 'banner-card__status--online' : 'banner-card__status--offline'">
          {{ statusLabel(row.status) }}
        </div>
      </div>

      <div class="banner-card__body">
        <div class="banner-card__title">
          {{ row.title }}
        </div>
        <div class="banner-card__subtitle">
          {{ row.subtitle || '暂无副标题文案' }}
        </div>
        <div class="banner-card__meta">
          <span>{{ formatSchedule(row) }}</span>
          <span>点击 {{ row.clickCount || 0 }}</span>
          <span>{{ jumpTypeLabel(row.jumpType) }}</span>
        </div>
        <div class="banner-card__link">
          {{ row.jumpTarget || '未设置跳转目标' }}
        </div>
      </div>

      <div class="banner-card__actions">
        <el-button plain @click="openEdit(row)">
          编辑
        </el-button>
        <el-button plain @click="handleSort(row, -1)">
          上移
        </el-button>
        <el-button plain @click="handleSort(row, 1)">
          下移
        </el-button>
        <el-button :type="row.status === 'online' ? 'warning' : 'success'" plain @click="handleToggle(row)">
          {{ row.status === 'online' ? '下架' : '上架' }}
        </el-button>
        <el-button type="danger" plain @click="handleDelete(row.id)">
          删除
        </el-button>
      </div>
    </article>

    <el-empty v-if="!rows.length && !loading" description="暂无轮播图数据" />
  </div>

  <div class="admin-pagination">
    <div class="pagination-info">
      共 {{ page.total }} 条记录
    </div>
    <el-pagination v-model:current-page="page.page" background layout="prev, pager, next" :total="page.total" @current-change="loadData" />
  </div>

  <el-dialog v-model="dialogVisible" class="admin-dialog" :title="editingId ? '编辑轮播图' : '新增轮播图'" width="760px">
    <div class="preview-banner">
      <div class="preview-banner__image" :style="!selectedMediaUrl ? gradientStyle(form.sortNo || Number(form.imageMediaId) || 1) : undefined">
        <el-image v-if="selectedMediaUrl" :src="selectedMediaUrl" fit="cover" class="preview-banner__image-el" />
        <span v-else class="preview-banner__fallback">
          {{ previewTitle.slice(0, 1) }}
        </span>
      </div>
      <div class="preview-banner__body">
        <div class="preview-banner__title">
          {{ previewTitle }}
        </div>
        <div class="preview-banner__subtitle">
          {{ previewSubtitle }}
        </div>
        <div class="preview-banner__link">
          {{ previewLink }}
        </div>
      </div>
    </div>

    <el-form label-position="top">
      <el-form-item class="admin-form-full" label="轮播图图片">
        <div class="media-field">
          <div class="media-field__preview">
            <el-image v-if="selectedMediaUrl" :src="selectedMediaUrl" fit="cover" class="media-field__image" />
            <div v-else class="media-field__placeholder" :style="gradientStyle(Number(form.imageMediaId) || 1)">
              {{ previewTitle.slice(0, 1) }}
            </div>
          </div>
          <div class="media-field__content">
            <div class="media-field__title">
              {{ selectedMediaName }}
            </div>
            <div class="media-field__meta">
              {{ selectedMediaMeta }}
            </div>
            <div v-if="selectedMedia?.url" class="media-field__meta media-field__meta--link">
              {{ selectedMedia.url }}
            </div>
            <div class="media-field__actions">
              <el-upload
                class="media-upload"
                accept="image/png,image/jpeg,image/webp"
                :before-upload="beforeImageUpload"
                :http-request="handleImageUpload"
                :show-file-list="false"
              >
                <el-button type="primary" plain :loading="mediaUploading">
                  上传图片
                </el-button>
              </el-upload>
              <el-button plain @click="openResourceDialog">
                从资源库选择
              </el-button>
              <el-button v-if="form.imageMediaId" text @click="clearSelectedMedia">
                清空已选
              </el-button>
            </div>
          </div>
        </div>
      </el-form-item>

      <div class="admin-form-grid">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="跳转类型">
          <el-select v-model="form.jumpType">
            <el-option label="菜谱" value="recipe" />
            <el-option label="动态" value="post" />
            <el-option label="外链" value="url" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model.number="form.sortNo" type="number" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="下架" value="offline" />
            <el-option label="上架" value="online" />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转目标">
          <el-input v-model="form.jumpTarget" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-input v-model="form.startAt" placeholder="2026-05-12T10:00:00" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-input v-model="form.endAt" placeholder="2026-05-31T23:59:59" />
        </el-form-item>
      </div>

      <el-form-item class="admin-form-full" label="副标题">
        <el-input v-model="form.subtitle" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">
        取消
      </el-button>
      <el-button type="primary" @click="submitForm">
        保存
      </el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="resourceDialogVisible" class="media-library-dialog" title="选择图片资源" width="920px">
    <div class="media-library__toolbar">
      <div class="media-library__filters">
        <el-input v-model="mediaFilters.keyword" clearable style="width: 260px;" placeholder="搜索文件名" @keyup.enter="loadMediaData" />
        <el-button type="primary" @click="loadMediaData">
          查询
        </el-button>
        <el-button @click="resetMediaFilters">
          重置
        </el-button>
      </div>
      <el-upload
        class="media-upload"
        accept="image/png,image/jpeg,image/webp"
        :before-upload="beforeImageUpload"
        :http-request="handleImageUpload"
        :show-file-list="false"
      >
        <el-button type="primary" :loading="mediaUploading">
          上传并选中
        </el-button>
      </el-upload>
    </div>

    <div v-loading="mediaLoading" class="media-library">
      <div class="media-library__grid">
        <article
          v-for="asset in mediaRows"
          :key="asset.id"
          class="media-library__card"
          :class="{ 'media-library__card--active': asset.id === form.imageMediaId }"
          @click="selectMedia(asset)"
        >
          <div class="media-library__thumb">
            <el-image v-if="resolveMediaUrl(asset)" :src="resolveMediaUrl(asset)" fit="cover" class="media-library__thumb-image" />
            <div v-else class="media-library__thumb-placeholder" :style="gradientStyle(asset.id)">
              {{ (asset.originalName || '图').slice(0, 1) }}
            </div>
          </div>
          <div class="media-library__body">
            <div class="media-library__name">
              {{ asset.originalName || `资源 #${asset.id}` }}
            </div>
            <div class="media-library__info">
              ID {{ asset.id }} · {{ formatFileSize(asset.sizeBytes) }}
            </div>
            <div class="media-library__info">
              {{ asset.createdAt ? formatDateTime(asset.createdAt) : '时间未知' }}
            </div>
          </div>
          <div class="media-library__footer">
            <el-button type="primary" link @click.stop="selectMedia(asset)">
              选择
            </el-button>
          </div>
        </article>
      </div>

      <el-empty v-if="!mediaRows.length && !mediaLoading" description="暂无可用图片资源" />
    </div>

    <div class="admin-pagination admin-pagination--compact">
      <div class="pagination-info">
        共 {{ mediaPage.total }} 条资源
      </div>
      <el-pagination v-model:current-page="mediaPage.page" background layout="prev, pager, next" :page-size="mediaPage.pageSize" :total="mediaPage.total" @current-change="loadMediaData" />
    </div>
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
.card-purple { background: #f9f0ff; }

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

.tip-bar {
  margin-bottom: 20px;
  padding: 12px 16px;
  border-radius: 10px;
  background: #e6f7ff;
  color: #1890ff;
  font-size: 13px;
  line-height: 1.7;
}

.banner-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.banner-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: var(--admin-card-shadow);
  overflow: hidden;
  transition: all 0.2s ease;
}

.banner-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.banner-card__visual {
  position: relative;
}

.banner-card__image {
  width: 100%;
  aspect-ratio: 16 / 7;
  overflow: hidden;
  background: #f0f0f0;
}

.banner-card__image--placeholder,
.preview-banner__fallback,
.media-field__placeholder,
.media-library__thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 40px;
  font-weight: 700;
}

.banner-card__sort,
.banner-card__status {
  position: absolute;
  top: 12px;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
}

.banner-card__sort {
  left: 12px;
  background: rgba(15, 23, 42, 0.68);
  color: #fff;
}

.banner-card__status {
  right: 12px;
}

.banner-card__status--online {
  background: rgba(82, 196, 26, 0.92);
  color: #fff;
}

.banner-card__status--offline {
  background: rgba(15, 23, 42, 0.56);
  color: #fff;
}

.banner-card__body {
  padding: 16px;
}

.banner-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.banner-card__subtitle,
.banner-card__meta,
.banner-card__link,
.preview-banner__subtitle,
.preview-banner__link {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #666;
}

.banner-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.banner-card__link,
.preview-banner__link,
.media-field__meta--link {
  color: #1890ff;
}

.banner-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 16px 16px;
}

.preview-banner {
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
  border: 1px solid #f0f0f0;
}

.preview-banner__image {
  width: 100%;
  aspect-ratio: 16 / 7;
  overflow: hidden;
  background: #f5f5f5;
}

.preview-banner__image-el,
.media-field__image,
.media-library__thumb-image {
  width: 100%;
  height: 100%;
  display: block;
}

.preview-banner__body {
  padding: 14px 16px;
  background: #fafafa;
}

.preview-banner__title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.media-field {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 16px;
  width: 100%;
}

.media-field__preview {
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 16 / 7;
  background: #f5f5f5;
  border: 1px solid #f0f0f0;
}

.media-field__content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.media-field__title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.media-field__meta {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #666;
  word-break: break-all;
}

.media-field__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.media-upload :deep(.el-upload) {
  display: inline-flex;
}

.media-library__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.media-library__filters {
  display: flex;
  align-items: center;
  gap: 12px;
}

.media-library__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.media-library__card {
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.media-library__card:hover,
.media-library__card--active {
  border-color: #1890ff;
  box-shadow: 0 8px 20px rgba(24, 144, 255, 0.12);
  transform: translateY(-1px);
}

.media-library__thumb {
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #f5f5f5;
}

.media-library__body {
  padding: 12px 14px 10px;
}

.media-library__name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  line-height: 1.6;
  word-break: break-all;
}

.media-library__info {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
  line-height: 1.6;
}

.media-library__footer {
  padding: 0 14px 12px;
}

.admin-pagination--compact {
  margin-top: 16px;
}

@media (max-width: 1200px) {
  .stats-row,
  .banner-grid,
  .media-library__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .media-field,
  .media-library__toolbar,
  .media-library__filters {
    grid-template-columns: 1fr;
    display: grid;
  }

  .media-field {
    gap: 12px;
  }
}
</style>
