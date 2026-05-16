<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import EmptyPlaceholder from '@/components/EmptyPlaceholder.vue'

defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <PageHeader :title="title" :description="description" />
  <el-card class="app-card">
    <EmptyPlaceholder :title="title" :description="description" />
  </el-card>
</template>
