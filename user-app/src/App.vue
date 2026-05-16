<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('码上智厨用户端启动')
})

onShow(() => {
  console.log('码上智厨用户端显示')
})
</script>

<style lang="scss">
html,
body {
  width: 100%;
  min-height: 100%;
  background: #f3f4f7;
}

body {
  margin: 0;
}

page {
  --app-bg-top: #fff7ef;
  --app-bg-main: #f3f4f7;
  --app-surface: rgba(255, 255, 255, 0.94);
  --app-surface-strong: #ffffff;
  --app-surface-muted: #f7f4ef;
  --app-border: rgba(15, 23, 42, 0.08);
  --app-border-strong: rgba(15, 23, 42, 0.12);
  --app-text: #18212b;
  --app-text-soft: #5e6977;
  --app-text-muted: #8d97a6;
  --app-primary: #e86d2f;
  --app-primary-strong: #c8561b;
  --app-primary-soft: #fff0e8;
  --app-success: #2d8768;
  --app-success-soft: #e8f5ef;
  --app-warning: #b77822;
  --app-warning-soft: #fff6de;
  --app-danger: #d35349;
  --app-danger-soft: #fff0ee;
  --app-shadow-lg: 0 18rpx 44rpx rgba(22, 28, 45, 0.09);
  --app-shadow-md: 0 10rpx 26rpx rgba(22, 28, 45, 0.07);
  --app-shadow-sm: 0 4rpx 14rpx rgba(22, 28, 45, 0.05);

  background: linear-gradient(180deg, var(--app-bg-top) 0, #fbfaf7 420rpx, var(--app-bg-main) 100%);
  color: var(--app-text);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.page-body {
  min-height: 100vh;
  max-width: 860rpx;
  margin: 0 auto;
  padding-bottom: calc(132rpx + env(safe-area-inset-bottom));
  overflow-x: hidden;
}

.surface-card {
  background: var(--app-surface);
  border: 1rpx solid var(--app-border);
  border-radius: 28rpx;
  box-shadow: var(--app-shadow-md);
  overflow: hidden;
}

.section-title {
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1.25;
  color: var(--app-text);
  letter-spacing: 0;
}

.section-caption {
  font-size: 24rpx;
  color: var(--app-text-muted);
  line-height: 1.6;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin: 34rpx 0 18rpx;
}

.section-link {
  flex-shrink: 0;
  font-size: 24rpx;
  color: var(--app-primary);
}

.app-page-head {
  position: sticky;
  top: 0;
  z-index: 30;
  padding: 22rpx 28rpx 18rpx;
  background: rgba(255, 252, 248, 0.9);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.app-page-head__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.icon-action {
  min-width: 60rpx;
  height: 60rpx;
  padding: 0 18rpx;
  border: 1rpx solid var(--app-border);
  border-radius: 999rpx;
  background: var(--app-surface-strong);
  color: var(--app-text-soft);
  font-size: 26rpx;
  line-height: 60rpx;
  text-align: center;
  box-shadow: var(--app-shadow-sm);
}

.segmented-tabs,
.topic-tabs {
  display: flex;
  gap: 10rpx;
  overflow-x: auto;
  padding: 12rpx 28rpx;
  white-space: nowrap;
}

.segmented-tabs::-webkit-scrollbar,
.topic-tabs::-webkit-scrollbar {
  display: none;
}

.segmented-tab,
.topic-tab {
  flex: 0 0 auto;
  padding: 14rpx 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.58);
  color: var(--app-text-muted);
  font-size: 25rpx;
  border: 1rpx solid transparent;
}

.segmented-tab.active,
.topic-tab.active {
  background: var(--app-primary-soft);
  color: var(--app-primary-strong);
  border-color: rgba(232, 109, 47, 0.2);
  font-weight: 700;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.food-chip {
  padding: 12rpx 20rpx;
  border-radius: 999rpx;
  background: var(--app-primary-soft);
  color: var(--app-primary-strong);
  font-size: 23rpx;
}

.muted-chip {
  background: var(--app-surface-muted);
  color: var(--app-text-soft);
}

.primary-button,
.ghost-button {
  height: 72rpx;
  margin: 0;
  padding: 0 26rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  line-height: 72rpx;
}

.primary-button {
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  border: none;
  box-shadow: 0 12rpx 24rpx rgba(232, 109, 47, 0.2);
}

.ghost-button {
  background: var(--app-surface-strong);
  color: var(--app-primary-strong);
  border: 1rpx solid rgba(232, 109, 47, 0.3);
}

.form-control {
  min-height: 80rpx;
  box-sizing: border-box;
  padding: 18rpx 22rpx;
  border-radius: 22rpx;
  background: var(--app-surface-muted);
  color: var(--app-text);
  font-size: 26rpx;
}

input,
textarea {
  box-sizing: border-box;
  color: var(--app-text);
  font-family: inherit;
}

button {
  font-family: inherit;
}

button::after {
  border: 0;
}
</style>
