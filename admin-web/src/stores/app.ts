import { ref } from 'vue'
import { defineStore } from 'pinia'

export interface VisitedTab {
  title: string
  path: string
}

/**
 * 后台壳层状态，维护侧边栏折叠和访问过的标签页。
 */
export const useAppStore = defineStore('admin-app', () => {
  const collapsed = ref(false)
  const visitedTabs = ref<VisitedTab[]>([])

  function toggleCollapsed() {
    collapsed.value = !collapsed.value
  }

  function addVisitedTab(tab: VisitedTab) {
    if (!visitedTabs.value.some(item => item.path === tab.path)) {
      visitedTabs.value.push(tab)
    }
  }

  function removeVisitedTab(path: string) {
    visitedTabs.value = visitedTabs.value.filter(item => item.path !== path)
  }

  return {
    collapsed,
    visitedTabs,
    toggleCollapsed,
    addVisitedTab,
    removeVisitedTab,
  }
})
