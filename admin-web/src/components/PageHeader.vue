<script setup lang="ts">
import {
  ChatDotRound,
  Collection,
  Cpu,
  DataAnalysis,
  Files,
  Grid,
  Picture,
  Setting,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { computed, useSlots } from 'vue'

const props = defineProps<{
  title: string
  description?: string
}>()

const slots = useSlots()

const iconMap = {
  仪表盘: DataAnalysis,
  菜谱管理: Files,
  菜谱审核: Grid,
  动态审核: ChatDotRound,
  菜谱分类: Setting,
  用户管理: User,
  群组管理: UserFilled,
  轮播图管理: Picture,
  'AI 模型配置': Cpu,
  'AI 知识库': Collection,
  'AI 对话日志': ChatDotRound,
  'AI 识图日志': Picture,
} as const

const titleIcon = computed(() => iconMap[props.title as keyof typeof iconMap] || Collection)
const hasActions = computed(() => Boolean(slots.actions))
</script>

<template>
  <div class="page-header">
    <div class="page-header__main">
      <div class="page-header__title-row">
        <span class="page-header__icon">
          <el-icon size="18">
            <component :is="titleIcon" />
          </el-icon>
        </span>
        <div class="page-header__copy">
          <h1 class="page-header__title">
            {{ title }}
          </h1>
          <p v-if="description" class="page-header__desc">
            {{ description }}
          </p>
        </div>
      </div>
    </div>

    <div v-if="hasActions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header__main {
  min-width: 0;
}

.page-header__title-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.page-header__icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--admin-primary);
  background: var(--admin-primary-soft);
  box-shadow: inset 0 0 0 1px rgba(255, 107, 53, 0.14);
  flex-shrink: 0;
}

.page-header__copy {
  display: grid;
  gap: 4px;
}

.page-header__title {
  margin: 0;
  font-size: 22px;
  line-height: 1.35;
  font-weight: 600;
  color: var(--admin-text-main);
}

.page-header__desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--admin-text-light);
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  flex-shrink: 0;
}
</style>
