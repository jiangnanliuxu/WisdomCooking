<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { createGroup } from '@/api/group'
import { listConversations, listNotifications } from '@/api/message'
import type { ConversationItem, MessageItem } from '@/types/cook'
import { requireLogin } from '@/utils/format'

const active = ref<'private' | 'group' | 'notification'>('private')
const conversations = ref<ConversationItem[]>([])
const notifications = ref<MessageItem[]>([])
const groupPopup = ref(false)
const groupForm = reactive({ name: '', intro: '' })

const visibleConversations = computed(() => conversations.value.filter(item => item.type === active.value))
const unreadTotal = computed(() => conversations.value.reduce((sum, item) => sum + (item.unreadCount || 0), 0) + notifications.value.length)
const privateCount = computed(() => conversations.value.filter(item => item.type === 'private').length)
const groupCount = computed(() => conversations.value.filter(item => item.type === 'group').length)

function hasToken() {
  return Boolean(uni.getStorageSync('cook_user_token'))
}

async function loadData() {
  if (!hasToken()) {
    conversations.value = []
    notifications.value = []
    return
  }
  try {
    const [conversationRes, notificationRes] = await Promise.all([
      listConversations(active.value === 'notification' ? '' : active.value),
      listNotifications(),
    ])
    conversations.value = conversationRes.data?.items || []
    notifications.value = notificationRes.data?.items || []
  }
  catch {
    conversations.value = []
    notifications.value = []
  }
}

function openConversation(item: ConversationItem) {
  if (item.type === 'group') {
    uni.navigateTo({ url: `/pages/message/group?groupId=${item.targetId || item.id}` })
  }
  else if (item.type === 'private') {
    uni.navigateTo({ url: `/pages/message/private?conversationId=${item.id}&title=${item.title || ''}` })
  }
}

function openGroupPopup() {
  if (!requireLogin()) return
  groupPopup.value = true
}

async function submitGroup() {
  if (!groupForm.name.trim()) {
    uni.showToast({ title: '请填写群名称', icon: 'none' })
    return
  }
  const response = await createGroup({ name: groupForm.name, intro: groupForm.intro })
  groupPopup.value = false
  uni.navigateTo({ url: `/pages/message/group?groupId=${response.data?.id}` })
}

function switchTab(tab: 'private' | 'group' | 'notification') {
  active.value = tab
  loadData()
}

onMounted(loadData)
</script>

<template>
  <view class="page-body message-page">
    <view class="app-page-head">
      <view class="app-page-head__row">
        <view class="head-placeholder" />
        <view class="section-title">消息</view>
        <view class="icon-action" @click="openGroupPopup">＋</view>
      </view>
      <view class="search-box">
        <text>⌕</text>
        <text>搜索联系人或群组...</text>
      </view>
    </view>

    <scroll-view scroll-x class="summary-scroll">
      <view class="summary-list">
        <view class="summary-chip surface-card">
          <view class="label">全部未读</view>
          <view class="value">{{ unreadTotal }}</view>
          <view class="hint">真实会话未读</view>
        </view>
        <view class="summary-chip surface-card">
          <view class="label">私信会话</view>
          <view class="value">{{ privateCount }}</view>
          <view class="hint">最近联系人</view>
        </view>
        <view class="summary-chip surface-card">
          <view class="label">互动通知</view>
          <view class="value">{{ notifications.length }}</view>
          <view class="hint">评论和点赞</view>
        </view>
        <view class="summary-chip surface-card">
          <view class="label">群聊会话</view>
          <view class="value">{{ groupCount }}</view>
          <view class="hint">我的群组</view>
        </view>
      </view>
    </scroll-view>

    <view class="segmented-tabs msg-tabs">
      <view class="segmented-tab" :class="{ active: active === 'private' }" @click="switchTab('private')">私信</view>
      <view class="segmented-tab" :class="{ active: active === 'group' }" @click="switchTab('group')">群聊</view>
      <view class="segmented-tab" :class="{ active: active === 'notification' }" @click="switchTab('notification')">通知</view>
    </view>

    <view class="message-content">
      <view v-if="active !== 'notification' && visibleConversations.length" class="msg-list">
        <view v-for="(item, index) in visibleConversations" :key="item.id" class="msg-item surface-card" @click="openConversation(item)">
          <view class="avatar" :class="{ group: item.type === 'group' }" :style="{ background: index % 2 === 0 ? 'linear-gradient(135deg,#ff9a9e,#fecfef)' : 'linear-gradient(135deg,#84fab0,#8fd3f4)' }">
            {{ item.type === 'group' ? '👥' : '厨' }}
            <view v-if="item.type === 'private'" class="online-dot" />
          </view>
          <view class="info">
            <view class="top-row">
              <text class="name">{{ item.title || '未命名会话' }}</text>
              <text class="time">{{ item.lastMessageAt || '-' }}</text>
            </view>
            <view class="last-msg">{{ item.lastMessagePreview || '暂无消息' }}</view>
            <view class="tag-row">
              <text class="tag">{{ item.type === 'group' ? '群聊活跃' : '作者已回复' }}</text>
            </view>
          </view>
          <view v-if="item.unreadCount" class="unread">{{ item.unreadCount }}</view>
        </view>
      </view>

      <view v-else-if="active === 'notification' && notifications.length" class="notice-list">
        <view v-for="(item, index) in notifications" :key="item.id" class="notice-card surface-card">
          <view class="notice-icon" :class="`notice-${index % 3}`">{{ ['♥', '!', '📢'][index % 3] }}</view>
          <view class="notice-body">
            <view class="notice-top">
              <text class="notice-title">{{ item.senderNickname || '系统通知' }}</text>
              <text class="notice-time">{{ item.createdAt || '-' }}</text>
            </view>
            <view class="notice-desc">{{ item.content }}</view>
            <view class="notice-meta">
              <text class="notice-badge">{{ index === 1 ? '系统消息' : index === 2 ? '群组通知' : '互动通知' }}</text>
              <text class="notice-cta">去看看</text>
            </view>
          </view>
        </view>
      </view>

      <EmptyState v-else title="暂无消息" description="消息中心固定包含私信、群聊、通知三个分区。" />
    </view>

    <view v-if="groupPopup" class="popup-mask">
      <view class="surface-card popup-box">
        <view class="section-title">创建群聊</view>
        <input v-model="groupForm.name" class="form-control" placeholder="群名称" />
        <textarea v-model="groupForm.intro" class="form-control intro-input" placeholder="群简介" />
        <view class="popup-actions">
          <button class="ghost-button" @click="groupPopup = false">取消</button>
          <button class="primary-button" @click="submitGroup">创建</button>
        </view>
      </view>
    </view>

    <AppTabBar current="message" />
  </view>
</template>

<style scoped lang="scss">
.message-page {
  padding-top: 1rpx;
}

.head-placeholder {
  width: 60rpx;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 18rpx;
  padding: 18rpx 24rpx;
  border-radius: 999rpx;
  background: var(--app-surface-muted);
  color: var(--app-text-muted);
  font-size: 25rpx;
}

.summary-scroll {
  width: 100%;
  padding: 20rpx 0 8rpx;
}

.summary-list {
  display: inline-flex;
  gap: 14rpx;
  padding: 0 28rpx;
}

.summary-chip {
  width: 210rpx;
  padding: 20rpx;
}

.summary-chip .label {
  color: var(--app-text-muted);
  font-size: 21rpx;
}

.summary-chip .value {
  margin-top: 8rpx;
  color: var(--app-text);
  font-size: 42rpx;
  font-weight: 900;
}

.summary-chip .hint {
  margin-top: 4rpx;
  color: var(--app-text-muted);
  font-size: 21rpx;
}

.msg-tabs {
  position: sticky;
  top: 154rpx;
  z-index: 20;
  background: rgba(255, 252, 248, 0.9);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.message-content {
  padding: 22rpx 28rpx 28rpx;
}

.msg-list,
.notice-list {
  display: grid;
  gap: 16rpx;
}

.msg-item {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 24rpx;
}

.avatar {
  position: relative;
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 30rpx;
  font-weight: 800;
}

.avatar.group {
  border-radius: 22rpx;
}

.online-dot {
  position: absolute;
  right: 2rpx;
  bottom: 2rpx;
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: var(--app-success);
  border: 4rpx solid #fff;
}

.info {
  flex: 1;
  min-width: 0;
}

.top-row {
  display: flex;
  justify-content: space-between;
  gap: 14rpx;
}

.name {
  min-width: 0;
  font-size: 28rpx;
  font-weight: 700;
  color: var(--app-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time {
  flex-shrink: 0;
  color: var(--app-text-muted);
  font-size: 21rpx;
}

.last-msg {
  margin-top: 8rpx;
  color: var(--app-text-muted);
  font-size: 24rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-row {
  margin-top: 10rpx;
}

.tag,
.notice-badge {
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  background: var(--app-warning-soft);
  color: var(--app-warning);
  font-size: 21rpx;
}

.unread {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-danger);
  color: #fff;
  font-size: 20rpx;
  flex-shrink: 0;
}

.notice-card {
  display: flex;
  gap: 18rpx;
  padding: 24rpx;
}

.notice-icon {
  width: 78rpx;
  height: 78rpx;
  border-radius: 24rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
}

.notice-0 { background: var(--app-danger-soft); color: var(--app-danger); }
.notice-1 { background: #eef4ff; color: #4477da; }
.notice-2 { background: var(--app-success-soft); color: var(--app-success); }

.notice-body {
  flex: 1;
  min-width: 0;
}

.notice-top {
  display: flex;
  justify-content: space-between;
  gap: 12rpx;
}

.notice-title {
  font-size: 27rpx;
  font-weight: 700;
  color: var(--app-text);
}

.notice-time {
  color: var(--app-text-muted);
  font-size: 21rpx;
  white-space: nowrap;
}

.notice-desc {
  margin-top: 10rpx;
  color: var(--app-text-soft);
  font-size: 24rpx;
  line-height: 1.65;
}

.notice-meta {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 16rpx;
}

.notice-cta {
  margin-left: auto;
  color: var(--app-primary);
  font-size: 23rpx;
}

.popup-mask {
  position: fixed;
  inset: 0;
  z-index: 90;
  padding: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.35);
}

.popup-box {
  width: 100%;
  padding: 28rpx;
}

.popup-box .form-control {
  width: 100%;
  margin-top: 20rpx;
}

.intro-input {
  min-height: 180rpx;
}

.popup-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 22rpx;
}

.popup-actions button {
  flex: 1;
}
</style>
