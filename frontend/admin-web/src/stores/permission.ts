/**
 * 权限状态管理
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { menuApi } from '@/api'
import type { MenuItem } from '@/types'

export const usePermissionStore = defineStore('permission', () => {
  const menus = ref<MenuItem[]>([])
  const permissions = ref<string[]>([])
  const loaded = ref(false)

  const loadPermissions = async () => {
    try {
      const res = await menuApi.getMyMenus()
      menus.value = res.menus || []
      permissions.value = res.permissions || []
      loaded.value = true
    } catch (e) {
      console.error('加载权限失败', e)
      menus.value = []
      permissions.value = []
      loaded.value = false
    }
  }

  const hasPermission = (key: string): boolean => {
    if (permissions.value.includes('*')) return true
    return permissions.value.includes(key)
  }

  const hasAction = (module: string, resource: string, action: string): boolean => {
    return hasPermission(`${module}:${resource}:${action}`)
  }

  const clear = () => {
    menus.value = []
    permissions.value = []
    loaded.value = false
  }

  return {
    menus,
    permissions,
    loaded,
    loadPermissions,
    hasPermission,
    hasAction,
    clear
  }
})
