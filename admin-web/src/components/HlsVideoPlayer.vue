<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps<{
  src?: string
  hlsSrc?: string
  poster?: string
}>()

const videoRef = ref<HTMLVideoElement>()
let player: any
let hls: any
let setupToken = 0

function disposeHls() {
  if (hls) {
    hls.destroy()
    hls = null
  }
}

function disposePlayer() {
  disposeHls()
  if (player) {
    player.dispose()
    player = null
  }
}

async function setupPlayer() {
  const token = ++setupToken
  await nextTick()
  const video = videoRef.value
  if (!video || token !== setupToken) return

  const source = props.hlsSrc || props.src || ''
  if (!source) return

  disposeHls()
  if (!player) {
    const videojsModule = await import('video.js')
    if (token !== setupToken) return
    const videojs = (videojsModule as any).default || videojsModule
    player = videojs(video, {
      controls: true,
      preload: 'metadata',
      fluid: true,
      poster: props.poster,
    })
  }

  const useHls = Boolean(props.hlsSrc || source.includes('.m3u8'))
  if (useHls && video.canPlayType('application/vnd.apple.mpegurl')) {
    player.src({ src: source, type: 'application/x-mpegURL' })
    return
  }

  if (useHls) {
    const hlsModule = await import('hls.js')
    if (token !== setupToken) return
    const Hls = (hlsModule as any).default || hlsModule
    if (Hls.isSupported()) {
      hls = new Hls()
      hls.loadSource(source)
      hls.attachMedia(video)
      return
    }
  }

  if (props.src) {
    player.src({ src: props.src, type: 'video/mp4' })
  }
}

watch(
  () => [props.src, props.hlsSrc, props.poster],
  () => setupPlayer(),
  { immediate: true },
)

onBeforeUnmount(disposePlayer)
</script>

<template>
  <div class="hls-video-player">
    <video ref="videoRef" class="video-js vjs-default-skin" controls playsinline />
  </div>
</template>

<style scoped>
@import 'video.js/dist/video-js.css';

.hls-video-player {
  width: 100%;
  overflow: hidden;
  border-radius: 10px;
  background: #0f172a;
}

:deep(.video-js) {
  width: 100%;
  min-height: 280px;
}

:deep(.video-js .vjs-tech) {
  object-fit: contain;
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
</style>
