/**
 * 应用状态管理
 */
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export interface SystemSettings {
  systemName: string
  logoUrl: string
  defaultPageSize: number
  tokenExpireHours: number
  enableCaptcha: boolean
}

const DEFAULT_SETTINGS: SystemSettings = {
  systemName: 'Careld可尔欧得视力养护系统',
  logoUrl: '',
  defaultPageSize: 20,
  tokenExpireHours: 2,
  enableCaptcha: true
}

function loadSettings(): SystemSettings {
  try {
    const saved = localStorage.getItem('systemSettings')
    if (saved) {
      return { ...DEFAULT_SETTINGS, ...JSON.parse(saved) }
    }
  } catch { /* ignore */ }
  return { ...DEFAULT_SETTINGS }
}

export const useAppStore = defineStore('app', () => {
  // State
  const sidebarCollapsed = ref(false)
  const theme = ref<'light' | 'dark'>('light')
  const loading = ref(false)
  const breadcrumbs = ref<Array<{ title: string; path?: string }>>([])
  const systemSettings = ref<SystemSettings>(loadSettings())

  // 持久化到 localStorage
  watch(systemSettings, (val) => {
    localStorage.setItem('systemSettings', JSON.stringify(val))
  }, { deep: true })

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

  const updateSystemSettings = (partial: Partial<SystemSettings>) => {
    systemSettings.value = { ...systemSettings.value, ...partial }
  }

  return {
    sidebarCollapsed,
    theme,
    loading,
    breadcrumbs,
    systemSettings,
    toggleSidebar,
    setSidebarCollapsed,
    setTheme,
    setLoading,
    setBreadcrumbs,
    updateSystemSettings
  }
})
