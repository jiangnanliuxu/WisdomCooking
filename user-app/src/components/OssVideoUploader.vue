<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import {
  completeVideoMultipartUpload,
  getMediaAsset,
  initVideoMultipartUpload,
} from '@/api/operation'
import type { MediaAsset } from '@/types/cook'
import { resolveAssetUrl } from '@/utils/media'

interface DisplayAsset {
  id?: number
  url?: string
  hlsUrl?: string
  originalName?: string
  status?: string
}

const props = defineProps<{
  asset?: DisplayAsset | null
}>()

const emit = defineEmits<{
  (event: 'uploaded', asset: MediaAsset): void
  (event: 'fallback'): void
}>()

const uploading = ref(false)
const progress = ref(0)
const uploadText = ref('')
const errorText = ref('')
let mediaPollingTimer: ReturnType<typeof setTimeout> | undefined
let mediaPollingCount = 0

const mediaPollingLimit = 100
const mediaPollingInterval = 3000

const assetName = computed(() => props.asset?.originalName || '已添加教学视频')
const assetStatus = computed(() => {
  const status = props.asset?.status || ''
  const labelMap: Record<string, string> = {
    uploaded: '视频已上传，正在生成播放版本',
    transcoding: '视频已上传，正在生成播放版本',
    ready: '上传完成，视频已就绪',
    failed: '视频处理失败，请重新上传',
  }
  return labelMap[status] || status || ''
})

function isH5Runtime() {
  return typeof window !== 'undefined' && typeof document !== 'undefined'
}

function fingerprint(file: File) {
  return [file.name, file.size, file.lastModified].join(':')
}

function clearMediaPolling() {
  if (mediaPollingTimer) {
    clearTimeout(mediaPollingTimer)
    mediaPollingTimer = undefined
  }
}

function normalizeAsset(asset: MediaAsset): MediaAsset {
  return {
    ...asset,
    url: resolveAssetUrl(asset.url || ''),
    hlsUrl: resolveAssetUrl(asset.hlsUrl || ''),
  }
}

function isPlayableAsset(asset?: MediaAsset | null) {
  return asset?.status === 'ready' && Boolean(asset.hlsUrl)
}

function startMediaPolling(asset: MediaAsset) {
  clearMediaPolling()
  const normalizedAsset = normalizeAsset(asset)
  emit('uploaded', normalizedAsset)
  if (!normalizedAsset.id || isPlayableAsset(normalizedAsset) || normalizedAsset.status === 'failed') {
    return
  }

  mediaPollingCount = 0
  const poll = async () => {
    if (!normalizedAsset.id || mediaPollingCount >= mediaPollingLimit) {
      clearMediaPolling()
      return
    }
    let shouldContinue = true
    mediaPollingCount += 1
    try {
      const response = await getMediaAsset(normalizedAsset.id)
      if (response.data) {
        const nextAsset = normalizeAsset(response.data)
        emit('uploaded', nextAsset)
        if (isPlayableAsset(nextAsset) || nextAsset.status === 'failed') {
          clearMediaPolling()
          shouldContinue = false
          return
        }
      }
    }
    finally {
      if (shouldContinue) {
        mediaPollingTimer = setTimeout(poll, mediaPollingInterval)
      }
    }
  }
  mediaPollingTimer = setTimeout(poll, mediaPollingInterval)
}

function openFilePicker() {
  if (uploading.value) return
  if (!isH5Runtime()) {
    emit('fallback')
    return
  }

  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'video/mp4,video/*'
  input.onchange = () => {
    const file = input.files?.[0]
    if (file) {
      uploadFileToOss(file)
    }
  }
  input.click()
}

async function uploadFileToOss(file: File) {
  uploading.value = true
  progress.value = 0
  errorText.value = ''
  uploadText.value = '正在申请上传凭证'
  try {
    const initResponse = await initVideoMultipartUpload({
      fileName: file.name,
      fileSize: file.size,
      contentType: file.type || 'video/mp4',
      fingerprint: fingerprint(file),
    })
    const uploadInfo = initResponse.data
    if (!uploadInfo) {
      throw new Error('上传初始化失败')
    }
    if (uploadInfo.media) {
      startMediaPolling(uploadInfo.media)
      return
    }

    uploadText.value = '正在上传视频'
    const checkpointStorageKey = `cook:oss-video:${uploadInfo.checkpointKey}`
    const storedCheckpoint = uni.getStorageSync(checkpointStorageKey)
    const aliOssModule = await import('ali-oss')
    const OSSClient = ((aliOssModule as any).default || aliOssModule) as any
    const client = new OSSClient({
      region: uploadInfo.sts.region,
      endpoint: uploadInfo.sts.endpoint,
      bucket: uploadInfo.sts.bucket,
      accessKeyId: uploadInfo.sts.accessKeyId,
      accessKeySecret: uploadInfo.sts.accessKeySecret,
      stsToken: uploadInfo.sts.securityToken,
      secure: true,
    })

    await client.multipartUpload(uploadInfo.objectKey, file, {
      partSize: uploadInfo.partSize,
      checkpoint: storedCheckpoint || undefined,
      progress: (percent: number, checkpoint: unknown) => {
        progress.value = Math.max(0, Math.min(100, Math.round(percent * 100)))
        if (checkpoint) {
          uni.setStorageSync(checkpointStorageKey, serializeCheckpoint(checkpoint))
        }
      },
    })

    uploadText.value = '正在确认上传结果'
    const completeResponse = await completeVideoMultipartUpload({
      sessionId: uploadInfo.sessionId,
      objectKey: uploadInfo.objectKey,
    })
    uni.removeStorageSync(checkpointStorageKey)
    if (completeResponse.data) {
      startMediaPolling(completeResponse.data)
    }
  }
  catch (error) {
    const message = error instanceof Error ? error.message : '视频上传失败'
    errorText.value = message
    uni.showToast({ title: message, icon: 'none' })
  }
  finally {
    uploading.value = false
    uploadText.value = ''
  }
}

function serializeCheckpoint(checkpoint: unknown) {
  if (!checkpoint || typeof checkpoint !== 'object') {
    return checkpoint
  }
  const snapshot = { ...(checkpoint as Record<string, unknown>) }
  delete snapshot.file
  return snapshot
}

onBeforeUnmount(clearMediaPolling)
</script>

<template>
  <view class="oss-video-uploader" :class="{ active: !!asset || uploading }" @click="openFilePicker">
    <template v-if="uploading">
      <text class="icon">🎥</text>
      <text>{{ uploadText || '正在上传视频' }}</text>
      <view class="progress-bar">
        <view class="progress-bar__inner" :style="{ width: `${progress}%` }" />
      </view>
      <text class="hint">{{ progress }}%</text>
    </template>
    <template v-else-if="asset">
      <text class="icon">🎥</text>
      <text>{{ assetName }}</text>
      <text v-if="assetStatus" class="hint">{{ assetStatus }}</text>
      <text class="hint">点击更换教学视频</text>
    </template>
    <template v-else>
      <text class="icon">📹</text>
      <text>点击上传教学视频</text>
      <text class="hint">H5 使用 OSS 分片断点续传，视频不超过 500MB</text>
    </template>
    <text v-if="errorText" class="error-text">{{ errorText }}</text>
  </view>
</template>

<style scoped lang="scss">
.oss-video-uploader {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  min-height: 220rpx;
  margin-top: 18rpx;
  padding: 24rpx;
  border: 2rpx dashed rgba(15, 23, 42, 0.12);
  border-radius: 22rpx;
  color: var(--app-text-muted);
  text-align: center;
}

.oss-video-uploader.active {
  border-style: solid;
}

.icon {
  font-size: 54rpx;
}

.hint {
  font-size: 20rpx;
}

.error-text {
  color: var(--app-danger);
  font-size: 20rpx;
}

.progress-bar {
  width: 80%;
  height: 8rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.1);
  overflow: hidden;
}

.progress-bar__inner {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
}
</style>
