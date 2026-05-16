<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppNavBar from '@/components/AppNavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import RecipeCard from '@/components/RecipeCard.vue'
import { listRecipes } from '@/api/recipe'
import { clearSearchHistory, getHotKeywords, getSearchHistory, searchAll } from '@/api/operation'
import type { RecipeListItem, UserPublicProfile } from '@/types/cook'

const keyword = ref('')
const categoryCode = ref('')
const categoryFilterActive = ref(false)
const active = ref<'recipe' | 'user'>('recipe')
const loading = ref(false)
const recipes = ref<RecipeListItem[]>([])
const users = ref<UserPublicProfile[]>([])
const hotKeywords = ref<string[]>([])
const historyKeywords = ref<string[]>([])

/**
 * 搜索页优先展示热点词和个人历史，真正搜索时再走综合搜索接口。
 */
async function loadMeta() {
  try {
    const hotResponse = await getHotKeywords('8')
    hotKeywords.value = hotResponse.data || []
  }
  catch {
    hotKeywords.value = []
  }
  try {
    const historyResponse = await getSearchHistory()
    historyKeywords.value = historyResponse.data || []
  }
  catch {
    historyKeywords.value = []
  }
}

async function search(nextKeyword?: string, forceKeyword = false) {
  const currentKeyword = (nextKeyword ?? keyword.value).trim()
  keyword.value = currentKeyword
  const currentCategoryCode = categoryCode.value.trim()
  if (!currentKeyword && !currentCategoryCode) {
    recipes.value = []
    users.value = []
    return
  }
  loading.value = true
  try {
    if (currentCategoryCode && categoryFilterActive.value && !forceKeyword) {
      const response = await listRecipes({ categoryCode: currentCategoryCode, page: '1', pageSize: '20' })
      recipes.value = response.data?.items || []
      users.value = []
      return
    }

    categoryFilterActive.value = false
    const response = await searchAll({ keyword: currentKeyword, page: '1', pageSize: '20' })
    recipes.value = response.data?.recipes?.items || []
    users.value = response.data?.users || []
    hotKeywords.value = response.data?.hotKeywords || hotKeywords.value
  }
  catch {
    recipes.value = []
    users.value = []
  }
  finally {
    loading.value = false
  }
}

async function clearHistory() {
  await clearSearchHistory()
  historyKeywords.value = []
}

onMounted(async () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as { options?: Record<string, string> } | undefined
  const queryKeyword = current?.options?.keyword
  categoryCode.value = current?.options?.categoryCode || ''
  categoryFilterActive.value = Boolean(categoryCode.value)
  await loadMeta()
  if (categoryCode.value) {
    keyword.value = typeof queryKeyword === 'string' ? queryKeyword : ''
    await search()
  }
  else if (typeof queryKeyword === 'string' && queryKeyword) {
    await search(queryKeyword)
  }
})
</script>

<template>
  <view class="page-body">
    <AppNavBar title="搜索" back />
    <view style="padding: 24rpx;">
      <view class="surface-card" style="padding: 16rpx; display: flex; gap: 12rpx; align-items: center;">
        <input v-model="keyword" placeholder="输入关键词搜索菜谱或达人" style="flex: 1;" @confirm="search(undefined, true)">
        <button size="mini" @click="search(undefined, true)">搜索</button>
      </view>

      <view v-if="!recipes.length && !users.length" style="margin-top: 18rpx; display: grid; gap: 18rpx;">
        <view class="surface-card" style="padding: 20rpx;">
          <view class="meta-head">
            <text class="meta-title">热门搜索</text>
          </view>
          <view class="chip-list">
            <view v-for="item in hotKeywords" :key="item" class="search-chip" @click="search(item, true)">
              {{ item }}
            </view>
          </view>
        </view>

        <view class="surface-card" style="padding: 20rpx;">
          <view class="meta-head">
            <text class="meta-title">搜索历史</text>
            <text class="meta-link" @click="clearHistory()">清空</text>
          </view>
          <view v-if="historyKeywords.length" class="chip-list">
            <view v-for="item in historyKeywords" :key="item" class="search-chip search-chip--history" @click="search(item, true)">
              {{ item }}
            </view>
          </view>
          <EmptyState v-else title="暂无搜索历史" description="输入关键词后会自动记录最近搜索。" />
        </view>
      </view>

      <view style="display: flex; gap: 18rpx; margin: 20rpx 0;">
        <view :style="{ color: active === 'recipe' ? '#f97316' : '#64748b', fontWeight: active === 'recipe' ? '600' : '400' }" @click="active = 'recipe'">菜谱</view>
        <view :style="{ color: active === 'user' ? '#f97316' : '#64748b', fontWeight: active === 'user' ? '600' : '400' }" @click="active = 'user'">达人</view>
      </view>

      <view v-if="loading" style="text-align: center; color: #94a3b8;">搜索中...</view>

      <view v-else-if="active === 'recipe'" style="display: grid; gap: 16rpx;">
        <RecipeCard v-for="item in recipes" :key="item.id" :item="item" />
        <EmptyState v-if="keyword && !recipes.length" title="没有找到相关菜谱" description="换个关键词试试，或者去分类页看看。" />
      </view>

      <view v-else style="display: grid; gap: 16rpx;">
        <view v-for="item in users" :key="item.id" class="surface-card user-card">
          <view class="user-card__avatar">👨‍🍳</view>
          <view class="user-card__content">
            <view class="user-card__name">{{ item.nickname || `用户${item.id}` }}</view>
            <view class="user-card__meta">{{ item.recipeCount || 0 }} 菜谱 · {{ item.followerCount || 0 }} 粉丝</view>
            <view class="user-card__bio">{{ item.bio || '这个人还没有填写简介。' }}</view>
          </view>
        </view>
        <EmptyState v-if="keyword && !users.length" title="没有找到相关达人" description="换个昵称、地区或者标签再试。" />
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.meta-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.meta-title {
  font-size: 28rpx;
  font-weight: 600;
}

.meta-link {
  font-size: 22rpx;
  color: #f97316;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.search-chip {
  padding: 12rpx 18rpx;
  border-radius: 999rpx;
  background: #fff7ed;
  color: #ea580c;
  font-size: 22rpx;
}

.search-chip--history {
  background: #f8fafc;
  color: #475569;
}

.user-card {
  padding: 22rpx;
  display: flex;
  gap: 18rpx;
}

.user-card__avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: #ffedd5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
}

.user-card__content {
  flex: 1;
}

.user-card__name {
  font-size: 28rpx;
  font-weight: 600;
}

.user-card__meta,
.user-card__bio {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #64748b;
  line-height: 1.7;
}
</style>
