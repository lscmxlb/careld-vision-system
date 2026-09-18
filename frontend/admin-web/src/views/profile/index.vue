<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <!-- 用户信息卡片 -->
      <el-col :span="10">
        <el-card>
          <template #header>
            <span>个人信息</span>
          </template>
          <div class="user-info">
            <el-avatar :size="80" :icon="UserFilled" />
            <div class="user-detail">
              <h3>{{ userStore.userInfo?.realName }}</h3>
              <p>{{ userTypeText }}</p>
            </div>
          </div>
          <el-descriptions :column="1" border class="user-descriptions">
            <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ userStore.userInfo?.realName }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userStore.userInfo?.phone || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="用户类型">{{ userTypeText }}</el-descriptions-item>
            <el-descriptions-item label="所属运营中心">{{ userStore.userInfo?.centerName || '无' }}</el-descriptions-item>
            <el-descriptions-item label="所属代理商">{{ userStore.userInfo?.agentName || '无' }}</el-descriptions-item>
            <el-descriptions-item label="所属医院">{{ userStore.userInfo?.storeName || '无' }}</el-descriptions-item>
            <el-descriptions-item label="最近登录">{{ userStore.userInfo?.lastLoginTime || '—' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ userStore.userInfo?.createdAt || '—' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 修改密码卡片 -->
      <el-col :span="14">
        <el-card>
          <template #header>
            <span>修改密码</span>
          </template>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">确认修改</el-button>
              <el-button @click="resetPwdForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api'

defineOptions({ name: 'Profile' })

const userStore = useUserStore()

const userTypeMap: Record<number, string> = {
  1: '运营中心',
  2: '代理商',
  3: '医院',
  4: '医生',
  5: '家长'
}
const userTypeText = computed(() => {
  const t = userStore.userInfo?.userType
  return t ? (userTypeMap[t] || '未知') : ''
})

// 修改密码表单
const pwdFormRef = ref<FormInstance>()
const pwdLoading = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleChangePassword = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    pwdLoading.value = true
    try {
      await userApi.changeMyPassword(pwdForm.oldPassword, pwdForm.newPassword)
      ElMessage.success('密码修改成功')
      resetPwdForm()
    } catch (error: any) {
      ElMessage.error(error?.message || '密码修改失败')
    } finally {
      pwdLoading.value = false
    }
  })
}

const resetPwdForm = () => {
  pwdFormRef.value?.resetFields()
}
</script>

<style scoped lang="scss">
.profile-page {
  padding: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;

  .user-detail {
    h3 {
      margin: 0 0 4px 0;
      font-size: 18px;
    }

    p {
      margin: 0;
      color: #999;
      font-size: 14px;
    }
  }
}

.user-descriptions {
  margin-top: 10px;
}
</style>
