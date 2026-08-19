<template>
  <div class="role-management">
    <el-row :gutter="16">
      <!-- 左侧角色列表 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>角色列表</span>
              <el-button type="primary" size="small" @click="showCreateDialog" v-permission="'settings:role:create'">
                <el-icon><Plus /></el-icon> 新增角色
              </el-button>
            </div>
          </template>
          <el-table
            :data="roles"
            highlight-current-row
            @current-change="handleRoleSelect"
            border
            size="small"
          >
            <el-table-column prop="roleName" label="角色名称" />
            <el-table-column prop="roleCode" label="角色编码" width="120" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" link @click.stop="showEditDialog(row)" v-permission="'settings:role:update'">编辑</el-button>
                <el-button size="small" link type="danger" @click.stop="handleDelete(row)" v-permission="'settings:role:delete'">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧权限分配 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>权限分配{{ selectedRole ? ' - ' + selectedRole.roleName : '' }}</span>
              <el-button
                type="primary"
                size="small"
                :disabled="!selectedRole"
                @click="savePermissions"
                v-permission="'settings:role:update'"
              >
                保存权限
              </el-button>
            </div>
          </template>
          <div v-if="!selectedRole" class="empty-tip">
            <el-empty description="请选择左侧角色" />
          </div>
          <template v-else>
            <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
              <template #title>操作说明</template>
              勾选【目录】和【菜单】节点控制角色可见的侧边栏菜单；勾选【按钮】节点控制具体的操作权限（如新增、删除、重置密码等）。保存后，该角色下的用户重新登录即生效。
            </el-alert>
            <el-tree
            ref="treeRef"
            :data="menuTree"
            :props="{ label: 'menuName', children: 'children' }"
            node-key="id"
            show-checkbox
            default-expand-all
            :default-checked-keys="checkedKeys"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <span>{{ data.menuName }}</span>
                <span class="node-type">{{ getMenuTypeText(data.menuType) }}</span>
                <span class="node-perm" v-if="data.permissionKey">{{ data.permissionKey }}</span>
              </span>
            </template>
          </el-tree>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增/编辑角色对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingRole ? '编辑角色' : '新增角色'" width="500px">
      <el-form :model="roleForm" label-width="100px">
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" required>
          <el-input v-model="roleForm.roleCode" placeholder="如: center_admin" :disabled="!!editingRole" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="roleForm.roleDesc" type="textarea" />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="roleForm.userType" placeholder="选择用户类型">
            <el-option label="总部" :value="1" />
            <el-option label="医院维护" :value="2" />
            <el-option label="家长" :value="3" />
            <el-option label="运营中心" :value="4" />
            <el-option label="代理商" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="roleForm.dataScope" placeholder="选择数据范围">
            <el-option label="全部数据" :value="1" />
            <el-option label="本中心及下级" :value="2" />
            <el-option label="本代理商及下级" :value="3" />
            <el-option label="本医院" :value="4" />
            <el-option label="个人" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="roleForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="roleForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { roleApi, menuApi } from '@/api'
import type { Role, RoleRequest, MenuItem } from '@/types'

const roles = ref<Role[]>([])
const selectedRole = ref<Role | null>(null)
const menuTree = ref<MenuItem[]>([])
const checkedKeys = ref<number[]>([])
const treeRef = ref()
const dialogVisible = ref(false)
const editingRole = ref<Role | null>(null)
const roleForm = reactive<RoleRequest>({
  roleCode: '',
  roleName: '',
  roleDesc: '',
  userType: 1,
  dataScope: 1,
  sortOrder: 1,
  status: 1
})

const getMenuTypeText = (type: number) => {
  const map: Record<number, string> = { 1: '目录', 2: '菜单', 3: '按钮' }
  return map[type] || ''
}

const loadRoles = async () => {
  try {
    roles.value = await roleApi.getRoleList()
  } catch (e) {
    ElMessage.error('加载角色列表失败')
  }
}

const loadMenuTree = async () => {
  try {
    menuTree.value = await menuApi.getMenuTree()
  } catch (e) {
    ElMessage.error('加载菜单树失败')
  }
}

const handleRoleSelect = async (row: Role | null) => {
  if (!row) return
  selectedRole.value = row
  try {
    const detail = await roleApi.getRoleDetail(row.id)
    const savedIds = detail.roleMenus?.map((rm: any) => rm.menuId) || []
    const savedSet = new Set(savedIds)

    // 只勾选叶子节点（按钮或无子节点的菜单），避免父级目录/菜单被设为全选
    // el-tree 会根据子节点勾选状态自动半选父级
    const leafCheckedKeys: number[] = []
    const walkForLeaves = (items: MenuItem[]) => {
      for (const item of items) {
        if (!item.children || item.children.length === 0) {
          // 叶子节点：只有保存过才勾选
          if (savedSet.has(item.id)) leafCheckedKeys.push(item.id)
        } else {
          // 非叶子节点：递归处理子节点
          walkForLeaves(item.children)
        }
      }
    }
    walkForLeaves(menuTree.value)

    checkedKeys.value = leafCheckedKeys
    // 等待 tree 渲染后设置选中
    setTimeout(() => {
      treeRef.value?.setCheckedKeys(leafCheckedKeys)
    }, 100)
  } catch (e) {
    ElMessage.error('加载角色权限失败')
  }
}

// 构建 menuId → menuType 查找表
const buildMenuTypeMap = (nodes: MenuItem[]): Map<number, number> => {
  const map = new Map<number, number>()
  const walk = (items: MenuItem[]) => {
    for (const item of items) {
      map.set(item.id, item.menuType)
      if (item.children) walk(item.children)
    }
  }
  walk(nodes)
  return map
}

const savePermissions = async () => {
  if (!selectedRole.value) return
  const checked = treeRef.value?.getCheckedKeys() || []        // 完全勾选的节点
  const halfChecked = treeRef.value?.getHalfCheckedKeys() || []  // 半选的父级节点

  const checkedSet = new Set(checked)

  // 根据菜单类型分配不同的操作权限
  const menuTypeMap = buildMenuTypeMap(menuTree.value)
  const ALL_ACTIONS = ['view', 'create', 'update', 'delete', 'release', 'resetPwd', 'toggleStatus']

  // 合并所有节点（完全勾选 + 半选），但区分 actions：
  // - 完全勾选的 type=2 菜单 → ALL_ACTIONS（用户明确勾选了整个菜单，给予全部操作权限）
  // - 半选的 type=2 菜单 → 只有 ['view']（菜单在侧边栏可见，具体操作由按钮权限控制）
  // - type=1 目录 → ['view']（标记目录可见，目录本身无权限标识）
  // - type=3 按钮 → ['view']（按钮的 permission_key 本身就是完整权限，actions 会被忽略）
  const allNodeIds = [...checked, ...halfChecked]

  try {
    await roleApi.saveRolePermissions(selectedRole.value.id, {
      roleMenus: allNodeIds.map((menuId: number) => {
        const menuType = menuTypeMap.get(menuId) || 2
        let actions: string[]
        if (menuType === 2) {
          // 菜单：全选→全部操作，半选→仅view
          actions = checkedSet.has(menuId) ? ALL_ACTIONS : ['view']
        } else {
          // 目录和按钮：统一 ['view']
          actions = ['view']
        }
        return { menuId, actions }
      })
    })
    ElMessage.success('权限保存成功')
  } catch (e) {
    ElMessage.error('权限保存失败')
  }
}

const showCreateDialog = () => {
  editingRole.value = null
  Object.assign(roleForm, {
    roleCode: '',
    roleName: '',
    roleDesc: '',
    userType: 1,
    dataScope: 1,
    sortOrder: 1,
    status: 1
  })
  dialogVisible.value = true
}

const showEditDialog = (row: Role) => {
  editingRole.value = row
  Object.assign(roleForm, {
    roleCode: row.roleCode,
    roleName: row.roleName,
    roleDesc: row.roleDesc || '',
    userType: row.userType,
    dataScope: row.dataScope,
    sortOrder: row.sortOrder,
    status: row.status
  })
  dialogVisible.value = true
}

const handleSaveRole = async () => {
  if (!roleForm.roleName || !roleForm.roleCode) {
    ElMessage.warning('请填写角色名称和编码')
    return
  }
  try {
    if (editingRole.value) {
      await roleApi.updateRole(editingRole.value.id, roleForm)
      ElMessage.success('编辑成功')
    } else {
      await roleApi.createRole(roleForm)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadRoles()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (row: Role) => {
  ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await roleApi.deleteRole(row.id)
      ElMessage.success('删除成功')
      if (selectedRole.value?.id === row.id) {
        selectedRole.value = null
      }
      loadRoles()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  loadRoles()
  loadMenuTree()
})
</script>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-tip {
  padding: 40px 0;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;

  .node-type {
    font-size: 12px;
    color: #999;
    background: #f0f0f0;
    padding: 1px 6px;
    border-radius: 3px;
  }

  .node-perm {
    font-size: 12px;
    color: #409eff;
  }
}
</style>
