<template>
  <div class="notify-page">
    <!-- 通知服务设置 -->
    <el-card class="setting-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">通知服务</span>
          <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
        </div>
      </template>

      <div class="setting-row">
        <span class="row-label">通知通道</span>
        <div class="switch-item">
          <span class="switch-label">手机短信通知</span>
          <el-switch v-model="config.smsEnabled" :active-value="1" :inactive-value="0" />
        </div>
        <div class="switch-item">
          <span class="switch-label">微信消息通知</span>
          <el-switch :model-value="0" disabled />
          <span class="switch-hint">暂未开放</span>
        </div>
        <span class="switch-hint">
          短信按 {{ smsUnitPrice.toFixed(2) }} 元/条计费
        </span>
      </div>

      <div class="setting-row">
        <span class="row-label">通知类型</span>
        <el-checkbox-group v-model="config.enabledTypes" class="type-group">
          <el-checkbox v-for="item in typeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </el-checkbox>
        </el-checkbox-group>
      </div>

      <div class="setting-tip">
        开启后，办理建档、预约、养护等业务时会自动通知家长；当前仅支持手机短信通知，微信消息通知暂未开放。
      </div>
    </el-card>

    <!-- 通知记录 -->
    <el-card class="record-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">通知记录</span>
          <!-- 费用汇总：查询范围和累计消费 + 可用余额 + 充值入口 -->
          <div class="fee-summary">
            <span class="fee-item">
              查询范围内费用总额：<b class="fee-num">￥{{ formatMoney(page.filterFee) }}</b>
            </span>
            <span class="fee-divider" />
            <span class="fee-item">
              累计消费：<b class="fee-num">￥{{ formatMoney(page.totalFee) }}</b>
            </span>
            <span class="fee-divider" />
            <span class="fee-item">
              可用余额：<b class="balance-num" :class="{ 'is-low': page.balance < 1 }">
                ￥{{ formatMoney(page.balance) }}
              </b>
              <el-button class="recharge-btn" type="primary" size="small" @click="openRecharge">微信充值</el-button>
              <span class="help-hint">短信余额不足将停发短信（微信消息不受影响）</span>
            </span>
          </div>
        </div>
      </template>

      <el-form :inline="true" class="query-form">
        <el-form-item label="儿童姓名" class="q-name">
          <el-input
            v-model="query.childNameLike"
            placeholder="姓名关键字"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="通知类型" class="q-type">
          <el-select v-model="query.eventType" placeholder="全部" clearable>
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道" class="q-channel">
          <el-select v-model="query.channel" placeholder="全部" clearable>
            <el-option label="手机短信" :value="1" />
            <el-option label="微信消息" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知结果" class="q-result">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知日期" class="is-daterange">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            unlink-panels
          />
        </el-form-item>
        <el-form-item class="q-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="records" v-loading="loading" stripe scrollbar-always-on>
        <el-table-column label="通知日期" width="120">
          <template #default="{ row }">{{ dateOf(row.sentAt) }}</template>
        </el-table-column>
        <el-table-column label="时间" width="95">
          <template #default="{ row }">{{ timeOf(row.sentAt) }}</template>
        </el-table-column>
        <el-table-column prop="eventLabel" label="通知类型" width="105" />
        <el-table-column prop="channelLabel" label="通知渠道" width="105" />
        <el-table-column prop="childName" label="通知对象" width="110" show-overflow-tooltip />
        <el-table-column prop="recipient" label="接收号码/账号" width="150" show-overflow-tooltip />
        <el-table-column label="通知结果" width="95">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="收费金额" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.fee) }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.status === 1">{{ row.remark || '—' }}</span>
            <span v-else class="fail-text">{{ row.failReason || '发送失败' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="通知内容" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content || '—' }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无通知记录" :image-size="80" />
        </template>
      </el-table>

      <div class="pagination-wrapper" v-if="pagination.total > pagination.size">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchRecords"
        />
      </div>
    </el-card>

    <!-- 续费弹窗（微信扫码支付） -->
    <el-dialog
      v-model="rechargeVisible"
      title="短信服务续费"
      width="460px"
      align-center
      :close-on-click-modal="false"
      @closed="handleRechargeClosed"
    >
      <div class="recharge-body" v-loading="paying">
        <div class="recharge-tip">
          短信按 <b>{{ smsUnitPrice.toFixed(2) }} 元/条</b> 计费，当前可用余额
          <b :class="{ 'is-low': page.balance < 1 }">￥{{ formatMoney(page.balance) }}</b>
        </div>

        <!-- 支付通道未就绪 -->
        <el-alert
          v-if="payStatus && !payStatus.realReady && !payStatus.mockEnabled"
          type="warning"
          :closable="false"
          show-icon
          title="微信支付暂不可用"
          :description="payStatus.notReadyReason || '请联系总部完成微信支付配置'"
        />

        <!-- 第一步：选择充值金额 -->
        <template v-if="!order">
          <div class="recharge-amount">
            <span class="amount-label">充值金额（元）</span>
            <div class="amount-options">
              <el-button
                v-for="amount in presetAmounts"
                :key="amount"
                :type="rechargeForm.amount === amount ? 'primary' : 'default'"
                size="small"
                @click="rechargeForm.amount = amount"
              >
                {{ amount }} 元
              </el-button>
              <el-input-number
                v-model="rechargeForm.amount"
                :min="1"
                :max="10000"
                :precision="0"
                :step="50"
                controls-position="right"
                size="small"
                style="width: 130px"
              />
            </div>
            <div class="amount-hint">
              金额范围 1 ~ 10000 元，按 {{ smsUnitPrice.toFixed(2) }} 元/条可发送
              <b>{{ estimatedSmsCount }}</b> 条短信
            </div>
          </div>

          <el-alert
            v-if="payStatus?.mockEnabled"
            type="info"
            :closable="false"
            show-icon
            title="当前为模拟支付环境：生成二维码后点击“模拟支付成功”即按所选金额入账。"
          />
        </template>

        <!-- 第二步：扫码支付 -->
        <template v-else>
          <div class="order-head">
            <span class="order-amount">￥{{ formatMoney(order.amount) }}</span>
            <el-tag v-if="order.status === 1" type="success">支付成功</el-tag>
            <el-tag v-else-if="order.status === 2" type="info">已关闭</el-tag>
            <el-tag v-else type="warning">等待支付</el-tag>
          </div>

          <div v-if="order.status === 0" class="qr-box">
            <canvas ref="qrCanvasRef" class="qr-canvas" width="200" height="200" />
            <div class="qr-caption">
              请使用微信扫一扫完成支付{{ order.mock ? '（模拟订单）' : '' }}
            </div>
          </div>
          <div v-else-if="order.status === 1" class="qr-box paid-box">
            <div class="paid-amount">￥{{ formatMoney(order.amount) }}</div>
            <div class="qr-caption">已充值到本院可用余额</div>
          </div>
          <div v-else class="qr-box">
            <el-empty description="订单已关闭" :image-size="70" />
          </div>

          <div class="order-meta">
            <div class="meta-row">
              <span class="meta-label">商户订单号</span>
              <span class="meta-value">{{ order.orderNo }}</span>
            </div>
            <div class="meta-row" v-if="order.status === 0 && order.expireAt">
              <span class="meta-label">支付有效期至</span>
              <span class="meta-value">{{ dateTimeOf(order.expireAt) }}</span>
            </div>
            <div class="meta-row" v-if="order.status === 1 && order.paidAt">
              <span class="meta-label">支付时间</span>
              <span class="meta-value">{{ dateTimeOf(order.paidAt) }}</span>
            </div>
            <div class="meta-row" v-if="order.status === 1 && order.transactionId">
              <span class="meta-label">微信支付账单号</span>
              <span class="meta-value">{{ order.transactionId }}</span>
            </div>
          </div>

          <div class="order-actions">
            <el-button v-if="order.status === 0" size="small" @click="resetOrder">
              <el-icon><RefreshLeft /></el-icon>
              修改金额 / 重新下单
            </el-button>
            <el-button
              v-if="order.status === 0 && payStatus?.mockEnabled"
              size="small"
              type="success"
              plain
              :loading="mockPaying"
              @click="handleMockPay"
            >
              模拟支付成功（联调）
            </el-button>
            <el-button
              v-if="order.status === 1"
              size="small"
              type="primary"
              @click="rechargeVisible = false"
            >
              完成
            </el-button>
          </div>

          <div v-if="order.status === 0" class="order-tip">
            支付成功后余额将自动到账，本页面每 3 秒自动刷新支付结果，无需手动刷新。
          </div>
        </template>
      </div>

      <template #footer>
        <el-button @click="rechargeVisible = false">关闭</el-button>
        <el-button
          v-if="!order"
          type="primary"
          :loading="paying"
          :disabled="!canCreateOrder"
          @click="handleCreateOrder"
        >
          生成付款二维码
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreNotify' })
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { notifyApi } from '@/api'
import { drawQrMatrix } from '@/utils/qrcode'
import { useUserStore } from '@/stores/user'
import type { NotifyConfig, NotifyRecord, NotifyRecordQuery, PayStatus, RechargeOrder } from '@/types'

const userStore = useUserStore()

/** 通知类型（与后端 NotifyEventTypes 编码一致） */
const typeOptions = [
  { value: 'child_created', label: '建档成功' },
  { value: 'reserve_created', label: '预约成功' },
  { value: 'reserve_cancelled', label: '预约取消' },
  { value: 'reserve_adjusted', label: '预约调整' },
  { value: 'care_reminder', label: '养护提醒' },
  { value: 'care_completed', label: '养护完成' },
  { value: 'other', label: '其它服务' }
]

const saving = ref(false)
const loading = ref(false)
const smsUnitPrice = ref(0.1)

const config = reactive<NotifyConfig>({
  smsEnabled: 0,
  wechatEnabled: 0, // 微信通道暂未开放：固定关闭，保存时也下发 0
  enabledTypes: []
})

const query = reactive<NotifyRecordQuery>({
  childNameLike: '',
  eventType: undefined,
  channel: undefined,
  status: undefined
})
const dateRange = ref<[string, string] | null>(null)

const records = ref<NotifyRecord[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })
const page = reactive({ filterFee: 0, totalFee: 0, balance: 0, totalRecharge: 0 })

const rechargeVisible = ref(false)
const presetAmounts = [50, 100, 200, 500]
const rechargeForm = reactive({ amount: 100 })
const paying = ref(false)
const mockPaying = ref(false)
const payStatus = ref<PayStatus | null>(null)
const order = ref<RechargeOrder | null>(null)
const qrCanvasRef = ref<HTMLCanvasElement>()
let pollTimer: number | undefined

/** 按当前金额可发送的短信条数（估算，用于提示） */
const estimatedSmsCount = computed(() => {
  const amount = Number(rechargeForm.amount ?? 0)
  if (!amount || amount <= 0) return 0
  return Math.floor(amount / (smsUnitPrice.value || 0.1))
})

/** 真实支付或模拟支付任一可用即可下单 */
const canCreateOrder = computed(() => !!payStatus.value?.realReady || !!payStatus.value?.mockEnabled)

const formatMoney = (value?: number) => Number(value ?? 0).toFixed(2)

const dateOf = (value?: string) => (value ? value.slice(0, 10) : '—')
const timeOf = (value?: string) => (value ? value.slice(11, 16) : '—')
const dateTimeOf = (value?: string) => (value ? `${value.slice(0, 10)} ${value.slice(11, 16)}` : '—')

const renderQr = async () => {
  const url = order.value?.codeUrl
  if (!url) return
  await nextTick()
  const canvas = qrCanvasRef.value
  if (!canvas) return
  try {
    drawQrMatrix(canvas, url, 200)
  } catch {
    ElMessage.error('二维码生成失败，请重新下单')
  }
}

const stopPolling = () => {
  if (pollTimer !== undefined) {
    window.clearInterval(pollTimer)
    pollTimer = undefined
  }
}

/** 轮询订单支付结果：成功即刷新余额与记录 */
const startPolling = () => {
  stopPolling()
  pollTimer = window.setInterval(async () => {
    const current = order.value
    if (!current || current.status !== 0) {
      stopPolling()
      return
    }
    try {
      const data = await notifyApi.getPayOrder(current.orderNo, userStore.storeId)
      order.value = data
      if (data.status === 1) {
        stopPolling()
        ElMessage.success(`充值成功 ￥${formatMoney(data.amount)}，已计入可用余额`)
        fetchRecords()
      } else if (data.status === 2) {
        stopPolling()
      }
    } catch {
      // 单次查询失败不中断轮询（网络抖动等），等待下次轮询
    }
  }, 3000)
}

const fetchConfig = async () => {
  const data = await notifyApi.getConfig(userStore.storeId)
  config.smsEnabled = data.smsEnabled ?? 0
  config.enabledTypes = data.enabledTypes ?? []
}

const fetchRecords = async () => {
  loading.value = true
  try {
    const data = await notifyApi.getRecords({
      storeId: userStore.storeId,
      childNameLike: query.childNameLike || undefined,
      eventType: query.eventType || undefined,
      channel: query.channel,
      status: query.status,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      page: pagination.page,
      size: pagination.size
    })
    records.value = data.list ?? []
    pagination.total = data.pagination?.total ?? 0
    pagination.size = data.pagination?.size ?? pagination.size
    page.filterFee = data.filterFee ?? 0
    page.totalFee = data.totalFee ?? 0
    page.balance = data.balance ?? 0
    page.totalRecharge = data.totalRecharge ?? 0
    smsUnitPrice.value = data.smsUnitPrice ?? 0.1
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchRecords()
}

const handleReset = () => {
  query.childNameLike = ''
  query.eventType = undefined
  query.channel = undefined
  query.status = undefined
  dateRange.value = null
  handleSearch()
}

const handleSave = async () => {
  saving.value = true
  try {
    const data = await notifyApi.saveConfig(
      {
        smsEnabled: config.smsEnabled,
        wechatEnabled: 0,
        enabledTypes: config.enabledTypes
      },
      userStore.storeId
    )
    config.smsEnabled = data.smsEnabled ?? 0
    config.enabledTypes = data.enabledTypes ?? []
    ElMessage.success('通知服务设置已保存')
  } finally {
    saving.value = false
  }
}

const fetchPayStatus = async () => {
  try {
    payStatus.value = await notifyApi.getPayStatus()
  } catch {
    payStatus.value = null
  }
}

const openRecharge = () => {
  rechargeForm.amount = 100
  order.value = null
  rechargeVisible.value = true
  fetchPayStatus()
}

const resetOrder = () => {
  stopPolling()
  order.value = null
}

const handleRechargeClosed = () => {
  stopPolling()
  order.value = null
  mockPaying.value = false
}

const handleCreateOrder = async () => {
  const amount = Number(rechargeForm.amount ?? 0)
  if (!amount || amount < 1 || amount > 10000) {
    ElMessage.warning('充值金额需在 1 ~ 10000 元之间')
    return
  }
  paying.value = true
  try {
    const data = await notifyApi.createPayOrder(amount, userStore.storeId)
    order.value = data
    if (data.codeUrl) {
      await renderQr()
    }
    if (data.status === 0) {
      startPolling()
    }
  } finally {
    paying.value = false
  }
}

const handleMockPay = async () => {
  const current = order.value
  if (!current) return
  mockPaying.value = true
  try {
    const data = await notifyApi.mockPayOrder(current.orderNo, userStore.storeId)
    order.value = data
    if (data.status === 1) {
      stopPolling()
      ElMessage.success(`模拟支付成功 ￥${formatMoney(data.amount)}，已计入可用余额`)
      fetchRecords()
    }
  } finally {
    mockPaying.value = false
  }
}

onMounted(() => {
  fetchConfig()
  fetchRecords()
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.notify-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.card-title {
  flex: 0 0 auto;
  font-size: 16px;
  font-weight: 600;
}

.setting-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
  padding: 6px 0;
}

.row-label {
  width: 72px;
  color: #606266;
  font-size: 14px;
}

.switch-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.switch-label {
  font-size: 14px;
  color: #303133;
}

.switch-hint {
  font-size: 12px;
  color: #909399;
}

.type-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
}

.setting-tip {
  margin-top: 10px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

/* 查询条件压成一行：横向 flex（不换行）+ 收紧默认 32px 间距，控件按比例伸缩，日期吃满剩余宽度 */
.query-form {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  margin-bottom: 16px;
}

.query-form :deep(.el-form-item) {
  margin-right: 12px;
  margin-bottom: 0;
  min-width: 0;
}

.query-form :deep(.el-form-item:last-child) {
  margin-right: 0;
  flex: 0 0 auto;
}

.query-form :deep(.el-form-item__content) {
  min-width: 0;
}

.query-form :deep(.el-input),
.query-form :deep(.el-select),
.query-form :deep(.el-date-editor) {
  width: 100%;
}

.query-form :deep(.q-name) { flex: 0 1 196px; }
.query-form :deep(.q-type) { flex: 0 1 186px; }
.query-form :deep(.q-channel) { flex: 0 1 186px; }
.query-form :deep(.q-result) { flex: 0 1 168px; }
.query-form :deep(.is-daterange) { flex: 1 1 258px; max-width: 320px; }

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

/* 费用汇总随标题排在卡片头部右侧：收紧内边距，右对齐，空间不足时整体换行 */
.fee-summary {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 4px 10px;
  min-width: 0;
  padding: 4px 10px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #303133;
}

.fee-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.fee-divider {
  width: 1px;
  height: 16px;
  background: #dcdfe6;
}

.fee-num {
  color: #303133;
}

.balance-num {
  color: #67c23a;
}

.balance-num.is-low {
  color: #f56c6c;
}

.recharge-btn {
  margin-left: 2px;
}

.help-hint {
  font-size: 12px;
  color: #909399;
}

.fail-text {
  color: #f56c6c;
}

.recharge-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.recharge-tip {
  font-size: 13px;
  color: #606266;
}

.recharge-tip .is-low {
  color: #f56c6c;
}

.qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.qr-canvas {
  width: 200px;
  height: 200px;
  padding: 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.qr-caption {
  font-size: 12px;
  color: #909399;
}

.paid-box {
  padding: 10px 0;
}

.paid-amount {
  font-size: 30px;
  font-weight: 700;
  color: #67c23a;
}

.amount-label {
  font-size: 13px;
  color: #606266;
}

.amount-options {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.amount-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.order-amount {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.order-meta {
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 2;
}

.meta-row {
  display: flex;
  gap: 8px;
}

.meta-label {
  flex: none;
  width: 96px;
  color: #909399;
}

.meta-value {
  flex: 1;
  color: #303133;
  word-break: break-all;
}

.order-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.order-tip {
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
  text-align: center;
}
</style>
