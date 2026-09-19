<template>
  <div class="care-record-page">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="儿童姓名">
          <el-input
            v-model="queryForm.childName"
            placeholder="儿童姓名"
            clearable
            style="width: 140px;"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="家长姓名">
          <el-input
            v-model="queryForm.parentName"
            placeholder="家长姓名"
            clearable
            style="width: 140px;"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="手机号码">
          <el-input
            v-model="queryForm.phone"
            placeholder="支持完整号码或片段"
            clearable
            maxlength="11"
            style="width: 170px;"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="养护次数">
          <span class="care-count-filter">大于</span>
          <el-input-number
            v-model="queryForm.minCareCount"
            :min="0"
            :controls="false"
            placeholder="不限"
            style="width: 80px;"
          />
          <span class="care-count-filter">次</span>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>

      <!-- 列表 -->
      <el-table :data="recordList" v-loading="loading" stripe empty-text="暂无养护记录" scrollbar-always-on>
        <el-table-column label="养护日期" width="105">
          <template #default="{ row }">{{ row.careDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="养护时段" width="118">
          <template #default="{ row }">{{ row.timeSlot || '-' }}</template>
        </el-table-column>
        <el-table-column label="儿童姓名" width="95">
          <template #default="{ row }">{{ row.childName || `儿童#${row.childId}` }}</template>
        </el-table-column>
        <el-table-column label="性别" width="55" align="center">
          <template #default="{ row }">{{ row.childGender === 1 ? '男' : row.childGender === 0 ? '女' : '-' }}</template>
        </el-table-column>
        <el-table-column label="手机号码" width="115" align="center">
          <template #default="{ row }">{{ row.childPhone || '-' }}</template>
        </el-table-column>
        <el-table-column label="养护次数" width="82" align="center">
          <template #default="{ row }">{{ row.careCount ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="可用次数" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="(row.remainingCount || 0) > 0 ? 'success' : 'info'" size="small">
              {{ row.remainingCount ?? 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="养护人" width="85">
          <template #default="{ row }">{{ row.executorName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="75" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : 'warning'" size="small">
              {{ row.status === 2 ? '已完成' : '养护中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="首次视力" width="82" align="center">
          <template #default="{ row }">{{ displayVision(row.nakedVisionBoth) || '-' }}</template>
        </el-table-column>
        <el-table-column label="当前视力" width="82" align="center">
          <template #default="{ row }">{{ displayVision(row.visionAfterBoth) || '-' }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="85" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗（横屏大尺寸：上部档案信息 + 下部视力趋势） -->
    <el-dialog
      v-model="detailVisible"
      title="养护记录详情"
      width="92%"
      top="4vh"
      class="care-detail-dialog"
      destroy-on-close
      @closed="handleDetailClosed"
    >
      <div v-loading="detailLoading">
        <!-- 上部：儿童基本信息（与儿童档案相同，只读） -->
        <div class="detail-section-title">
          儿童基本信息[档案编号：{{ childInfo?.childCode || '-' }}]
        </div>
        <el-descriptions v-if="childInfo" :column="4" border size="small" label-width="96px">
          <el-descriptions-item label="儿童姓名">{{ childInfo.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ childInfo.gender === 1 ? '男' : '女' }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">
            {{ childInfo.birthDate ? `${childInfo.birthDate}（${childInfo.age}岁）` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="所在学校">{{ childInfo.school || '-' }}</el-descriptions-item>
          <el-descriptions-item label="家长姓名">{{ childInfo.parentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关系">{{ childInfo.relation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号码">{{ childInfo.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="家庭地址">{{ childInfo.homeAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="视力状况" :span="2">{{ childInfo.eyeCondition || '-' }}</el-descriptions-item>
          <el-descriptions-item label="裸眼视力" :span="2">
            双眼 {{ displayVision(childInfo.nakedVisionBoth) || '-' }}　左眼 {{ displayVision(childInfo.nakedVisionLeft) || '-' }}　右眼
            {{ displayVision(childInfo.nakedVisionRight) || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="主治医师">{{ childInfo.doctorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="累计养护次数">{{ detailRow?.careCount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="可用次数">{{ childInfo.remainingCount ?? 0 }}</el-descriptions-item>
        </el-descriptions>

        <!-- 下部：视力趋势图（每次养护一组：双眼/左眼/右眼 × 养护前后） -->
        <div class="detail-section-title">养护视力趋势</div>
        <div v-if="chartRecords.length > 0" class="trend-scroll" ref="trendScrollRef">
          <div class="trend-chart" ref="trendChartRef"></div>
        </div>
        <el-empty v-if="!detailLoading && chartRecords.length === 0" description="暂无养护记录" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreCareRecord' })
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import type { CareRecord, Child } from '@/types'
import { careRecordApi, childApi } from '@/api'

const route = useRoute()

const loading = ref(false)
const recordList = ref<CareRecord[]>([])

const queryForm = reactive({
  childId: undefined as number | undefined,
  childName: '',
  parentName: '',
  phone: '',
  minCareCount: undefined as number | undefined
})
const pagination = reactive({ page: 1, size: 20, total: 0 })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await careRecordApi.getRecordPage({
      childId: queryForm.childId,
      childName: queryForm.childName || undefined,
      parentName: queryForm.parentName || undefined,
      phone: queryForm.phone || undefined,
      minCareCount: queryForm.minCareCount,
      page: pagination.page,
      size: pagination.size
    })
    recordList.value = res.list
    pagination.total = res.pagination.total
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.childId = undefined
  queryForm.childName = ''
  queryForm.parentName = ''
  queryForm.phone = ''
  queryForm.minCareCount = undefined
  pagination.page = 1
  fetchData()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  fetchData()
}

// ==================== 详情弹窗 ====================
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRow = ref<CareRecord | null>(null)
const childInfo = ref<Child | null>(null)
const chartRecords = ref<CareRecord[]>([])
const trendScrollRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null

/** 视力值解析：'5.0+0' → 主值 5.0；无法解析返回 null */
const visionMain = (v?: string): number | null => {
  if (!v) return null
  const n = parseFloat(v)
  return Number.isNaN(n) ? null : n
}

/** 视力值展示：去掉结尾的 +0（如 '5.0+0' 显示为 '5.0'） */
const displayVision = (v?: string): string => (v || '').replace(/\+0$/, '')

/** 六个数据系列（前浅后深：双眼蓝、左眼橙、右眼绿） */
const SERIES_DEFS = [
  { name: '养护前·双眼', key: 'visionBeforeBoth', color: '#a0cfff' },
  { name: '养护后·双眼', key: 'visionAfterBoth', color: '#409eff' },
  { name: '养护前·左眼', key: 'visionBeforeLeft', color: '#f3d19e' },
  { name: '养护后·左眼', key: 'visionAfterLeft', color: '#e6a23c' },
  { name: '养护前·右眼', key: 'visionBeforeRight', color: '#b3e19d' },
  { name: '养护后·右眼', key: 'visionAfterRight', color: '#67c23a' }
] as const

/** 每次养护的绘图列宽（px）：X 轴三行标注（日期/时段/养护人）需要足够宽度 */
const CHART_CELL_WIDTH = 150

const renderTrendChart = () => {
  if (!trendChartRef.value || chartRecords.value.length === 0) return
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }

  const records = chartRecords.value
  // 绘制全部数据：图表内容宽度随记录数增长，外层容器横向滚动
  const containerWidth = trendScrollRef.value?.clientWidth || 900
  trendChartRef.value.style.width = `${Math.max(containerWidth, records.length * CHART_CELL_WIDTH)}px`
  trendChartRef.value.style.height = '440px'
  trendChart = echarts.init(trendChartRef.value)

  const series = SERIES_DEFS.map((def) => ({
    name: def.name,
    type: 'bar' as const,
    barMaxWidth: 14,
    itemStyle: { color: def.color },
    label: {
      show: true,
      position: 'top' as const,
      fontSize: 10,
      color: '#606266',
      formatter: (p: { data?: { full?: string } }) => p.data?.full || ''
    },
    data: records.map((r) => {
      const raw = (r as unknown as Record<string, string | undefined>)[def.key]
      const main = visionMain(raw)
      return main == null ? null : { value: main, full: displayVision(raw) }
    })
  }))

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: Array<{ dataIndex: number }>) => {
        const idx = params[0]?.dataIndex ?? 0
        const r = records[idx]
        if (!r) return ''
        const rows = SERIES_DEFS.map(
          (d) => `${d.name}：${displayVision((r as unknown as Record<string, string | undefined>)[d.key]) || '-'}`
        ).join('<br/>')
        return `<b>${r.careDate} ${r.timeSlot || ''}</b><br/>养护人：${r.executorName || '-'}<br/>${rows}`
      }
    },
    legend: { top: 0, itemWidth: 12, itemHeight: 8, textStyle: { fontSize: 12 }, selectedMode: false },
    grid: { left: 45, right: 20, top: 40, bottom: 92 },
    xAxis: {
      type: 'category',
      data: records.map((r) => r.careDate),
      axisTick: { alignWithLabel: true },
      axisLabel: {
        interval: 0,
        fontSize: 11,
        lineHeight: 16,
        color: '#606266',
        formatter: (_val: string, idx: number) => {
          const r = records[idx]
          if (!r) return ''
          return `${r.careDate}\n${r.timeSlot || '-'}\n${r.executorName || '-'}`
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '视力',
      min: (value: { min: number }) => Math.max(0, Math.floor((value.min - 0.2) * 10) / 10),
      max: 5.3,
      axisLabel: { formatter: (val: number) => val.toFixed(1) }
    },
    series
  })
}

const openDetail = async (row: CareRecord) => {
  detailVisible.value = true
  detailLoading.value = true
  detailRow.value = row
  childInfo.value = null
  chartRecords.value = []
  try {
    const [child, records] = await Promise.all([
      childApi.getChildDetail(row.childId),
      careRecordApi.getRecordsByChild(row.childId, row.storeId)
    ])
    childInfo.value = child
    // 接口按日期倒序返回，图表按时间升序（第 1 次在最左）
    chartRecords.value = [...records].sort((a, b) =>
      a.careDate === b.careDate ? a.id - b.id : a.careDate.localeCompare(b.careDate)
    )
  } catch {
    // 错误已在拦截器处理
  } finally {
    detailLoading.value = false
  }
  await nextTick()
  renderTrendChart()
}

const handleDetailClosed = () => {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
}

const handleResize = () => {
  if (trendChart) {
    trendChart.resize()
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  // 支持从档案列表跳转带入 childId
  const childId = Number(route.query.childId)
  if (childId > 0) {
    queryForm.childId = childId
  }
  fetchData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style scoped lang="scss">
.care-record-page {
  .care-count-filter {
    color: #606266;
    font-size: 14px;
    margin: 0 4px;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .detail-section-title {
    font-size: 14px;
    font-weight: bold;
    margin: 4px 0 12px;
    padding-left: 8px;
    border-left: 3px solid var(--el-color-primary);
  }

  .trend-scroll {
    margin-top: 4px;
    overflow-x: auto;
    overflow-y: hidden;
    border: 1px solid #ebeef5;
    border-radius: 6px;
    padding: 8px 0;
  }

  .trend-chart {
    height: 440px;
  }
}

:deep(.care-detail-dialog) {
  max-width: 1400px;
  margin: 0 auto;

  .el-dialog__body {
    max-height: 80vh;
    overflow-y: auto;
    padding-top: 8px;
  }

  /* 儿童基本信息各列平均分配宽度 */
  .el-descriptions__table {
    table-layout: fixed;
  }
}
</style>
