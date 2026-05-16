<script setup lang="ts">
import { nextTick, onBeforeUnmount, watch, ref } from 'vue'

const props = defineProps<{
  src?: string
  hlsSrc?: string
  poster?: string
  label?: string
  status?: string
}>()

const videoRef = ref<unknown>()
let player: any
let hls: any
let setupToken = 0
let fallbackTimer: ReturnType<typeof setTimeout> | null = null
let usingFallback = false

function isHlsSource(value?: string) {
  return Boolean(value && value.includes('.m3u8'))
}

function isMp4Source(value?: string) {
  return Boolean(value && value.toLowerCase().includes('.mp4'))
}

function disposeHls() {
  if (hls) {
    hls.destroy()
    hls = null
  }
}

function clearFallbackTimer() {
  if (fallbackTimer) {
    clearTimeout(fallbackTimer)
    fallbackTimer = null
  }
}

function disposePlayer() {
  clearFallbackTimer()
  disposeHls()
  if (player) {
    player.dispose()
    player = null
  }
}

function resolveNativeVideo() {
  const raw = videoRef.value as any
  const element = raw instanceof HTMLElement ? raw : raw?.$el
  if (!element) return null
  if (element instanceof HTMLVideoElement) {
    return element
  }
  return typeof element.querySelector === 'function' ? element.querySelector('video') : null
}

function fallbackToMp4(shouldResume = false) {
  if (!player || !props.src || usingFallback) return

  const video = resolveNativeVideo()
  const resumePlayback = shouldResume || Boolean(video && !video.paused)
  usingFallback = true
  clearFallbackTimer()
  disposeHls()
  if (typeof player.error === 'function') {
    player.error(null)
  }
  player.src(isMp4Source(props.src) ? { src: props.src, type: 'video/mp4' } : { src: props.src })
  if (props.poster) {
    player.poster(props.poster)
  }
  if (resumePlayback && typeof player.play === 'function') {
    player.play()?.catch?.(() => {})
  }
}

function watchHlsStartup(token: number) {
  clearFallbackTimer()
  if (!props.src) return
  fallbackTimer = setTimeout(() => {
    const video = resolveNativeVideo()
    if (token !== setupToken || usingFallback || !video) return
    if (video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA && video.currentTime === 0) {
      fallbackToMp4(true)
    }
  }, 5000)
}

async function setupPlayer() {
  const token = ++setupToken
  await nextTick()
  const video = resolveNativeVideo()
  if (!video || token !== setupToken) return

  const hlsSource = props.hlsSrc || (isHlsSource(props.src) ? props.src : '')
  const source = hlsSource || props.src || ''
  if (!source) return

  clearFallbackTimer()
  disposeHls()
  usingFallback = false
  if (!player) {
    const videojsModule = await import('video.js')
    if (token !== setupToken) return
    const videojs = (videojsModule as any).default || videojsModule
    player = videojs(video, {
      controls: true,
      preload: 'metadata',
      fluid: false,
      poster: props.poster,
    })
    player.on('error', () => fallbackToMp4(true))
  }

  const shouldUseHls = Boolean(hlsSource)
  if (shouldUseHls && video.canPlayType('application/vnd.apple.mpegurl')) {
    player.src({ src: hlsSource, type: 'application/x-mpegURL' })
    watchHlsStartup(token)
    return
  }

  if (shouldUseHls) {
    const hlsModule = await import('hls.js')
    if (token !== setupToken) return
    const Hls = (hlsModule as any).default || hlsModule
    if (Hls.isSupported()) {
      hls = new Hls()
      hls.on(Hls.Events.ERROR, (_event: string, data: any) => {
        if (data?.fatal) {
          fallbackToMp4(true)
        }
      })
      hls.loadSource(hlsSource)
      hls.attachMedia(video)
      watchHlsStartup(token)
      return
    }
  }

  if (props.src) {
    player.src(isMp4Source(props.src) ? { src: props.src, type: 'video/mp4' } : { src: props.src })
  }
}

watch(
  () => [props.src, props.hlsSrc, props.poster],
  () => {
    setupPlayer()
  },
  { immediate: true },
)

onBeforeUnmount(disposePlayer)
</script>

<template>
  <view class="hls-video-player">
    <!-- #ifdef H5 -->
    <video ref="videoRef" class="video-js vjs-default-skin hls-video-player__media" controls playsinline webkit-playsinline />
    <!-- #endif -->
    <!-- #ifndef H5 -->
    <video class="hls-video-player__media" :src="hlsSrc || src" :poster="poster" controls object-fit="cover" />
    <!-- #endif -->
    <view v-if="label || status" class="hls-video-player__badge">
      {{ label || '视频' }}<text v-if="status"> · {{ status }}</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
@import 'video.js/dist/video-js.css';

.hls-video-player {
  position: relative;
  width: 100%;
  height: 100%;
  background: #0f172a;
}

.hls-video-player__media,
:deep(.video-js) {
  width: 100%;
  height: 100%;
}

:deep(.video-js .vjs-tech) {
  object-fit: cover;
}

:deep(.video-js .vjs-big-play-button) {
  z-index: 3;
}

:deep(.video-js .vjs-control-bar) {
  z-index: 4;
}

:deep(.video-js.vjs-paused .vjs-tech) {
  pointer-events: none;
}

:deep(.video-js .vjs-text-track-display) {
  pointer-events: none;
}

.hls-video-player__badge {
  position: absolute;
  top: 22rpx;
  right: 22rpx;
  z-index: 2;
  padding: 8rpx 16rpx;
  border-radius: 12rpx;
  background: rgba(0, 0, 0, 0.42);
  color: #fff;
  font-size: 20rpx;
}
</style>
