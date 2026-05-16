<script setup lang="ts">
defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <el-empty :description="description">
    <template #image>
      <div
        style="width: 120px; height: 120px; border-radius: 24px; background: linear-gradient(135deg, #fff1eb, #ace0f9); display: flex; align-items: center; justify-content: center; font-size: 40px;"
      >
        📦
      </div>
    </template>
    <div style="font-size: 18px; font-weight: 600; color: #111827; margin-top: 8px;">
      {{ title }}
    </div>
  </el-empty>
</template>
