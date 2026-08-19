<template>
  <div class="statistics-page">
    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="所属医院">
          <el-select v-model="filterForm.storeId" placeholder="全部医院" clearable filterable>
            <el-option
              v-for="item in storeOptions"
              :key="item.id"
              :label="item.storeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
        <el-form-item class="export-btns">
          <el-button type="success" @click="handleExport('excel')">
            <el-icon><Download /></el-icon>导出Excel
          </el-button>
          <el-button type="warning" @click="handleExport('csv')">
            <el-icon><Download /></el-icon>导出CSV
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 概览卡片 -->
    <el-row :gutter="20" class="overview-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #ecf5ff">
            <el-icon :size="28" color="#409eff"><Shop /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summaryData.storeCount }}</div>
            <div class="stat-label">医院总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #f0f9eb">
            <el-icon :size="28" color="#67c23a"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summaryData.totalChildren }}</div>
            <div class="stat-label">儿童总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #fdf6ec">
            <el-icon :size="28" color="#e6a23c"><View /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summaryData.monthlyVisits }}</div>
            <div class="stat-label">本月检测</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #fef0f0">
            <el-icon :size="28" color="#f56c6c"><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summaryData.avgImprovement }}</div>
            <div class="stat-label">平均改善</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>月度视力检测趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-container" v-loading="trendLoading" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>医院客流对比</span>
          </template>
          <div ref="trafficChartRef" class="chart-container" v-loading="trafficLoading" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>视力等级分布</span>
          </template>
          <div ref="pieChartRef" class="chart-container" v-loading="pieLoading" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>优秀医院排名</span>
          </template>
          <el-table :data="summaryData.topStores" stripe size="small" max-height="350">
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="storeName" label="医院名称" />
            <el-table-column prop="visitCount" label="到店次数" width="100" />
            <el-table-column prop="improvementRate" label="改善率" width="100">
              <template #default="{ row }">
                <span style="color: #67c23a">{{ row.improvementRate }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminStatistics' })
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download, Shop, User, View, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { statisticsApi, storeApi } from '@/api'
import type { NationalSummary, Store } from '@/types'

// 筛选条件
const storeOptions = ref<Store[]>([])
const dateRange = ref<[string, string] | null>(null)
const filterForm = reactive({
  storeId: undefined as number | undefined,
  startDate: '',
  endDate: ''
})

const dateShortcuts = [
  {
    text: '最近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  },
  {
    text: '最近90天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
      return [start, end]
    }
  }
]

// 概览数据
const summaryData = reactive<NationalSummary>({
  storeCount: 0,
  totalChildren: 0,
  monthlyVisits: 0,
  avgImprovement: '0%',
  topStores: []
})

// 图表相关
const trendChartRef = ref<HTMLDivElement>()
const trafficChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let trafficChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
const trendLoading = ref(false)
const trafficLoading = ref(false)
const pieLoading = ref(false)

// 获取医院列表
const fetchStores = async () => {
  try {
    const res = await storeApi.getAllStores()
    storeOptions.value = res
  } catch (error) {
    console.error('获取医院列表失败', error)
  }
}

// 获取全国汇总
const fetchSummary = async () => {
  try {
    const res = await statisticsApi.getNationalSummary({
      startDate: filterForm.startDate,
      endDate: filterForm.endDate
    })
    Object.assign(summaryData, res)
  } catch (error) {
    console.error('获取汇总数据失败', error)
  }
}

// 获取医院客流数据并渲染柱状图
const fetchTrafficChart = async () => {
  if (!trafficChartRef.value) return
  trafficLoading.value = true
  try {
    if (!trafficChart) {
      trafficChart = echarts.init(trafficChartRef.value)
    }
    // 使用已有医院数据作为演示
    const stores = storeOptions.value.slice(0, 8)
    const mockData = stores.map(() => Math.floor(Math.random() * 500 + 100))

    trafficChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: stores.map(s => s.storeName),
        axisLabel: { rotate: 30, fontSize: 11 }
      },
      yAxis: { type: 'value', name: '到店次数' },
      series: [{
        name: '到店次数',
        type: 'bar',
        data: mockData,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#79bbff' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      }],
      grid: { left: 50, right: 20, top: 30, bottom: 60 }
    })
  } finally {
    trafficLoading.value = false
  }
}

// 渲染趋势图
const fetchTrendChart = async () => {
  if (!trendChartRef.value) return
  trendLoading.value = true
  try {
    if (!trendChart) {
      trendChart = echarts.init(trendChartRef.value)
    }
    const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
    const mockData = [320, 302, 341, 374, 390, 450, 420, 480, 530, 490, 550, 600]

    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: months },
      yAxis: { type: 'value', name: '检测次数' },
      series: [{
        name: '检测次数',
        type: 'line',
        data: mockData,
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        },
        lineStyle: { color: '#409eff', width: 2 },
        itemStyle: { color: '#409eff' }
      }],
      grid: { left: 50, right: 20, top: 30, bottom: 30 }
    })
  } finally {
    trendLoading.value = false
  }
}

// 渲染饼图
const fetchPieChart = async () => {
  if (!pieChartRef.value) return
  pieLoading.value = true
  try {
    if (!pieChart) {
      pieChart = echarts.init(pieChartRef.value)
    }
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left', top: 'center' },
      series: [{
        name: '视力等级',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%' },
        data: [
          { value: 335, name: '正常(5.0+)', itemStyle: { color: '#67c23a' } },
          { value: 310, name: '轻度(4.7-4.9)', itemStyle: { color: '#409eff' } },
          { value: 234, name: '中度(4.3-4.6)', itemStyle: { color: '#e6a23c' } },
          { value: 135, name: '重度(<4.3)', itemStyle: { color: '#f56c6c' } }
        ]
      }]
    })
  } finally {
    pieLoading.value = false
  }
}

// 查询
const handleQuery = () => {
  if (dateRange.value) {
    filterForm.startDate = dateRange.value[0]
    filterForm.endDate = dateRange.value[1]
  } else {
    filterForm.startDate = ''
    filterForm.endDate = ''
  }
  fetchSummary()
  fetchTrendChart()
  fetchTrafficChart()
  fetchPieChart()
}

// 重置
const handleReset = () => {
  filterForm.storeId = undefined
  dateRange.value = null
  filterForm.startDate = ''
  filterForm.endDate = ''
  fetchSummary()
  fetchTrendChart()
  fetchTrafficChart()
  fetchPieChart()
}

// 导出
const handleExport = async (format: 'excel' | 'csv') => {
  try {
    const res = await statisticsApi.exportData({
      exportType: 'all',
      storeId: filterForm.storeId,
      startDate: filterForm.startDate,
      endDate: filterForm.endDate,
      format
    })
    if (res.downloadUrl) {
      window.open(res.downloadUrl, '_blank')
      ElMessage.success('导出成功')
    }
  } catch {
    ElMessage.error('导出失败')
  }
}

// 窗口大小变化时重绘图表
const handleResize = () => {
  trendChart?.resize()
  trafficChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  await fetchStores()
  // 默认最近30天
  const end = new Date()
  const start = new Date()
  start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
  const fmt = (d: Date) => d.toISOString().slice(0, 10)
  filterForm.startDate = fmt(start)
  filterForm.endDate = fmt(end)
  dateRange.value = [fmt(start), fmt(end)]

  await fetchSummary()
  await nextTick()
  fetchTrendChart()
  fetchTrafficChart()
  fetchPieChart()

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  trafficChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped lang="scss">
.statistics-page {
  .filter-card {
    margin-bottom: 20px;

    .export-btns {
      float: right;
    }
  }

  .overview-cards {
    margin-bottom: 20px;

    .stat-card {
      :deep(.el-card__body) {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 20px;
      }

      .stat-icon {
        width: 56px;
        height: 56px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      .stat-info {
        .stat-value {
          font-size: 24px;
          font-weight: 600;
          color: #303133;
          line-height: 1.2;
        }

        .stat-label {
          font-size: 13px;
          color: #909399;
          margin-top: 4px;
        }
      }
    }
  }

  .chart-row {
    margin-bottom: 20px;

    .chart-container {
      height: 350px;
      width: 100%;
    }
  }
}
</style>
