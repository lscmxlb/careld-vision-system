<template>
  <div class="settings-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="系统参数" name="system">
        <el-row :gutter="20" class="equal-height-row">
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
                  <el-button v-if="systemSettings.logoUrl" type="danger" size="small" class="logo-remove" @click="systemSettings.logoUrl = ''">移除Logo</el-button>
                </el-form-item>
                <el-row :gutter="24">
                  <el-col :span="12">
                    <el-form-item label="默认分页大小">
                      <el-input-number v-model="systemSettings.defaultPageSize" :min="10" :max="100" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="Token有效期(小时)">
                      <el-input-number v-model="systemSettings.tokenExpireHours" :min="1" :max="72" />
                    </el-form-item>
                  </el-col>
                </el-row>
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

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="24">
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
                <el-row :gutter="24">
                  <el-col :span="12">
                    <el-divider content-position="left">
                      <span class="divider-text">基础配置（短信验证码，阿里云通道）</span>
                    </el-divider>
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
                    <el-divider content-position="left">
                      <span class="divider-text">通知开关与提醒</span>
                    </el-divider>
                    <el-form-item label="开启短信通知">
                      <el-switch v-model="smsConfig.enableNotice" />
                    </el-form-item>
                    <el-form-item label="养护提醒提前时间">
                      <el-input-number v-model="smsConfig.careReminderMinutes" :min="5" :max="1440" :step="5" />
                      <span class="unit">分钟前</span>
                      <div class="hint">
                        预约成功短信在预约成功当下即时发送；医院端开启「养护提醒」后，此处设的分钟数到达时（预约开始前）再发一条同样的预约成功短信
                      </div>
                    </el-form-item>
                  </el-col>

                  <el-col :span="12">
                    <el-divider content-position="left">
                      <span class="divider-text">业务通知短信（建档 / 预约 / 取消 / 养护完成）</span>
                    </el-divider>
                    <el-form-item label="通知模板 Code">
                      <el-input v-model="smsConfig.noticeTemplateCode" placeholder="如 SMS_987654321，与验证码模板分开申请" clearable />
                    </el-form-item>
                    <el-form-item label="通知模板变量">
                      <el-input v-model="smsConfig.noticeTemplateFields" placeholder="hist,date,time" clearable />
                      <div class="hint">按模板正文里变量出现的先后顺序填，逗号分隔</div>
                    </el-form-item>
                    <el-form-item label="变量含义">
                      <el-input v-model="smsConfig.noticeTemplateRoles" placeholder="storeName,reserveDate,timeRange" clearable />
                      <div class="hint">
                        与变量一一对应，可选值：storeName 医院名称 / childName 儿童姓名 / noticeTitle 通知类型 /
                        noticeStatus 状态短语 / noticeTime 业务时间 / reserveDate 预约日期 / timeRange 预约时段 /
                        timeStart 开始时间 / timeEnd 结束时间 / reason 取消原因 / oldReserveDate 原预约日期 /
                        oldTimeRange 原预约时段 / content 整段正文
                      </div>
                    </el-form-item>
                    <el-form-item label="适用通知事件">
                      <el-input v-model="smsConfig.noticeTemplateEvents" placeholder="reserve_created" clearable />
                      <div class="hint">
                        该模板文案只适用于这些事件（逗号分隔），其它事件不发短信，避免「取消/调整」套用「预约成功」文案。
                        可选：reserve_created 预约成功 / reserve_cancelled 预约取消 / reserve_adjusted 预约调整 /
                        care_reminder 养护提醒 / child_created 建档 / care_completed 养护完成；留空表示所有事件共用
                      </div>
                    </el-form-item>
                    <el-divider content-position="left">
                      <span class="divider-text">其它事件短信模板（各事件文案不同，单独配置；模板 Code 留空表示该事件不发短信）</span>
                    </el-divider>
                    <el-form-item
                      v-for="tpl in smsConfig.eventTemplates"
                      :key="tpl.eventType"
                      :label="eventTemplateLabel(tpl.eventType)"
                    >
                      <div class="event-template-row">
                        <el-input v-model="tpl.templateCode" placeholder="模板 Code" style="width: 190px" clearable />
                        <el-input v-model="tpl.templateFields" placeholder="模板变量名" style="width: 200px" clearable />
                        <el-input v-model="tpl.templateRoles" placeholder="变量含义" style="width: 220px" clearable />
                      </div>
                      <div class="hint">{{ EVENT_TEMPLATE_HINTS[tpl.eventType] }}</div>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item label="发送测试">
                  <el-input v-model="testPhone" placeholder="接收测试短信的手机号" style="width: 200px" clearable />
                  <el-select v-model="testEventType" style="width: 170px">
                    <el-option
                      v-for="opt in TEST_EVENT_OPTIONS"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                  <div class="test-buttons">
                    <el-button type="primary" plain :loading="testSending" @click="handleTestSend">测试验证码</el-button>
                    <el-button type="primary" plain :loading="noticeTestSending" @click="handleNoticeTestSend">测试通知短信</el-button>
                    <el-button plain :loading="noticePreviewing" @click="handleNoticePreview">预览变量</el-button>
                  </div>
                  <div class="hint">
                    「测试通知短信」「预览变量」按右侧所选事件取模板（预约成功/养护提醒用主模板，其余事件用各自的
                    事件模板）；真实发送前请先保存配置
                  </div>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="smsSaving" @click="saveSmsConfig">保存配置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="24">
            <el-card v-loading="wechatLoading">
              <template #header>
                <span>微信通知配置（公众号）</span>
                <el-tag v-if="wechatConfig.realSendReady" type="success" size="small" style="margin-left: 8px">真实发送</el-tag>
                <el-tag v-else type="info" size="small" style="margin-left: 8px">模拟通道</el-tag>
                <span class="header-hint">家长在家长端「我的 → 微信公众号」绑定后，业务通知会同时推送微信模板消息（免费）</span>
              </template>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form :model="wechatConfig" label-width="150px">
                    <el-form-item label="公众号 AppID">
                      <el-input v-model="wechatConfig.appId" placeholder="wx 开头的 AppID" clearable />
                    </el-form-item>
                    <el-form-item label="公众号 AppSecret">
                      <el-input
                        v-model="wechatConfig.appSecret"
                        type="password"
                        show-password
                        :placeholder="wechatConfig.appSecretConfigured
                          ? `已配置（${wechatConfig.appSecretMasked}），留空则不修改`
                          : '公众平台「基本配置」中的 AppSecret'"
                      />
                    </el-form-item>
                    <el-form-item label="公众号名称">
                      <el-input v-model="wechatConfig.accountName" placeholder="家长端展示用，如：可尔欧得视力养护" clearable />
                    </el-form-item>
                    <el-form-item label="模板 ID">
                      <el-input v-model="wechatConfig.templateId" placeholder="模板消息 / 订阅通知 的模板 ID" clearable />
                    </el-form-item>
                    <el-form-item label="模板类型">
                      <el-radio-group v-model="wechatConfig.templateType">
                        <el-radio-button value="template">公众号模板消息</el-radio-button>
                        <el-radio-button value="subscribe">订阅通知</el-radio-button>
                      </el-radio-group>
                      <div class="hint">不确定时先点右侧「诊断」，探针会判定这个模板 ID 的归属</div>
                    </el-form-item>
                    <el-form-item label="字段映射">
                      <el-input v-model="wechatConfig.fields" placeholder="first,keyword1,keyword2,remark" />
                      <div class="hint">按公众号模板里的字段名填写（逗号分隔），顺序即模板展示顺序</div>
                    </el-form-item>
                    <el-form-item label="字段语义">
                      <el-input v-model="wechatConfig.fieldRoles" placeholder="summary,childName,noticeTitle,remark" />
                      <div class="hint">
                        与字段映射一一对应，决定每个字段填什么内容：summary 通知正文 / childName 儿童姓名 /
                        noticeTitle 通知类型 / noticeTime 业务时间 / noticeStatus 状态短语 / storeName 医院名称 / remark 固定说明
                      </div>
                    </el-form-item>
                    <el-form-item label="回调域名前缀">
                      <el-input v-model="wechatConfig.publicBaseUrl" placeholder="https://h5.careld.net" clearable />
                      <div class="hint">
                        备案域名（careld.net 的子域）解析到本服务器后填这里，用于「服务器配置 URL」和「网页授权域名」；
                        填完记得保存
                      </div>
                    </el-form-item>
                    <el-form-item>
                      <el-button type="primary" :loading="wechatSaving" @click="saveWechatConfig">保存配置</el-button>
                    </el-form-item>
                  </el-form>
                </el-col>

                <el-col :span="12">
                  <el-form label-width="120px">
                    <el-form-item label="联调诊断">
                      <el-button type="primary" plain :loading="probeLoading" @click="handleProbe">
                        诊断（取 token + 拉模板列表）
                      </el-button>
                    </el-form-item>
                    <el-form-item label="测试 openid">
                      <el-input v-model="testOpenid" placeholder="关注该公众号的 openid" clearable style="width: 260px" />
                      <el-button type="primary" plain :loading="wechatTesting" style="margin-left: 8px" @click="handleWechatTest">
                        发送测试
                      </el-button>
                    </el-form-item>
                    <el-form-item v-if="probeResult?.followers?.length" label="关注者">
                      <div class="follower-list">
                        <el-tag
                          v-for="(openid, idx) in probeResult.followers"
                          :key="openid"
                          :type="testOpenid === openid ? 'success' : 'info'"
                          style="cursor: pointer"
                          @click="testOpenid = openid"
                        >
                          {{ probeResult.followerTotal ? `${idx + 1}. ` : '' }}{{ openid.slice(-8) }}
                        </el-tag>
                        <span class="hint">共 {{ probeResult.followerTotal ?? probeResult.followers.length }} 人，点击填入测试 openid</span>
                      </div>
                    </el-form-item>
                    <el-form-item v-if="probeResult?.followerHint" label="关注者">
                      <span class="hint">{{ probeResult.followerHint }}</span>
                    </el-form-item>
                  </el-form>
                  <el-alert
                    v-if="probeResult && probeResult.hint"
                    :title="probeResult.hint"
                    type="warning"
                    :closable="false"
                    show-icon
                    style="margin-bottom: 8px"
                  />
                  <el-alert
                    v-if="testHint"
                    :title="testHint"
                    type="warning"
                    :closable="false"
                    show-icon
                    style="margin-bottom: 8px"
                  />
                  <pre v-if="probeText" class="debug-box">{{ probeText }}</pre>
                  <pre v-if="testText" class="debug-box">{{ testText }}</pre>
                </el-col>
              </el-row>

              <el-divider content-position="left">公众平台配置与家长订阅授权闭环</el-divider>
              <el-row :gutter="20">
                <el-col :span="14">
                  <el-form label-width="150px">
                    <el-form-item label="服务器配置 URL">
                      <el-input :model-value="wechatConfig.callbackUrl" readonly style="width: 320px" />
                      <el-button style="margin-left: 8px" @click="copyText(wechatConfig.callbackUrl, '服务器配置 URL')">
                        复制
                      </el-button>
                    </el-form-item>
                    <el-form-item label="服务器配置 Token">
                      <el-input :model-value="wechatConfig.serverToken" readonly style="width: 320px" />
                      <el-button style="margin-left: 8px" @click="copyText(wechatConfig.serverToken, '服务器配置 Token')">
                        复制
                      </el-button>
                      <el-button type="warning" plain :loading="tokenRegenerating" @click="regenerateToken">
                        重新生成
                      </el-button>
                    </el-form-item>
                    <el-form-item label="订阅授权页">
                      <el-input :model-value="authPageUrl" readonly style="width: 320px" />
                      <el-button style="margin-left: 8px" @click="copyText(authPageUrl, '订阅授权页地址')">复制</el-button>
                      <div class="hint">
                        家长在微信内打开该页即可：静默完成 openid 绑定 → 弹出「订阅通知」授权 → 之后业务通知可下发微信。
                        建议把它放进公众号自定义菜单（如「消息通知」），家长从公众号点进来最顺。
                      </div>
                    </el-form-item>
                  </el-form>
                  <el-alert type="info" :closable="false" show-icon title="上线前需要完成的四步配置">
                    <template #default>
                      <div class="config-steps">
                        <div>1. 域名解析：把 <b>careld.net</b> 的子域（如 h5.careld.net）A 记录指向本服务器公网 IP，云服务器安全组放行 <b>80 / 443</b>。</div>
                        <div>2. 服务器配置：公众平台「设置与开发 → 基本配置 → 服务器配置(已启用)」，URL 填上面的服务器配置 URL、Token 填上面的 Token、消息加解密方式选 <b>明文模式</b>。</div>
                        <div>3. 域名授权：公众平台「设置与开发 → 公众号设置 → 功能设置」，<b>JS接口安全域名</b> 与 <b>网页授权域名</b> 都填回调域名（不带 http://，如 h5.careld.net）；把微信下载的 <b>MP_verify_*.txt</b> 放到域名根目录（交给我们部署到服务器根目录即可）。</div>
                        <div>4. 订阅模板：公众号内的模板须为 <b>订阅通知</b> 且字段与上方「字段映射」一致；家长授权后即可真实下发。</div>
                      </div>
                    </template>
                  </el-alert>
                </el-col>
                <el-col :span="10">
                  <div class="official-qr">
                    <div class="qr-title">公众号关注二维码（真实）</div>
                    <img :src="officialQr" alt="可尔欧得公众号" />
                    <div class="hint">
                      家长端「我的 → 微信公众号」展示的也是这张图。未配置回调域名时，家长扫码关注后点「我已关注，确认绑定」
                      可先完成 openid 绑定；配置好域名后在微信内点「开启消息通知授权」完成订阅授权即可收到消息。
                    </div>
                  </div>
                </el-col>
              </el-row>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'
import { smsApi, wechatApi } from '@/api'
import RoleManagement from './role.vue'
import MenuManagement from './menu.vue'
import type { UploadRequestOptions } from 'element-plus'
import type { SmsEventTemplate, WechatProbeResult, WechatTestResult } from '@/api'

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

const smsConfig = reactive({
  enabled: false,
  accessKeyId: '',
  accessKeySecret: '',
  accessKeySecretConfigured: false,
  accessKeySecretMasked: '',
  signName: '',
  templateCode: '',
  templateParam: 'code',
  noticeTemplateCode: '',
  noticeTemplateParam: '',
  noticeTemplateFields: '',
  noticeTemplateRoles: '',
  noticeTemplateEvents: '',
  mockFallback: true,
  enableNotice: true,
  careReminderMinutes: 90,
  eventTemplates: [] as SmsEventTemplate[]
})

/** 需要独立模板的事件（顺序与后端返回一致） */
const EVENT_TEMPLATE_LABELS: Record<string, string> = {
  reserve_cancelled: '预约取消短信',
  reserve_adjusted: '预约调整短信',
  child_created: '建档成功短信',
  care_completed: '养护完成短信'
}

/** 各事件模板的固定文案与建议变量映射（阿里云模板申请后照填即可） */
const EVENT_TEMPLATE_HINTS: Record<string, string> = {
  reserve_cancelled:
    '模板文案「您已经取消了${date}，${time}时段在${hist}预约的视力养护服务，取消原因是${of}。」→ 变量名 date,time,hist,of，变量含义 reserveDate,timeRange,storeName,reason',
  reserve_adjusted:
    '模板文案「原预约[${date}，${time}]已调整，新的预约时间是${date1}，${time1}。」→ 变量名 date,time,date1,time1，变量含义 oldReserveDate,oldTimeRange,reserveDate,timeRange',
  child_created:
    '模板文案「您已经为${name}在${hist}建立了视力健康档案，并通过了审核。」→ 变量名 name,hist，变量含义 childName,storeName',
  care_completed:
    '模板文案「${name}在${hist}预约的视力养护服务已经完成，你可以到诊约助手公众号家长端查看养护详细数据。」→ 变量名 name,hist，变量含义 childName,storeName'
}

const eventTemplateLabel = (eventType: string) => EVENT_TEMPLATE_LABELS[eventType] || eventType

/** 通知测试可选事件：预约成功/养护提醒用主模板，其余用事件模板 */
const TEST_EVENT_OPTIONS = [
  { value: 'reserve_created', label: '预约成功' },
  { value: 'care_reminder', label: '养护提醒' },
  { value: 'reserve_cancelled', label: '预约取消' },
  { value: 'reserve_adjusted', label: '预约调整' },
  { value: 'child_created', label: '建档成功' },
  { value: 'care_completed', label: '养护完成' }
]

const smsLoading = ref(false)
const smsSaving = ref(false)
const testPhone = ref('')
const testEventType = ref('reserve_created')
const testSending = ref(false)
const noticeTestSending = ref(false)
const noticePreviewing = ref(false)

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
      noticeTemplateCode: smsConfig.noticeTemplateCode,
      noticeTemplateParam: smsConfig.noticeTemplateParam,
      noticeTemplateFields: smsConfig.noticeTemplateFields,
      noticeTemplateRoles: smsConfig.noticeTemplateRoles,
      noticeTemplateEvents: smsConfig.noticeTemplateEvents,
      enableNotice: smsConfig.enableNotice,
      careReminderMinutes: smsConfig.careReminderMinutes,
      eventTemplates: smsConfig.eventTemplates.map((tpl) => ({ ...tpl }))
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
    ElMessage.success('测试验证码已发送，请查收')
  } catch {
    // 拦截器已提示错误（含阿里云返回的具体原因）
  } finally {
    testSending.value = false
  }
}

const handleNoticeTestSend = async () => {
  if (!/^1[3-9]\d{9}$/.test(testPhone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  noticeTestSending.value = true
  try {
    await smsApi.testSendNotice(testPhone.value, testEventType.value)
    ElMessage.success('测试通知短信已发送，请查收')
  } catch {
    // 拦截器已提示错误（含阿里云返回的具体原因）
  } finally {
    noticeTestSending.value = false
  }
}

const handleNoticePreview = async () => {
  noticePreviewing.value = true
  try {
    const params = await smsApi.previewNotice(testEventType.value)
    ElMessageBox.alert(
      `<div style="line-height:1.8">按已保存的配置生成，阿里云模板参数字段与取值：<br/><code style="word-break:break-all">${params}</code></div>`,
      '模板变量预览（未发送短信）',
      { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' }
    )
  } catch {
    // 拦截器已提示错误
  } finally {
    noticePreviewing.value = false
  }
}

onMounted(() => {
  loadSmsConfig()
  loadWechatConfig()
})

// ==================== 微信通知配置（公众号模板消息） ====================
const wechatConfig = reactive({
  appId: '',
  appSecret: '',
  appSecretConfigured: false,
  appSecretMasked: '',
  templateId: '',
  templateType: 'template',
  fields: 'first,keyword1,keyword2,remark',
  fieldRoles: 'summary,childName,noticeTitle,remark',
  accountName: '',
  realSendReady: false,
  publicBaseUrl: '',
  callbackUrl: '',
  serverToken: ''
})
const wechatLoading = ref(false)
const wechatSaving = ref(false)
const probeLoading = ref(false)
const probeResult = ref<WechatProbeResult | null>(null)
const probeText = ref('')
const testOpenid = ref('')
const wechatTesting = ref(false)
const testText = ref('')
const testHint = ref('')
const tokenRegenerating = ref(false)
/** 公众号真实关注二维码（public 目录，随构建一起发布） */
const officialQr = '/wechat-official-qr.jpg'

/** 家长端订阅授权页（微信内打开，静默绑定 + 拉起订阅授权） */
const authPageUrl = computed(() => {
  const base = (wechatConfig.publicBaseUrl || '').replace(/\/+$/, '')
  return base ? `${base}/static/wechat-auth.html` : '/static/wechat-auth.html（先填回调域名前缀）'
})

const copyText = async (text: string, label: string) => {
  if (!text) {
    ElMessage.warning(`${label} 暂为空`)
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`${label} 已复制`)
  } catch {
    ElMessage.warning('浏览器剪贴板不可用，请手动选中复制')
  }
}

const regenerateToken = async () => {
  tokenRegenerating.value = true
  try {
    const data = await wechatApi.regenerateServerToken()
    Object.assign(wechatConfig, data, { appSecret: '' })
    ElMessage.success('已生成新 Token，请到公众平台「服务器配置」同步替换')
  } catch {
    // 拦截器已提示错误
  } finally {
    tokenRegenerating.value = false
  }
}

const loadWechatConfig = async () => {
  wechatLoading.value = true
  try {
    const data = await wechatApi.getConfig()
    Object.assign(wechatConfig, data, { appSecret: '' })
  } catch {
    // 拦截器已提示错误
  } finally {
    wechatLoading.value = false
  }
}

const saveWechatConfig = async () => {
  wechatSaving.value = true
  try {
    const data = await wechatApi.saveConfig({
      appId: wechatConfig.appId,
      appSecret: wechatConfig.appSecret || undefined,
      templateId: wechatConfig.templateId,
      templateType: wechatConfig.templateType,
      fields: wechatConfig.fields,
      fieldRoles: wechatConfig.fieldRoles,
      accountName: wechatConfig.accountName,
      publicBaseUrl: wechatConfig.publicBaseUrl || undefined
    })
    Object.assign(wechatConfig, data, { appSecret: '' })
    ElMessage.success('微信通知配置保存成功')
  } catch {
    // 拦截器已提示错误
  } finally {
    wechatSaving.value = false
  }
}

const handleProbe = async () => {
  probeLoading.value = true
  try {
    const data = await wechatApi.probe()
    probeResult.value = data
    probeText.value = formatProbe(data)
  } catch {
    // 拦截器已提示错误
  } finally {
    probeLoading.value = false
  }
}

const handleWechatTest = async () => {
  if (!testOpenid.value.trim()) {
    ElMessage.warning('请填写接收测试消息的 openid')
    return
  }
  wechatTesting.value = true
  testHint.value = ''
  try {
    const data = await wechatApi.testSend(testOpenid.value.trim())
    testHint.value = data.hint || ''
    testText.value = formatTest(data)
    if (Number(data.errcode) === 0) {
      ElMessage.success('微信模板消息发送成功，请在微信中查看')
    } else {
      ElMessage.error(`发送失败：${data.errcode ?? '-'} ${data.errmsg ?? ''}`)
    }
  } catch {
    // 拦截器已提示错误
  } finally {
    wechatTesting.value = false
  }
}

const formatProbe = (data: WechatProbeResult) => {
  const lines: string[] = []
  lines.push(`【access_token】${data.tokenOk ? '获取成功' : `失败：${data.tokenErrcode ?? '-'} ${data.tokenErrmsg ?? ''}`}`)
  const typeLabel = data.matchedType === 'template'
    ? '公众号模板消息'
    : data.matchedType === 'subscribe' ? '订阅通知' : '未在模板列表中找到'
  lines.push(`【配置模板】${data.configuredTemplateId || '（未填写）'}`)
  lines.push(`【归属判定】${typeLabel}${data.matchedTitle ? `｜${data.matchedTitle}` : ''}`)
  if (data.matchedContent) {
    lines.push('【模板内容】')
    lines.push(data.matchedContent)
  }
  if (data.matchedFields && data.matchedFields.length) {
    lines.push(`【可用字段】${data.matchedFields.join(', ')}`)
  }
  if (data.followers) {
    lines.push(`【关注者】共 ${data.followerTotal ?? data.followers.length} 人`
      + (data.followerHint ? `（${data.followerHint}）` : ''))
  }
  lines.push(`【模板消息列表】${(data.templateMessageTemplates || []).length} 条`)
  ;(data.templateMessageTemplates || []).forEach((item) => {
    lines.push(`　- ${item.title || '(无标题)'} → ${item.templateId || ''}`)
  })
  lines.push(`【订阅通知列表】${(data.subscribeTemplates || []).length} 条`)
  ;(data.subscribeTemplates || []).forEach((item) => {
    lines.push(`　- ${item.title || '(无标题)'} → ${item.templateId || ''}`)
  })
  return lines.join('\n')
}

const formatTest = (data: WechatTestResult) => {
  const lines: string[] = []
  lines.push(`【阶段】${data._stage || '-'}${data._api ? `（${data._api}）` : ''}`)
  lines.push(`【微信返回】errcode=${data.errcode ?? '-'} errmsg=${data.errmsg ?? '-'}`)
  lines.push(`【发送对象】openid=${data.openid || testOpenid.value}`)
  lines.push(`【使用模板】${data.templateId || '-'}（${data.templateType || '-'}）`)
  lines.push(`【字段映射】${data.fields || '-'}`)
  lines.push(`【字段语义】${data.fieldRoles || '-'}`)
  if (data._requestBody) {
    lines.push('【请求体】')
    lines.push(data._requestBody)
  }
  return lines.join('\n')
}

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

const saveSecuritySettings = () => {
  ElMessage.success('安全设置保存成功')
}
</script>

<style scoped lang="scss">
.settings-page {
  // 系统参数配置 / 安全设置两张卡片等高（较矮的一张随行高拉伸）
  .equal-height-row > .el-col > .el-card {
    height: 100%;
  }

  .avatar-uploader {
    :deep(.el-upload) {
      display: block;
      width: fit-content;
      line-height: 0; /* 去掉行内基线的多余间隙，盒子高度=图片高度+边框 */
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

    /* 紧凑预览：与输入框同高（30px + 1px 边框 = 32px），宽度随图片比例（最宽 160px） */
    .avatar {
      height: 30px;
      width: auto;
      max-width: 160px;
      display: block;
    }

    .avatar-uploader-icon {
      display: block;
      font-size: 16px;
      color: #8c939d;
      width: 120px;
      height: 30px;
      text-align: center;
      line-height: 30px;
    }
  }

  .logo-remove {
    margin-left: 12px;
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

  .divider-text {
    font-size: 13px;
    color: #606266;
  }

  .test-buttons {
    display: inline-flex;
    gap: 8px;
    margin-left: 8px;
  }

  .event-template-row {
    display: inline-flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .follower-list {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    max-width: 100%;
  }

  .config-steps {
    margin-top: 6px;
    font-size: 13px;
    line-height: 1.9;

    b {
      color: #303133;
    }
  }

  .official-qr {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 8px 16px;

    .qr-title {
      margin-bottom: 10px;
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }

    img {
      width: 240px;
      height: 240px;
      object-fit: contain;
      border: 1px solid #ebeef5;
      border-radius: 6px;
      background: #fff;
    }
  }

  .debug-box {
    max-height: 320px;
    overflow: auto;
    margin: 0 0 8px;
    padding: 10px 12px;
    background: #f7f8fa;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    font-size: 12px;
    line-height: 1.6;
    color: #303133;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
