import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from '@/stores/app'

describe('AppStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('initializes with default state', () => {
    const store = useAppStore()
    expect(store.sidebarCollapsed).toBe(false)
    expect(store.theme).toBe('light')
    expect(store.loading).toBe(false)
    expect(store.breadcrumbs).toEqual([])
  })

  it('toggles sidebar collapsed state', () => {
    const store = useAppStore()
    expect(store.sidebarCollapsed).toBe(false)

    store.toggleSidebar()
    expect(store.sidebarCollapsed).toBe(true)

    store.toggleSidebar()
    expect(store.sidebarCollapsed).toBe(false)
  })

  it('sets sidebar collapsed explicitly', () => {
    const store = useAppStore()
    store.setSidebarCollapsed(true)
    expect(store.sidebarCollapsed).toBe(true)

    store.setSidebarCollapsed(false)
    expect(store.sidebarCollapsed).toBe(false)
  })

  it('switches theme', () => {
    const store = useAppStore()
    expect(store.theme).toBe('light')

    store.setTheme('dark')
    expect(store.theme).toBe('dark')
  })

  it('sets loading state', () => {
    const store = useAppStore()
    store.setLoading(true)
    expect(store.loading).toBe(true)

    store.setLoading(false)
    expect(store.loading).toBe(false)
  })

  it('sets breadcrumbs', () => {
    const store = useAppStore()
    const crumbs = [
      { title: '首页', path: '/' },
      { title: '门店管理', path: '/store' },
    ]
    store.setBreadcrumbs(crumbs)
    expect(store.breadcrumbs).toEqual(crumbs)
    expect(store.breadcrumbs.length).toBe(2)
  })
})
