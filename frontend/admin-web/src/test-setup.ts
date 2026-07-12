// 测试全局设置 - 为所有测试提供通用 mock
import { config } from '@vue/test-utils'
import { beforeEach, vi } from 'vitest'

// 全局 mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value }),
    removeItem: vi.fn((key: string) => { delete store[key] }),
    clear: vi.fn(() => { store = {} }),
    get length() { return Object.keys(store).length },
    key: vi.fn((index: number) => Object.keys(store)[index] ?? null),
  }
})()
Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock })

// Element Plus 全局 stub
config.global.stubs = {
  ElBreadcrumb: { template: '<div><slot /></div>' },
  ElBreadcrumbItem: { template: '<div><slot /></div>' },
  ElMenu: { template: '<div><slot /></div>' },
  ElMenuItem: { template: '<div><slot /></div>' },
  ElSubMenu: { template: '<div><slot /></div>' },
  ElIcon: { template: '<span><slot /></span>' },
  ElContainer: { template: '<div><slot /></div>' },
  ElHeader: { template: '<div><slot /></div>' },
  ElAside: { template: '<div><slot /></div>' },
  ElMain: { template: '<div><slot /></div>' },
  ElFooter: { template: '<div><slot /></div>' },
  ElButton: { template: '<button><slot /></button>' },
  ElInput: { template: '<input />' },
  ElForm: { template: '<form><slot /></form>' },
  ElFormItem: { template: '<div><slot /></div>' },
  ElTable: { template: '<table><slot /></table>' },
  ElTableColumn: { template: '<td><slot /></td>' },
  ElDialog: { template: '<div><slot /></div>' },
  ElPagination: { template: '<div />' },
  ElSelect: { template: '<select><slot /></select>' },
  ElOption: { template: '<option><slot /></option>' },
  ElDatePicker: { template: '<input />' },
}

// 每个测试前清理 localStorage
beforeEach(() => {
  localStorageMock.clear()
  vi.clearAllMocks()
})
