/**
 * 角色管理API
 */
import request from './request'
import type { Role, RoleRequest, RolePermissionRequest } from '@/types'

export const roleApi = {
  // 角色列表
  getRoleList: (): Promise<Role[]> => {
    return request.get('/roles')
  },

  // 角色详情（含权限）
  getRoleDetail: (id: number): Promise<{ role: Role; roleMenus: any[] }> => {
    return request.get(`/roles/${id}`)
  },

  // 新增角色
  createRole: (data: RoleRequest): Promise<number> => {
    return request.post('/roles', data)
  },

  // 修改角色
  updateRole: (id: number, data: RoleRequest): Promise<void> => {
    return request.put(`/roles/${id}`, data)
  },

  // 删除角色
  deleteRole: (id: number): Promise<void> => {
    return request.delete(`/roles/${id}`)
  },

  // 分配角色权限
  saveRolePermissions: (id: number, data: RolePermissionRequest): Promise<void> => {
    return request.put(`/roles/${id}/permissions`, data)
  },

  // 查看用户角色
  getUserRoles: (userId: number): Promise<any[]> => {
    return request.get(`/users/${userId}/roles`)
  },

  // 分配用户角色
  assignUserRoles: (userId: number, roleIds: number[]): Promise<void> => {
    return request.put(`/users/${userId}/roles`, { roleIds })
  }
}
