<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '@/components/EmptyState.vue'
import { listMessages, markRead, sendMessage, updateConversationSettings } from '@/api/message'
import { connectConversation } from '@/services/websocket'
import type { MessageItem } from '@/types/cook'
import { getStoredUser } from '@/utils/auth'
import { requireLogin } from '@/utils/format'

const conversationId = ref(0)
const title = ref('私信聊天')
const rows = ref<MessageItem[]>([])
const text = ref('')

const selfProfile = getStoredUser<{ id?: number; nickname?: string }>()
const selfId = Number(selfProfile?.id || 0)
const selfNickname = selfProfile?.nickname || '我'

async function loadData() {
  if (!requireLogin() || !conversationId.value) return
  const response = await listMessages(conversationId.value)
  rows.value = response.data?.items || []
  await markRead(conversationId.value)
}

async function submit() {
  if (!text.value.trim()) return
  await sendMessage(conversationId.value, { messageType: 'text', content: text.value })
  text.value = ''
  await loadData()
}

function isSelf(item: MessageItem) {
  if (selfId && item.senderId) return item.senderId === selfId
  return item.senderNickname === selfNickname || item.senderNickname === '我'
}

function goBack() {
  uni.navigateBack()
}

function showMore() {
  uni.showActionSheet({
    itemList: ['开启免打扰', '取消免打扰', '置顶聊天', '取消置顶'],
    success: async ({ tapIndex }) => {
      const actions = [
        { muted: true },
        { muted: false },
        { pinned: true },
        { pinned: false },
      ]
      await updateConversationSettings(conversationId.value, actions[tapIndex] || {})
      uni.showToast({ title: '会话设置已更新', icon: 'none' })
    },
  })
}

onLoad((options) => {
  conversationId.value = Number(options?.conversationId || 0)
  title.value = String(options?.title || '私信聊天')
})

onMounted(async () => {
  await loadData()
  if (conversationId.value) {
    connectConversation(conversationId.value, () => loadData())
  }
})
</script>

<template>
  <view class="page-body private-chat-page">
    <view class="chat-nav">
      <view class="chat-nav__back" @click="goBack">←</view>
      <view class="chat-nav__title">{{ title }}</view>
      <view class="chat-nav__more" @click="showMore">⋯</view>
    </view>

    <view class="chat-area">
      <EmptyState v-if="!rows.length" title="还没有聊天记录" description="发送第一条消息后，这里会展示完整会话。" />
      <view
        v-for="item in rows"
        :key="item.id"
        class="chat-row"
        :class="{ self: isSelf(item) }"
      >
        <view class="chat-row__avatar" :class="{ self: isSelf(item) }">{{ item.senderNickname?.slice(0, 1) || '厨' }}</view>
        <view class="chat-bubble">{{ item.content }}</view>
      </view>
    </view>

    <view class="input-bar">
      <view class="tool-btn">🎤</view>
      <input v-model="text" class="input-bar__field" placeholder="输入消息..." />
      <view class="tool-btn">😄</view>
      <view class="tool-btn">🖼️</view>
      <button class="send-btn" @click="submit">发送</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.private-chat-page {
  min-height: 100vh;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
  background: #f0f0f0;
}

.chat-nav {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 28rpx;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.chat-nav__back,
.chat-nav__more {
  width: 44rpx;
  color: var(--app-text-soft);
  font-size: 34rpx;
  text-align: center;
}

.chat-nav__title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--app-text);
}

.chat-area {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  padding: 24rpx 28rpx;
}

.time-divider {
  align-self: center;
  color: var(--app-text-muted);
  font-size: 20rpx;
}

.chat-row {
  display: flex;
  gap: 12rpx;
  max-width: 84%;
}

.chat-row.self {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.chat-row__avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, #ff9a9e, #fecfef);
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
}

.chat-row__avatar.self {
  background: linear-gradient(135deg, #a1c4fd, #c2e9fb);
}

.chat-bubble {
  padding: 18rpx 22rpx;
  border-radius: 24rpx;
  background: #fff;
  color: var(--app-text);
  font-size: 24rpx;
  line-height: 1.7;
  border-bottom-left-radius: 8rpx;
}

.chat-row.self .chat-bubble {
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  border-bottom-left-radius: 24rpx;
  border-bottom-right-radius: 8rpx;
}

.input-bar {
  position: fixed;
  right: 50%;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
  width: calc(100% - 56rpx);
  max-width: 804rpx;
  padding: 14rpx 18rpx calc(14rpx + env(safe-area-inset-bottom));
  transform: translateX(50%);
  background: rgba(255, 255, 255, 0.96);
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
}

.tool-btn {
  width: 42rpx;
  color: var(--app-text-muted);
  font-size: 32rpx;
  text-align: center;
}

.input-bar__field {
  flex: 1;
  height: 72rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #f5f5f5;
  font-size: 24rpx;
}

.send-btn {
  height: 64rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  font-size: 22rpx;
  line-height: 64rpx;
}
</style>
