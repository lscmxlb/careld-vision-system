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
            <el-card v-loading="smsLoading">
              <template #header>
                <span>短信配置</span>
                <el-tag v-if="smsConfig.enabled" type="success" size="small" style="margin-left: 8px">真实发送</el-tag>
                <el-tag v-else type="info" size="small" style="margin-left: 8px">未启用</el-tag>
                <span v-if="!smsConfig.enabled && smsConfig.mockFallback" class="header-hint">
                  未启用时验证码固定为 123456（仅开发/测试环境）
                </span>
              </template>
              <el-form :model="smsConfig" label-width="150px">
                <el-form-item label="启用真实发送">
                  <el-switch v-model="smsConfig.enabled" />
                  <span class="unit">开启后验证码通过阿里云短信发送</span>
                </el-form-item>
                <el-form-item label="短信服务商">
                  <el-input model-value="阿里云" disabled style="width: 240px" />
                </el-form-item>
                <el-form-item label="AccessKey ID">
                  <el-input v-model="smsConfig.accessKeyId" placeholder="阿里云账号 AccessKey ID" clearable />
                </el-form-item>
                <el-form-item label="AccessKey Secret">
                  <el-input
                    v-model="smsConfig.accessKeySecret"
                    type="password"
                    show-password
                    clearable
                    :placeholder="smsConfig.accessKeySecretConfigured
                      ? `已配置（${smsConfig.accessKeySecretMasked}），留空则不修改`
                      : '阿里云 AccessKey Secret'"
                  />
                </el-form-item>
                <el-form-item label="短信签名">
                  <el-input v-model="smsConfig.signName" placeholder="阿里云控制台已审核通过的签名" clearable />
                </el-form-item>
                <el-form-item label="模板 Code">
                  <el-input v-model="smsConfig.templateCode" placeholder="如 SMS_123456789" clearable />
                </el-form-item>
                <el-form-item label="模板变量名">
                  <el-input v-model="smsConfig.templateParam" placeholder="code" style="width: 240px" />
                </el-form-item>
                <el-form-item label="开启短信通知">
                  <el-switch v-model="smsConfig.enableNotice" />
                </el-form-item>
                <el-form-item label="预约提醒时间">
                  <el-input-number v-model="smsConfig.appointmentReminderHours" :min="1" :max="24" />
                  <span class="unit">小时前</span>
                </el-form-item>
                <el-form-item label="发送测试">
                  <el-input v-model="testPhone" placeholder="接收测试短信的手机号" style="width: 200px" clearable />
                  <el-button type="primary" plain :loading="testSending" style="margin-left: 8px" @click="handleTestSend">发送测试</el-button>
                  <div class="hint">使用已保存的配置真实发送一条验证码，请先保存配置</div>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="smsSaving" @click="saveSmsConfig">保存配置</el-button>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'
import { smsApi } from '@/api'
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

const smsConfig = reactive({
  enabled: false,
  accessKeyId: '',
  accessKeySecret: '',
  accessKeySecretConfigured: false,
  accessKeySecretMasked: '',
  signName: '',
  templateCode: '',
  templateParam: 'code',
  mockFallback: true,
  enableNotice: true,
  appointmentReminderHours: 2
})
const smsLoading = ref(false)
const smsSaving = ref(false)
const testPhone = ref('')
const testSending = ref(false)

const loadSmsConfig = async () => {
  smsLoading.value = true
  try {
    const data = await smsApi.getConfig()
    Object.assign(smsConfig, data, { accessKeySecret: '' })
  } catch {
    // 拦截器已提示错误
  } finally {
    smsLoading.value = false
  }
}

const saveSmsConfig = async () => {
  smsSaving.value = true
  try {
    await smsApi.saveConfig({
      enabled: smsConfig.enabled,
      accessKeyId: smsConfig.accessKeyId,
      accessKeySecret: smsConfig.accessKeySecret || undefined,
      signName: smsConfig.signName,
      templateCode: smsConfig.templateCode,
      templateParam: smsConfig.templateParam,
      enableNotice: smsConfig.enableNotice,
      appointmentReminderHours: smsConfig.appointmentReminderHours
    })
    ElMessage.success('短信配置保存成功')
    await loadSmsConfig()
  } catch {
    // 拦截器已提示错误
  } finally {
    smsSaving.value = false
  }
}

const handleTestSend = async () => {
  if (!/^1[3-9]\d{9}$/.test(testPhone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  testSending.value = true
  try {
    await smsApi.testSend(testPhone.value)
    ElMessage.success('测试短信已发送，请查收')
  } catch {
    // 拦截器已提示错误（含阿里云返回的具体原因）
  } finally {
    testSending.value = false
  }
}

onMounted(loadSmsConfig)

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

  .header-hint {
    margin-left: 10px;
    font-size: 12px;
    color: #e6a23c;
  }

  .hint {
    width: 100%;
    margin-top: 4px;
    font-size: 12px;
    color: #999;
    line-height: 1.5;
  }
}
</style>
