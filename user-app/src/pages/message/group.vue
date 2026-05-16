<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '@/components/EmptyState.vue'
import { getGroup, inviteGroupMembers, leaveGroup, listGroupMembers } from '@/api/group'
import { listMessages, markRead, sendMessage, updateConversationSettings } from '@/api/message'
import { listMyFollowers } from '@/api/user'
import { connectConversation } from '@/services/websocket'
import type { GroupMember, GroupVo, MessageItem, UserPublicProfile } from '@/types/cook'
import { getStoredUser } from '@/utils/auth'
import { requireLogin } from '@/utils/format'
import { resolveAssetUrl } from '@/utils/media'

const groupId = ref(0)
const group = ref<GroupVo>()
const members = ref<GroupMember[]>([])
const messages = ref<MessageItem[]>([])
const text = ref('')
const showPanel = ref(false)
const invite = reactive({ userIdsText: '' })
const fanPicker = reactive({
  visible: false,
  loading: false,
  rows: [] as UserPublicProfile[],
  selectedIds: [] as number[],
})

const selfProfile = getStoredUser<{ id?: number; nickname?: string }>()
const selfId = Number(selfProfile?.id || 0)
const displayGroup = computed(() => group.value)
const displayMembers = computed(() => members.value)
const displayMessages = computed(() => messages.value)

async function loadData() {
  if (!requireLogin() || !groupId.value) return
  const groupRes = await getGroup(groupId.value)
  group.value = groupRes.data
  const [memberRes, messageRes] = await Promise.all([
    listGroupMembers(groupId.value),
    listMessages(groupRes.data?.conversationId || 0),
  ])
  members.value = memberRes.data || []
  messages.value = messageRes.data?.items || []
  if (groupRes.data?.conversationId) {
    await markRead(groupRes.data.conversationId)
  }
}

async function submit() {
  if (!group.value?.conversationId || !text.value.trim()) return
  await sendMessage(group.value.conversationId, { messageType: 'text', content: text.value })
  text.value = ''
  await loadData()
}

async function doInvite() {
  const ids = invite.userIdsText.split(',').map(item => Number(item.trim())).filter(Boolean)
  if (!ids.length) {
    uni.showToast({ title: '请输入用户 ID', icon: 'none' })
    return
  }
  await inviteGroupMembers(groupId.value, { userIds: ids })
  invite.userIdsText = ''
  await loadData()
}

async function openFanPicker() {
  fanPicker.visible = true
  fanPicker.selectedIds = []
  fanPicker.loading = true
  try {
    const response = await listMyFollowers({ page: '1', pageSize: '100' })
    const memberIds = new Set(members.value.map(item => item.userId))
    fanPicker.rows = (response.data?.items || []).filter(item => !memberIds.has(item.id))
  }
  catch {
    fanPicker.rows = []
  }
  finally {
    fanPicker.loading = false
  }
}

function closeFanPicker() {
  fanPicker.visible = false
}

function toggleFan(item: UserPublicProfile) {
  if (fanPicker.selectedIds.includes(item.id)) {
    fanPicker.selectedIds = fanPicker.selectedIds.filter(id => id !== item.id)
    return
  }
  fanPicker.selectedIds = [...fanPicker.selectedIds, item.id]
}

async function inviteSelectedFans() {
  if (!fanPicker.selectedIds.length) {
    uni.showToast({ title: '请选择粉丝', icon: 'none' })
    return
  }
  await inviteGroupMembers(groupId.value, { userIds: fanPicker.selectedIds })
  closeFanPicker()
  await loadData()
}

function isSelf(item: MessageItem) {
  return Boolean(selfId && item.senderId === selfId)
}

function goBack() {
  uni.navigateBack()
}

function togglePanel() {
  showPanel.value = !showPanel.value
}

async function updateGroupSetting(data: { muted?: boolean; pinned?: boolean }) {
  if (!group.value?.conversationId) return
  await updateConversationSettings(group.value.conversationId, data)
  uni.showToast({ title: '群聊设置已更新', icon: 'none' })
}

function confirmLeaveGroup() {
  uni.showModal({
    title: '退出群聊',
    content: '确认退出当前群聊吗？',
    success: async ({ confirm }) => {
      if (!confirm) return
      await leaveGroup(groupId.value)
      uni.showToast({ title: '已退出群聊', icon: 'none' })
      uni.navigateBack()
    },
  })
}

onLoad((options) => {
  groupId.value = Number(options?.groupId || 0)
})

onMounted(async () => {
  await loadData()
  if (group.value?.conversationId) {
    connectConversation(group.value.conversationId, () => loadData())
  }
})
</script>

<template>
  <view class="page-body group-chat-page">
    <view class="chat-nav">
      <view class="chat-nav__back" @click="goBack">←</view>
      <view class="chat-nav__title">🏠 {{ displayGroup?.name || '群聊详情' }}</view>
      <view class="chat-nav__more" @click="togglePanel">⋯</view>
    </view>

    <template v-if="displayGroup">
      <view class="surface-card summary-card">
      <view class="summary-main">
        <view class="summary-avatar">👥</view>
        <view class="summary-info">
          <view class="summary-title">{{ displayGroup.name }}</view>
          <view class="summary-desc">{{ displayGroup.intro || '暂无群简介' }}</view>
        </view>
      </view>
      <view class="summary-stats">
        <view class="summary-stat">
          <view class="value">{{ displayGroup.memberCount || displayMembers.length }}</view>
          <view class="label">群成员</view>
        </view>
        <view class="summary-stat">
          <view class="value">{{ displayGroup.messageCount || displayMessages.length }}</view>
          <view class="label">消息数</view>
        </view>
        <view class="summary-stat">
          <view class="value">{{ displayMessages.length }}</view>
          <view class="label">聊天记录</view>
        </view>
      </view>
      </view>

      <view class="notice-card">
        <text class="label">📢 群公告</text>
        <text>{{ displayGroup.notice || '暂无群公告' }}</text>
      </view>

      <view class="chat-area">
        <EmptyState v-if="!displayMessages.length" title="还没有群消息" description="邀请成员或发送第一条消息后，这里会实时更新。" />

        <view
          v-for="item in displayMessages"
          :key="item.id"
          class="chat-row"
          :class="{ self: isSelf(item) }"
        >
          <view class="chat-row__avatar" :class="{ self: isSelf(item) }">{{ item.senderNickname?.slice(0, 1) || '群' }}</view>
          <view>
            <view v-if="!isSelf(item)" class="chat-row__name">{{ item.senderNickname || '成员' }}</view>
            <view class="chat-bubble">{{ item.content }}</view>
          </view>
        </view>
      </view>

      <view class="input-bar">
        <view class="tool-btn">＋</view>
        <view class="tool-btn">🎤</view>
        <input v-model="text" class="input-bar__field" placeholder="输入消息..." />
        <view class="tool-btn">😄</view>
        <view class="tool-btn">🖼️</view>
        <button class="send-btn" @click="submit">发送</button>
      </view>

      <view v-if="showPanel" class="overlay" @click="togglePanel" />
      <view class="side-panel" :class="{ show: showPanel }">
        <view class="panel-section">
          <view class="panel-title">群成员 ({{ displayGroup.memberCount || displayMembers.length }})</view>
          <view class="member-grid">
            <view v-for="member in displayMembers" :key="member.userId" class="member-card">
              <view class="member-card__avatar">{{ member.nickname?.slice(0, 1) || '群' }}</view>
              <view class="member-card__name">{{ member.nickname }}</view>
            </view>
          </view>
        </view>

        <view class="panel-section">
          <view class="panel-title">邀请成员</view>
          <input v-model="invite.userIdsText" class="panel-input" placeholder="输入用户 ID，逗号分隔" />
          <button class="panel-button" @click="doInvite">邀请</button>
          <button class="panel-button secondary" @click="openFanPicker">选择粉丝邀请</button>
        </view>

        <view class="panel-section">
          <view class="panel-title">群资料</view>
          <view class="panel-meta">{{ displayGroup.intro || '暂无群简介' }}</view>
          <view class="panel-meta">{{ displayGroup.notice || '暂无群公告' }}</view>
        </view>

        <view class="panel-section">
          <view class="panel-toggle" @click="updateGroupSetting({ muted: true })">🔇 开启免打扰</view>
          <view class="panel-toggle" @click="updateGroupSetting({ muted: false })">🔔 取消免打扰</view>
          <view class="panel-toggle" @click="updateGroupSetting({ pinned: true })">📌 置顶聊天</view>
          <view class="panel-toggle" @click="updateGroupSetting({ pinned: false })">📍 取消置顶</view>
          <view class="panel-toggle danger" @click="confirmLeaveGroup">🚪 退出群聊</view>
        </view>
      </view>
    </template>
    <EmptyState v-else title="群聊不存在" description="可能已被解散，或你已不在该群中。" />

    <view v-if="fanPicker.visible" class="fan-picker">
      <view class="fan-picker__mask" @click="closeFanPicker" />
      <view class="fan-picker__panel">
        <view class="fan-picker__header">
          <view>
            <view class="fan-picker__title">选择粉丝</view>
            <view class="fan-picker__subtitle">选中的粉丝会直接加入群聊，并在通知列表收到邀请消息</view>
          </view>
          <view class="fan-picker__close" @click="closeFanPicker">✕</view>
        </view>
        <scroll-view scroll-y class="fan-picker__list">
          <view v-if="fanPicker.loading" class="fan-picker__empty">加载中...</view>
          <view v-else-if="!fanPicker.rows.length" class="fan-picker__empty">暂无可邀请粉丝</view>
          <template v-else>
            <view
              v-for="item in fanPicker.rows"
              :key="item.id"
              class="fan-picker__item"
              :class="{ selected: fanPicker.selectedIds.includes(item.id) }"
              @click="toggleFan(item)"
            >
              <view class="fan-picker__avatar">
                <image v-if="resolveAssetUrl(item.avatarUrl || '')" :src="resolveAssetUrl(item.avatarUrl || '')" mode="aspectFill" />
                <text v-else>{{ item.nickname?.slice(0, 1) || '粉' }}</text>
              </view>
              <view class="fan-picker__body">
                <view class="fan-picker__name">{{ item.nickname || `用户${item.id}` }}</view>
                <view class="fan-picker__meta">{{ item.recipeCount || 0 }} 菜谱 · {{ item.postCount || 0 }} 动态</view>
              </view>
              <view class="fan-picker__check">{{ fanPicker.selectedIds.includes(item.id) ? '✓' : '' }}</view>
            </view>
          </template>
        </scroll-view>
        <view class="fan-picker__actions">
          <button class="fan-picker__cancel" @click="closeFanPicker">取消</button>
          <button class="fan-picker__confirm" @click="inviteSelectedFans">邀请 {{ fanPicker.selectedIds.length || '' }}</button>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.group-chat-page {
  min-height: 100vh;
  padding-bottom: calc(128rpx + env(safe-area-inset-bottom));
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
  font-size: 28rpx;
  font-weight: 700;
  color: var(--app-text);
}

.summary-card {
  margin: 18rpx 28rpx 0;
  padding: 24rpx;
}

.summary-main {
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.summary-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fccb90, #d57eeb);
  color: #fff;
  font-size: 40rpx;
}

.summary-info {
  flex: 1;
}

.summary-title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--app-text);
}

.summary-desc {
  margin-top: 8rpx;
  color: var(--app-text-muted);
  font-size: 22rpx;
  line-height: 1.65;
}

.summary-stats {
  display: flex;
  gap: 10rpx;
  margin-top: 18rpx;
}

.summary-stat {
  flex: 1;
  padding: 16rpx;
  border-radius: 18rpx;
  background: #f7f7f7;
}

.summary-stat .value {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--app-text);
}

.summary-stat .label {
  margin-top: 4rpx;
  color: var(--app-text-muted);
  font-size: 20rpx;
}

.quick-actions {
  display: flex;
  gap: 10rpx;
  overflow-x: auto;
  padding: 18rpx 28rpx 0;
  white-space: nowrap;
}

.quick-actions::-webkit-scrollbar {
  display: none;
}

.quick-action {
  flex: 0 0 auto;
  padding: 14rpx 18rpx;
  border-radius: 18rpx;
  background: #fff;
  color: var(--app-text-soft);
  font-size: 22rpx;
}

.notice-card {
  display: flex;
  gap: 8rpx;
  margin: 18rpx 28rpx 0;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #fff9e6;
  color: #8c6d1f;
  font-size: 22rpx;
  line-height: 1.7;
}

.notice-card .label {
  color: #fa8c16;
  font-weight: 700;
  white-space: nowrap;
}

.chat-area {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  padding: 20rpx 28rpx 0;
}

.time-divider {
  align-self: center;
  color: var(--app-text-muted);
  font-size: 20rpx;
}

.system-chip {
  align-self: center;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-text-muted);
  font-size: 20rpx;
}

.task-card,
.file-card {
  padding: 20rpx;
}

.task-card__head,
.file-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--app-text);
  font-size: 24rpx;
  font-weight: 700;
}

.task-card__desc,
.file-card__desc {
  margin-top: 10rpx;
  color: var(--app-text-soft);
  font-size: 22rpx;
  line-height: 1.7;
}

.member-strip {
  display: flex;
  gap: 8rpx;
  overflow-x: auto;
  margin-top: 14rpx;
  white-space: nowrap;
}

.member-strip::-webkit-scrollbar {
  display: none;
}

.member-pill {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 12rpx;
  border-radius: 16rpx;
  background: #f7f7f7;
  color: var(--app-text-soft);
  font-size: 20rpx;
}

.member-pill .icon {
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #84fab0, #8fd3f4);
  color: #fff;
  font-size: 16rpx;
}

.chat-row {
  display: flex;
  gap: 12rpx;
  max-width: 88%;
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
  background: linear-gradient(135deg, #84fab0, #8fd3f4);
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
}

.chat-row__avatar.self {
  background: linear-gradient(135deg, #a1c4fd, #c2e9fb);
}

.chat-row__name {
  margin-bottom: 4rpx;
  color: var(--app-text-muted);
  font-size: 18rpx;
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

.file-card__link {
  margin-top: 10rpx;
  color: var(--app-primary);
  font-size: 22rpx;
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
  z-index: 40;
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

.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 50;
}

.side-panel {
  position: fixed;
  top: 0;
  right: -520rpx;
  z-index: 60;
  width: 520rpx;
  height: 100vh;
  padding: 88rpx 24rpx 32rpx;
  background: #fff;
  box-shadow: -8rpx 0 20rpx rgba(0, 0, 0, 0.08);
  transition: right 0.24s ease;
  overflow-y: auto;
}

.side-panel.show {
  right: 0;
}

.panel-section {
  margin-bottom: 24rpx;
}

.panel-title {
  margin-bottom: 14rpx;
  color: var(--app-text);
  font-size: 26rpx;
  font-weight: 700;
}

.member-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14rpx;
}

.member-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
}

.member-card__avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f7;
  color: var(--app-text);
  font-size: 24rpx;
}

.member-card__name {
  color: var(--app-text-soft);
  font-size: 18rpx;
  text-align: center;
}

.panel-input {
  height: 72rpx;
  padding: 0 18rpx;
  border-radius: 18rpx;
  background: var(--app-surface-muted);
  font-size: 24rpx;
}

.panel-button {
  width: 100%;
  height: 68rpx;
  margin-top: 12rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  font-size: 24rpx;
  line-height: 68rpx;
}

.panel-button.secondary {
  background: var(--app-primary-soft);
  color: var(--app-primary-strong);
}

.panel-meta {
  padding: 16rpx;
  border-radius: 18rpx;
  background: #f7f7f7;
  color: var(--app-text-soft);
  font-size: 22rpx;
  line-height: 1.7;
}

.panel-file {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
  padding: 16rpx;
  border-radius: 18rpx;
  background: #fff7e6;
  color: #8c6d1f;
  font-size: 22rpx;
}

.panel-toggle {
  padding: 18rpx 0;
  border-top: 1rpx solid rgba(15, 23, 42, 0.05);
  color: var(--app-text);
  font-size: 24rpx;
}

.panel-toggle.danger {
  color: var(--app-danger);
}

.fan-picker {
  position: fixed;
  inset: 0;
  z-index: 120;
}

.fan-picker__mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
}

.fan-picker__panel {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  max-height: 78vh;
  padding: 28rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
  border-radius: 32rpx 32rpx 0 0;
  background: #fff;
}

.fan-picker__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.fan-picker__title {
  color: var(--app-text);
  font-size: 31rpx;
  font-weight: 800;
}

.fan-picker__subtitle {
  margin-top: 6rpx;
  color: var(--app-text-muted);
  font-size: 23rpx;
  line-height: 1.5;
}

.fan-picker__close {
  flex-shrink: 0;
  color: var(--app-text-muted);
  font-size: 30rpx;
}

.fan-picker__list {
  max-height: 52vh;
  margin-top: 20rpx;
}

.fan-picker__empty {
  padding: 50rpx 0;
  color: var(--app-text-muted);
  font-size: 24rpx;
  text-align: center;
}

.fan-picker__item {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.fan-picker__item.selected .fan-picker__name {
  color: var(--app-primary);
}

.fan-picker__avatar {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--app-primary-soft);
  color: var(--app-primary);
  font-size: 27rpx;
  font-weight: 700;
}

.fan-picker__avatar image {
  width: 100%;
  height: 100%;
}

.fan-picker__body {
  min-width: 0;
  flex: 1;
}

.fan-picker__name {
  overflow: hidden;
  color: var(--app-text);
  font-size: 27rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fan-picker__meta {
  margin-top: 6rpx;
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.fan-picker__check {
  width: 38rpx;
  color: var(--app-primary);
  font-size: 30rpx;
  text-align: center;
}

.fan-picker__actions {
  display: flex;
  gap: 16rpx;
  margin-top: 22rpx;
}

.fan-picker__cancel,
.fan-picker__confirm {
  flex: 1;
  height: 74rpx;
  margin: 0;
  border-radius: 999rpx;
  font-size: 26rpx;
  line-height: 74rpx;
}

.fan-picker__cancel {
  background: #f8fafc;
  color: var(--app-text-soft);
}

.fan-picker__confirm {
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
}
</style>
