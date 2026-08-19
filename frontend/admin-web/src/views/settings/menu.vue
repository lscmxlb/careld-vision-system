<template>
  <div class="menu-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>菜单管理</span>
          <el-button type="primary" size="small" @click="showCreateDialog(0)" v-permission="'settings:menu:create'">
            <el-icon><Plus /></el-icon> 新增菜单
          </el-button>
        </div>
      </template>

      <el-table
        :data="menuTree"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        border
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.menuType)" size="small">{{ getTypeText(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="menuIcon" label="图标" width="80" />
        <el-table-column prop="menuPath" label="路由路径" min-width="150" />
        <el-table-column prop="permissionKey" label="权限标识" min-width="180" />
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" link @click="showCreateDialog(row.id)" v-permission="'settings:menu:create'">新增子级</el-button>
            <el-button size="small" link @click="showEditDialog(row)" v-permission="'settings:menu:update'">编辑</el-button>
            <el-button size="small" link type="danger" @click="handleDelete(row)" v-permission="'settings:menu:delete'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingMenu ? '编辑菜单' : '新增菜单'" width="600px">
      <el-form :model="menuForm" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="menuForm.parentId"
            :data="parentOptions"
            :props="{ label: 'menuName', value: 'id', children: 'children' }"
            check-strictly
            placeholder="顶级菜单"
            clearable
          />
        </el-form-item>
        <el-form-item label="菜单名称" required>
          <el-input v-model="menuForm.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="菜单类型" required>
          <el-radio-group v-model="menuForm.menuType">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由路径" v-if="menuForm.menuType !== 3">
          <el-input v-model="menuForm.menuPath" placeholder="如 /organization/centers" />
        </el-form-item>
        <el-form-item label="图标" v-if="menuForm.menuType !== 3">
          <el-input v-model="menuForm.menuIcon" placeholder="如 OfficeBuilding" />
        </el-form-item>
        <el-form-item label="权限标识" v-if="menuForm.menuType !== 1">
          <el-input v-model="menuForm.permissionKey" placeholder="如 organization:center:view" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="menuForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="是否显示" v-if="menuForm.menuType !== 3">
          <el-switch v-model="menuForm.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="menuForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { menuApi } from '@/api'
import type { MenuItem, MenuRequest } from '@/types'

const menuTree = ref<MenuItem[]>([])
const dialogVisible = ref(false)
const editingMenu = ref<MenuItem | null>(null)
const menuForm = reactive<MenuRequest>({
  parentId: 0,
  menuName: '',
  menuType: 2,
  menuPath: '',
  menuIcon: '',
  permissionKey: '',
  sortOrder: 1,
  visible: 1,
  status: 1
})

const parentOptions = computed(() => {
  // 只显示目录和菜单类型作为父级选项
  const filterTree = (items: MenuItem[]): MenuItem[] => {
    return items
      .filter(item => item.menuType <= 2)
      .map(item => ({
        ...item,
        children: item.children ? filterTree(item.children) : undefined
      }))
  }
  return filterTree(menuTree.value)
})

const getTypeText = (type: number) => {
  const map: Record<number, string> = { 1: '目录', 2: '菜单', 3: '按钮' }
  return map[type] || ''
}

const typeTag = (type: number) => {
  const map: Record<number, string> = { 1: '', 2: 'success', 3: 'warning' }
  return map[type] || ''
}

const loadMenuTree = async () => {
  try {
    menuTree.value = await menuApi.getMenuTree()
  } catch (e) {
    ElMessage.error('加载菜单树失败')
  }
}

const showCreateDialog = (parentId: number) => {
  editingMenu.value = null
  Object.assign(menuForm, {
    parentId,
    menuName: '',
    menuType: parentId === 0 ? 1 : 2,
    menuPath: '',
    menuIcon: '',
    permissionKey: '',
    sortOrder: 1,
    visible: 1,
    status: 1
  })
  dialogVisible.value = true
}

const showEditDialog = (row: MenuItem) => {
  editingMenu.value = row
  Object.assign(menuForm, {
    parentId: row.parentId,
    menuName: row.menuName,
    menuType: row.menuType,
    menuPath: row.menuPath || '',
    menuIcon: row.menuIcon || '',
    permissionKey: row.permissionKey || '',
    sortOrder: row.sortOrder,
    visible: row.visible,
    status: row.status
  })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!menuForm.menuName) {
    ElMessage.warning('请输入菜单名称')
    return
  }
  try {
    if (editingMenu.value) {
      await menuApi.updateMenu(editingMenu.value.id, menuForm)
      ElMessage.success('编辑成功')
    } else {
      await menuApi.createMenu(menuForm)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadMenuTree()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (row: MenuItem) => {
  ElMessageBox.confirm(`确认删除菜单「${row.menuName}」吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await menuApi.deleteMenu(row.id)
      ElMessage.success('删除成功')
      loadMenuTree()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  loadMenuTree()
})
</script>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
