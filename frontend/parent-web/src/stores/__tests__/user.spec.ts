import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'

// Mock the authApi module
vi.mock('@/api', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
  },
}))

describe('UserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('initializes with empty state when no token in localStorage', () => {
    const store = useUserStore()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.permissions).toEqual([])
    expect(store.isLoggedIn).toBe(false)
  })

  it('reads existing token from localStorage on init', () => {
    localStorage.setItem('token', 'existing-token')
    // 需要重新创建 store 以读取 localStorage
    setActivePinia(createPinia())
    const store = useUserStore()
    expect(store.token).toBe('existing-token')
    expect(store.isLoggedIn).toBe(true)
  })

  it('sets token and persists to localStorage', () => {
    const store = useUserStore()
    store.setToken('new-token-123')
    expect(store.token).toBe('new-token-123')
    expect(localStorage.setItem).toHaveBeenCalledWith('token', 'new-token-123')
  })

  it('clears token and removes from localStorage', () => {
    const store = useUserStore()
    store.setToken('token-to-clear')
    store.clearToken()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.permissions).toEqual([])
    expect(localStorage.removeItem).toHaveBeenCalledWith('token')
    expect(store.isLoggedIn).toBe(false)
  })

  it('hasPermission returns true for matching permission', () => {
    const store = useUserStore()
    store.permissions = ['user:read', 'user:write']
    expect(store.hasPermission('user:read')).toBe(true)
    expect(store.hasPermission('user:write')).toBe(true)
    expect(store.hasPermission('admin:delete')).toBe(false)
  })

  it('hasPermission returns true for wildcard permission', () => {
    const store = useUserStore()
    store.permissions = ['*']
    expect(store.hasPermission('any:permission')).toBe(true)
  })
})
