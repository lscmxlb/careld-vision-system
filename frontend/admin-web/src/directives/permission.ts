/**
 * v-permission 指令
 * 用法: v-permission="'user:create'" 或 v-permission="['user:create', 'user:update']"
 * 无权限时移除 DOM 元素
 */
import type { Directive, DirectiveBinding } from 'vue'
import { usePermissionStore } from '@/stores/permission'

const permissionDirective: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const permStore = usePermissionStore()
    const value = binding.value

    if (!value) return

    const keys = Array.isArray(value) ? value : [value]
    const hasPermission = keys.some((key: string) => permStore.hasPermission(key))

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  }
}

export default permissionDirective
