<template>
  <div class="settings-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="系统参数" name="system">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card>
              <template #header>
                <span>系统参数配置</span>
              </template>
              <el-form :model="systemSettings" label-width="150px">
                <el-form-item label="系统名称">
                  <el-input v-model="systemSettings.systemName" />
                </el-form-item>
                <el-form-item label="Logo">
                  <el-upload
                    class="avatar-uploader"
                    :show-file-list="false"
                    :http-request="handleLogoUpload"
                    accept="image/*"
                  >
                    <img v-if="systemSettings.logoUrl" :src="systemSettings.logoUrl" class="avatar">
                    <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
                  </el-upload>
                  <el-button v-if="systemSettings.logoUrl" type="danger" size="small" @click="systemSettings.logoUrl = ''" style="margin-top: 8px;">移除Logo</el-button>
                </el-form-item>
                <el-form-item label="默认分页大小">
                  <el-input-number v-model="systemSettings.defaultPageSize" :min="10" :max="100" />
                </el-form-item>
                <el-form-item label="Token有效期(小时)">
                  <el-input-number v-model="systemSettings.tokenExpireHours" :min="1" :max="72" />
                </el-form-item>
                <el-form-item label="登录验证码">
                  <el-switch v-model="systemSettings.enableCaptcha" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="saveSystemSettings">保存设置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card>
              <template #header>
                <span>TV同步配置</span>
              </template>
              <el-form :model="syncSettings" label-width="150px">
                <el-form-item label="自动同步间隔(分钟)">
                  <el-input-number v-model="syncSettings.autoSyncInterval" :min="5" :max="120" />
                </el-form-item>
                <el-form-item label="最大批次大小">
                  <el-input-number v-model="syncSettings.maxBatchSize" :min="10" :max="500" />
                </el-form-item>
                <el-form-item label="数据保留天数">
                  <el-input-number v-model="syncSettings.dataRetentionDays" :min="30" :max="365" />
                </el-form-item>
                <el-form-item label="重试间隔(秒)">
                  <el-input
                    v-model="syncSettings.retryIntervals"
                    placeholder="逗号分隔，如：60,300,900,3600"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="saveSyncSettings">保存配置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <el-card>
              <template #header>
                <span>通知配置</span>
              </template>
              <el-form :model="notificationSettings" label-width="150px">
                <el-form-item label="开启短信通知">
                  <el-switch v-model="notificationSettings.enableSms" />
                </el-form-item>
                <el-form-item label="短信服务商">
                  <el-select v-model="notificationSettings.smsProvider">
                    <el-option label="阿里云" value="aliyun" />
                    <el-option label="腾讯云" value="tencent" />
                  </el-select>
                </el-form-item>
                <el-form-item label="预约提醒时间">
                  <el-input-number v-model="notificationSettings.appointmentReminderHours" :min="1" :max="24" />
                  <span class="unit">小时前</span>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="saveNotificationSettings">保存配置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card>
              <template #header>
                <span>安全设置</span>
              </template>
              <el-form :model="securitySettings" label-width="150px">
                <el-form-item label="密码最小长度">
                  <el-input-number v-model="securitySettings.minPasswordLength" :min="6" :max="20" />
                </el-form-item>
                <el-form-item label="登录失败锁定次数">
                  <el-input-number v-model="securitySettings.maxLoginAttempts" :min="3" :max="10" />
                </el-form-item>
                <el-form-item label="锁定时间(分钟)">
                  <el-input-number v-model="securitySettings.lockDurationMinutes" :min="5" :max="60" />
                </el-form-item>
                <el-form-item label="强制密码修改周期(天)">
                  <el-input-number v-model="securitySettings.passwordExpireDays" :min="0" :max="365" />
                  <span class="unit">0表示不强制</span>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="saveSecuritySettings">保存设置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane v-if="permStore.hasPermission('settings:role:view')" label="角色管理" name="role">
        <RoleManagement />
      </el-tab-pane>

      <el-tab-pane v-if="permStore.hasPermission('settings:menu:view')" label="菜单管理" name="menu">
        <MenuManagement />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminSettings' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'
import RoleManagement from './role.vue'
import MenuManagement from './menu.vue'
import type { UploadRequestOptions } from 'element-plus'

const appStore = useAppStore()
const permStore = usePermissionStore()
const activeTab = ref('system')

// 根据权限自动切换tab
const initTab = () => {
  if (!permStore.hasPermission('settings:role:view') && !permStore.hasPermission('settings:menu:view')) {
    activeTab.value = 'system'
  }
}
initTab()

const systemSettings = reactive({
  systemName: appStore.systemSettings.systemName,
  logoUrl: appStore.systemSettings.logoUrl,
  defaultPageSize: appStore.systemSettings.defaultPageSize,
  tokenExpireHours: appStore.systemSettings.tokenExpireHours,
  enableCaptcha: appStore.systemSettings.enableCaptcha
})

const syncSettings = reactive({
  autoSyncInterval: 30,
  maxBatchSize: 100,
  dataRetentionDays: 90,
  retryIntervals: '60,300,900,3600'
})

const notificationSettings = reactive({
  enableSms: true,
  smsProvider: 'aliyun',
  appointmentReminderHours: 2
})

const securitySettings = reactive({
  minPasswordLength: 6,
  maxLoginAttempts: 5,
  lockDurationMinutes: 30,
  passwordExpireDays: 90
})

// 自定义上传：转为base64 data URL 存储
const handleLogoUpload = (options: UploadRequestOptions) => {
  const file = options.file as File
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('Logo文件不能超过2MB')
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    systemSettings.logoUrl = e.target?.result as string
    ElMessage.success('Logo已加载，点击保存设置生效')
  }
  reader.readAsDataURL(file)
}

const saveSystemSettings = () => {
  appStore.updateSystemSettings({ ...systemSettings })
  ElMessage.success('系统设置保存成功')
}

const saveSyncSettings = () => {
  ElMessage.success('同步配置保存成功')
}

const saveNotificationSettings = () => {
  ElMessage.success('通知配置保存成功')
}

const saveSecuritySettings = () => {
  ElMessage.success('安全设置保存成功')
}
</script>

<style scoped lang="scss">
.settings-page {
  .avatar-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: border-color 0.3s;

      &:hover {
        border-color: #409eff;
      }
    }

    .avatar {
      width: 178px;
      height: 178px;
      display: block;
    }

    .avatar-uploader-icon {
      font-size: 28px;
      color: #8c939d;
      width: 178px;
      height: 178px;
      text-align: center;
      line-height: 178px;
    }
  }

  .unit {
    margin-left: 10px;
    color: #999;
  }
}
</style>
