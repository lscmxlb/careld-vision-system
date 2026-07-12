import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import Breadcrumb from '../Breadcrumb.vue'

// Mock vue-router
const mockRoute = {
  matched: [
    { path: '/store', meta: { title: '门店管理' } },
    { path: '/store/list', meta: { title: '门店列表' } },
  ],
}

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({ push: vi.fn() }),
}))

describe('Breadcrumb', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders home link', () => {
    const wrapper = mount(Breadcrumb, {
      global: {
        stubs: {
          ElBreadcrumb: { template: '<div class="el-breadcrumb"><slot /></div>' },
          ElBreadcrumbItem: {
            template: '<span class="breadcrumb-item"><slot /></span>',
          },
        },
      },
    })
    expect(wrapper.text()).toContain('首页')
  })

  it('renders breadcrumb items from route matched', () => {
    const wrapper = mount(Breadcrumb, {
      global: {
        stubs: {
          ElBreadcrumb: { template: '<div class="el-breadcrumb"><slot /></div>' },
          ElBreadcrumbItem: {
            template: '<span class="breadcrumb-item"><slot /></span>',
          },
        },
      },
    })
    const items = wrapper.findAll('.breadcrumb-item')
    // home + 2 matched routes
    expect(items.length).toBe(3)
    expect(wrapper.text()).toContain('门店管理')
    expect(wrapper.text()).toContain('门店列表')
  })

  it('does not render undefined titles', () => {
    const wrapper = mount(Breadcrumb, {
      global: {
        stubs: {
          ElBreadcrumb: { template: '<div><slot /></div>' },
          ElBreadcrumbItem: {
            template: '<span class="item"><slot /></span>',
          },
        },
      },
    })
    expect(wrapper.text()).not.toContain('undefined')
  })
})
