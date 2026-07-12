import { describe, it, expect, vi } from 'vitest'
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
  it('initializes with default empty state', () => {
    setActivePinia(createPinia())
    const store = useUserStore()
    expect(store.token).toBe('')
    expect(store.isLoggedIn).toBe(false)
  })

  it('setToken updates token and login state', () => {
    setActivePinia(createPinia())
    const store = useUserStore()
    store.setToken('test-token')
    expect(store.token).toBe('test-token')
    expect(store.isLoggedIn).toBe(true)
  })

  it('clearToken resets all state', () => {
    setActivePinia(createPinia())
    const store = useUserStore()
    store.setToken('token-to-clear')
    store.clearToken()
    expect(store.token).toBe('')
    expect(store.isLoggedIn).toBe(false)
  })
})
