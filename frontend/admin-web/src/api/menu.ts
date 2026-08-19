/**
 * 菜单管理API
 */
import request from './request'
import type { MenuItem, MenuRequest } from '@/types'

export const menuApi = {
  // 当前用户菜单和权限
  getMyMenus: (): Promise<{ menus: MenuItem[]; permissions: string[] }> => {
    return request.get('/menus/my')
  },

  // 菜单树（管理用）
  getMenuTree: (): Promise<MenuItem[]> => {
    return request.get('/menus')
  },

  // 新增菜单
  createMenu: (data: MenuRequest): Promise<number> => {
    return request.post('/menus', data)
  },

  // 修改菜单
  updateMenu: (id: number, data: MenuRequest): Promise<void> => {
    return request.put(`/menus/${id}`, data)
  },

  // 删除菜单
  deleteMenu: (id: number): Promise<void> => {
    return request.delete(`/menus/${id}`)
  }
}
