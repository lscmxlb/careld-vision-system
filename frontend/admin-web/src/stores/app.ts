/**
 * 应用状态管理
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  // State
  const sidebarCollapsed = ref(false)
  const theme = ref<'light' | 'dark'>('light')
  const loading = ref(false)
  const breadcrumbs = ref<Array<{ title: string; path?: string }>>([])

  // Actions
  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  const setSidebarCollapsed = (collapsed: boolean) => {
    sidebarCollapsed.value = collapsed
  }

  const setTheme = (newTheme: 'light' | 'dark') => {
    theme.value = newTheme
  }

  const setLoading = (isLoading: boolean) => {
    loading.value = isLoading
  }

  const setBreadcrumbs = (items: Array<{ title: string; path?: string }>) => {
    breadcrumbs.value = items
  }

  return {
    sidebarCollapsed,
    theme,
    loading,
    breadcrumbs,
    toggleSidebar,
    setSidebarCollapsed,
    setTheme,
    setLoading,
    setBreadcrumbs
  }
})
