<template>
  <div class="recharge-page">
    <!-- 微信支付配置（总部维护，真实支付凭据 + 联调模拟开关） -->
    <el-card v-loading="configLoading" class="config-card">
      <template #header>
        <div class="card-header">
          <span>微信支付配置</span>
          <el-tag v-if="config.realReady" type="success" size="small">真实支付已就绪</el-tag>
          <el-tag v-else-if="config.mockEnabled" type="warning" size="small">模拟支付（联调中）</el-tag>
          <el-tag v-else type="info" size="small">未启用真实支付</el-tag>
          <span class="header-hint">
            医院端「通知服务 → 可用余额」中的扫码充值依赖此配置；未就绪时医院端不能下单
          </span>
        </div>
      </template>

      <el-form :model="config" label-width="160px">
        <el-row :gutter="24">
          <el-col :span="12" class="config-col config-col--left">
            <el-form-item label="启用真实微信支付">
              <el-switch v-model="config.enabled" />
              <span class="unit">开启后医院端生成真实微信 Native 支付二维码</span>
            </el-form-item>
            <el-form-item label="微信支付商户号">
              <el-input v-model="config.mchId" placeholder="微信支付商户平台 mchid" clearable />
            </el-form-item>
            <el-form-item label="APIv3 密钥">
              <el-input
                v-model="config.apiV3Key"
                type="password"
                show-password
                clearable
                :placeholder="config.apiV3KeyConfigured ? `已配置（${config.apiV3KeyMasked}），留空表示不修改` : '商户平台设置的 32 位 APIv3 密钥'"
              />
            </el-form-item>
            <el-form-item label="商户证书序列号">
              <el-input v-model="config.serialNo" placeholder="apiclient_cert.pem 对应的证书序列号" clearable />
            </el-form-item>
            <el-form-item label="微信支付公钥ID">
              <el-input v-model="config.publicKeyId" placeholder="商户平台-API安全 的 PUB_KEY_ID_…" clearable />
            </el-form-item>
            <el-form-item class="config-actions">
              <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
              <el-button :loading="probing" @click="handleProbe">配置诊断</el-button>
            </el-form-item>
          </el-col>
          <el-col :span="12" class="config-col config-col--right">
            <el-form-item label="商户API私钥路径">
              <el-input v-model="config.privateKeyPath" placeholder="服务器上 apiclient_key.pem 的绝对路径" clearable />
            </el-form-item>
            <el-form-item label="微信支付公钥路径">
              <el-input v-model="config.publicKeyPath" placeholder="服务器上 pub_key.pem 的绝对路径" clearable />
              <div class="field-tip">
                商户平台「API安全」下载的微信支付公钥；回调验签用（未配置时回退下载微信平台证书）
              </div>
            </el-form-item>
            <el-form-item label="支付 AppID">
              <el-input v-model="config.appId" placeholder="留空时复用公众号 AppID" clearable />
            </el-form-item>
            <el-form-item label="支付结果回调地址">
              <el-input v-model="config.notifyUrl" :placeholder="config.suggestedNotifyUrl || 'https://域名/api/v1/notify/pay/callback/wechat'" clearable />
              <div class="field-tip">
                微信要求 HTTPS 公网地址；回调域名就绪后填
                <b>{{ config.suggestedNotifyUrl || 'https://域名/api/v1/notify/pay/callback/wechat' }}</b>
              </div>
            </el-form-item>
            <el-form-item label="模拟支付（联调）">
              <el-switch v-model="config.mockEnabled" />
              <span class="unit warning-text">仅联调期使用：可跳过微信真实收款直接入账，正式运营必须关闭</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-alert
          v-if="probeResult"
          :type="probeResult.privateKeyOk && (probeResult.publicKeyOk || (probeResult.certificateCount ?? 0) > 0) ? 'success' : 'warning'"
          :closable="false"
          show-icon
          class="probe-result"
        >
          <template #title>
            验签方式：{{ probeResult.verifyMode || '—' }}；私钥：{{ probeResult.privateKeyMessage }}；公钥：{{ probeResult.publicKeyMessage }}
          </template>
          <div class="probe-sub">
            商户号 {{ probeResult.mchId || '未配置' }} · AppID {{ probeResult.appId || '未配置' }} · 回调地址
            {{ probeResult.notifyUrl || '未配置' }}
          </div>
        </el-alert>
      </el-form>
    </el-card>

    <!-- 充值记录（各医院短信账户充值流水：微信扫码 + 试用赠送） -->
    <el-card class="record-card">
      <template #header>
        <div class="card-header">
          <span>充值记录</span>
          <span class="header-hint">记录各医院短信账户充值流水（微信扫码 / 试用赠送），含微信支付账单号，可按医院与时间查询</span>
        </div>
      </template>

      <el-form :inline="true" class="query-form">
        <el-form-item label="支付医院">
          <el-select v-model="query.storeId" placeholder="全部" clearable filterable style="width: 200px">
            <el-option v-for="s in stores" :key="s.id" :label="s.storeName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="已支付" :value="1" />
            <el-option label="待支付" :value="0" />
            <el-option label="已关闭" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单号/账单号">
          <el-input
            v-model="query.keyword"
            placeholder="商户订单号或微信支付账单号"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="支付日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 250px"
            unlink-panels
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="orders" v-loading="loading" stripe scrollbar-always-on>
        <el-table-column prop="storeName" label="支付医院" min-width="180" show-overflow-tooltip />
        <el-table-column label="支付时间" width="160">
          <template #default="{ row }">{{ dateTimeOf(row.paidAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="支付金额" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'paid-amount': row.status === 1 }">￥{{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="实际收入" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'real-income': row.actualIncome > 0 }">￥{{ formatMoney(row.actualIncome) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="微信支付账单号" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.transactionId || '—' }}</template>
        </el-table-column>
        <el-table-column prop="orderNo" label="商户订单号" min-width="210" show-overflow-tooltip />
        <el-table-column label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.remark" type="success" size="small">{{ row.remark }}</el-tag>
            <el-tag v-else-if="row.mock" type="warning" size="small">模拟支付（联调）</el-tag>
            <span v-else-if="row.status === 2" class="fail-text">{{ row.tradeState || '已关闭/支付失败' }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无充值记录" :image-size="80" />
        </template>
      </el-table>

      <div class="pagination-wrapper" v-if="pagination.total > pagination.size">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchOrders"
        />
      </div>

      <div class="summary-bar">
        <span>当前查询条件下实际收入合计：<b>￥{{ formatMoney(page.actualIncome) }}</b></span>
        <span class="summary-divider" />
        <span>记录条数：<b>{{ pagination.total }}</b></span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreRecharge' })
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { payApi, storeApi } from '@/api'
import type { PayConfig, PayProbeResult, RechargeOrder } from '@/api'
import type { Store } from '@/types'

const configLoading = ref(false)
const saving = ref(false)
const probing = ref(false)
const probeResult = ref<PayProbeResult | null>(null)

const config = reactive<PayConfig & { apiV3Key: string }>({
  enabled: false,
  mchId: '',
  apiV3Key: '',
  apiV3KeyConfigured: false,
  apiV3KeyMasked: '',
  serialNo: '',
  privateKeyPath: '',
  publicKeyPath: '',
  publicKeyId: '',
  appId: '',
  notifyUrl: '',
  suggestedNotifyUrl: '',
  mockEnabled: false,
  realReady: false,
  notReadyReason: ''
})

const stores = ref<Store[]>([])
const orders = ref<RechargeOrder[]>([])
const loading = ref(false)
const query = reactive<{ storeId?: number; status?: number; keyword: string }>({
  storeId: undefined,
  status: undefined,
  keyword: ''
})
const dateRange = ref<[string, string] | null>(null)
const pagination = reactive({ page: 1, size: 20, total: 0 })
const page = reactive({ actualIncome: 0 })

const formatMoney = (value?: number) => Number(value ?? 0).toFixed(2)

const dateTimeOf = (value?: string) => {
  if (!value) return '—'
  return `${value.slice(0, 10)} ${value.slice(11, 16)}`
}

const statusLabel = (status: number) => {
  if (status === 1) return '已支付'
  if (status === 2) return '已关闭'
  return '待支付'
}

const statusTagType = (status: number) => {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'warning'
}

const fetchConfig = async () => {
  configLoading.value = true
  try {
    const data = await payApi.getConfig()
    Object.assign(config, data)
    config.apiV3Key = ''
  } finally {
    configLoading.value = false
  }
}

const fetchStores = async () => {
  stores.value = await storeApi.getAllStores({ includeDisabled: true })
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const data = await payApi.getOrders({
      storeId: query.storeId,
      status: query.status,
      keyword: query.keyword || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
      page: pagination.page,
      size: pagination.size
    })
    orders.value = data.list ?? []
    pagination.total = data.pagination?.total ?? 0
    pagination.size = data.pagination?.size ?? pagination.size
    page.actualIncome = data.actualIncome ?? 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchOrders()
}

const handleReset = () => {
  query.storeId = undefined
  query.status = undefined
  query.keyword = ''
  dateRange.value = null
  handleSearch()
}

const handleSave = async () => {
  if (config.enabled) {
    if (!config.mchId.trim()) {
      ElMessage.warning('请填写微信支付商户号')
      return
    }
    if (!config.apiV3Key.trim() && !config.apiV3KeyConfigured) {
      ElMessage.warning('请填写 APIv3 密钥')
      return
    }
    if (!config.serialNo.trim()) {
      ElMessage.warning('请填写商户证书序列号')
      return
    }
    if (!config.privateKeyPath.trim()) {
      ElMessage.warning('请填写商户API私钥路径')
      return
    }
    if (!config.publicKeyPath.trim()) {
      ElMessage.warning('请填写微信支付公钥路径')
      return
    }
  }
  saving.value = true
  try {
    const data = await payApi.saveConfig({
      enabled: config.enabled,
      mchId: config.mchId,
      apiV3Key: config.apiV3Key || undefined,
      serialNo: config.serialNo,
      privateKeyPath: config.privateKeyPath,
      publicKeyPath: config.publicKeyPath,
      publicKeyId: config.publicKeyId,
      appId: config.appId,
      notifyUrl: config.notifyUrl,
      mockEnabled: config.mockEnabled
    })
    Object.assign(config, data)
    config.apiV3Key = ''
    ElMessage.success('微信支付配置已保存')
  } finally {
    saving.value = false
  }
}

const handleProbe = async () => {
  probing.value = true
  try {
    probeResult.value = await payApi.probe()
  } finally {
    probing.value = false
  }
}

onMounted(() => {
  fetchConfig()
  fetchStores()
  fetchOrders()
})
</script>

<style scoped>
.recharge-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-hint {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}

.unit {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.warning-text {
  color: #e6a23c;
}

.field-tip {
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.field-tip b {
  color: #606266;
}

/* 操作按钮沉到左栏底部，与右栏「模拟支付（联调）」同一行，压缩卡片高度 */
.config-col {
  display: flex;
  flex-direction: column;
}

.config-actions {
  margin-top: auto;
  margin-bottom: 0;
}

.config-col--right .el-form-item:last-child {
  margin-bottom: 0;
}

.probe-result {
  margin-bottom: 4px;
}

.probe-sub {
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
}

.query-form {
  margin-bottom: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.summary-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
}

.summary-divider {
  width: 1px;
  height: 16px;
  background: #dcdfe6;
}

.paid-amount {
  color: #67c23a;
  font-weight: 600;
}

.real-income {
  color: #67c23a;
  font-weight: 600;
}

.fail-text {
  color: #f56c6c;
}
</style>
