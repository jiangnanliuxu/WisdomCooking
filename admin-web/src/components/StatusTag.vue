<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  value?: string
}>()

const tone = computed(() => {
  const value = props.value || ''
  if (['enabled', 'published', 'online', 'success', 'normal', 'approved', 'active', 'indexed'].includes(value)) return 'success'
  if (['warning', 'pending_review', 'pending', 'warned', 'muted', 'running', 'indexing', 'partial_failed'].includes(value)) return 'warning'
  if (['violation', 'failed', 'disabled', 'rejected', 'blocked', 'offline', 'dissolved', 'banned'].includes(value)) return 'danger'
  return 'info'
})

const labelMap: Record<string, string> = {
  enabled: '已启用',
  disabled: '已停用',
  published: '已发布',
  online: '已上架',
  offline: '已下架',
  success: '成功',
  failed: '失败',
  unknown: '未知',
  normal: '正常',
  warning: '警告',
  violation: '违规',
  pending_review: '待审核',
  pending: '待处理',
  indexing: '索引中',
  indexed: '已索引',
  miss: '未命中',
  running: '运行中',
  partial_failed: '部分失败',
  rejected: '已驳回',
  approved: '已通过',
  blocked: '已屏蔽',
  warned: '已警告',
  dissolved: '已解散',
  muted: '已禁言',
  banned: '已封禁',
  active: '正常',
  inactive: '已停用',
  draft: '草稿',
  withdrawn: '已撤回',
}
</script>

<template>
  <span class="status-tag" :class="`status-tag--${tone}`">
    {{ labelMap[value || ''] || value || '未设置' }}
  </span>
</template>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.status-tag::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-tag--success {
  color: #52c41a;
  background: #f6ffed;
}

.status-tag--warning {
  color: #fa8c16;
  background: #fff7e6;
}

.status-tag--danger {
  color: #ff4d4f;
  background: #fff1f0;
}

.status-tag--info {
  color: #5b6b7e;
  background: #f5f7fa;
}
</style>
