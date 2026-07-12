<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6f7ff;">
              <el-icon :size="28" color="#1890ff"><Calendar /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayReserves }}</div>
              <div class="stat-label">今日预约</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #fff7e6;">
              <el-icon :size="28" color="#faad14"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingChildren }}</div>
              <div class="stat-label">待审核档案</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f6ffed;">
              <el-icon :size="28" color="#52c41a"><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.activeDevices }}</div>
              <div class="stat-label">在线设备</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f9f0ff;">
              <el-icon :size="28" color="#722ed1"><View /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayTests }}</div>
              <div class="stat-label">今日检测</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>预约/检测趋势</span>
              <el-radio-group v-model="trendPeriod" size="small" @change="fetchWeeklyTrend">
                <el-radio-button label="week">近7天</el-radio-button>
                <el-radio-button label="month">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>
            <span>视力检测统计</span>
          </template>
          <div ref="visionChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近预约 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>最近预约</span>
          <el-button type="primary" text @click="$router.push('/schedule')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentReserves" v-loading="reserveLoading" stripe>
        <el-table-column prop="scheduleDate" label="日期" width="120" />
        <el-table-column label="时段" width="120">
          <template #default="{ row }">{{ row.timeSlotStart }}-{{ row.timeSlotEnd }}</template>
        </el-table-column>
        <el-table-column prop="childName" label="儿童姓名" width="100" />
        <el-table-column prop="parentPhone" label="家长电话" width="130" />
        <el-table-column prop="technicianName" label="技师" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getReserveStatusType(row.status)" size="small">
              {{ getReserveStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreDashboard' })
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Calendar, User, Monitor, View } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { Reserve, DashboardStats } from '@/types'
import { statisticsApi, reserveApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ==================== 统计数据 ====================
const stats = reactive<DashboardStats>({
  todayReserves: 0,
  pendingChildren: 0,
  activeDevices: 0,
  todayTests: 0
})

const fetchDashboardStats = async () => {
  if (!userStore.storeId) return
  try {
    const res = await statisticsApi.getDashboardStats(userStore.storeId)
    Object.assign(stats, res)
  } catch {
    // 错误已在拦截器处理
  }
}

// ==================== 趋势图 ====================
const trendPeriod = ref('week')
const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

const fetchWeeklyTrend = async () => {
  if (!userStore.storeId) return
  try {
    const res = await statisticsApi.getWeeklyTrend({
      storeId: userStore.storeId,
      period: trendPeriod.value as 'week' | 'month'
    })
    renderTrendChart(res.dates, res.reserveCounts, res.testCounts)
  } catch {
    // 使用空数据渲染
    renderTrendChart([], [], [])
  }
}

const renderTrendChart = (dates: string[], reserveCounts: number[], testCounts: number[]) => {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['预约数', '检测数'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates.length > 0 ? dates : ['暂无数据'],
      axisLabel: {
        formatter: (val: string) => val.substring(5) // MM-DD
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '预约数',
        type: 'line',
        smooth: true,
        data: reserveCounts.length > 0 ? reserveCounts : [0],
        itemStyle: { color: '#1890ff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24,144,255,0.3)' },
            { offset: 1, color: 'rgba(24,144,255,0.05)' }
          ])
        }
      },
      {
        name: '检测数',
        type: 'line',
        smooth: true,
        data: testCounts.length > 0 ? testCounts : [0],
        itemStyle: { color: '#722ed1' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(114,46,209,0.3)' },
            { offset: 1, color: 'rgba(114,46,209,0.05)' }
          ])
        }
      }
    ]
  })
}

// ==================== 视力统计图 ====================
const visionChartRef = ref<HTMLElement>()
let visionChart: echarts.ECharts | null = null

const fetchVisionStatistics = async () => {
  if (!userStore.storeId) return
  try {
    const now = new Date()
    const startDate = new Date(now.getFullYear(), now.getMonth(), 1)
    const formatDate = (d: Date): string => {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    }

    const res = await statisticsApi.getVisionStatistics({
      storeId: userStore.storeId,
      startDate: formatDate(startDate),
      endDate: formatDate(now)
    })
    renderVisionChart(res.labels, res.beforeValues, res.afterValues)
  } catch {
    renderVisionChart([], [], [])
  }
}

const renderVisionChart = (labels: string[], beforeValues: number[], afterValues: number[]) => {
  if (!visionChartRef.value) return
  if (!visionChart) {
    visionChart = echarts.init(visionChartRef.value)
  }
  visionChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['养护前', '养护后'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: labels.length > 0 ? labels : ['暂无数据']
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '养护前',
        type: 'bar',
        data: beforeValues.length > 0 ? beforeValues : [0],
        itemStyle: { color: '#faad14', borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 30
      },
      {
        name: '养护后',
        type: 'bar',
        data: afterValues.length > 0 ? afterValues : [0],
        itemStyle: { color: '#52c41a', borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 30
      }
    ]
  })
}

// ==================== 最近预约 ====================
const reserveLoading = ref(false)
const recentReserves = ref<Reserve[]>([])

const getReserveStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] || 'info'
}

const getReserveStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '待服务', 1: '已完成', 2: '已取消' }
  return map[status] || '未知'
}

const fetchRecentReserves = async () => {
  reserveLoading.value = true
  try {
    const res = await reserveApi.getReserveList({
      storeId: userStore.storeId,
      page: 1,
      size: 5
    })
    recentReserves.value = res.list
  } catch {
    // 错误已在拦截器处理
  } finally {
    reserveLoading.value = false
  }
}

// ==================== 窗口resize ====================
const handleResize = () => {
  trendChart?.resize()
  visionChart?.resize()
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await nextTick()
  fetchDashboardStats()
  fetchWeeklyTrend()
  fetchVisionStatistics()
  fetchRecentReserves()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  visionChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  .stat-cards {
    .stat-card {
      .stat-content {
        display: flex;
        align-items: center;
        gap: 16px;

        .stat-icon {
          width: 56px;
          height: 56px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .stat-info {
          .stat-value {
            font-size: 28px;
            font-weight: bold;
            color: #333;
            line-height: 1.2;
          }

          .stat-label {
            font-size: 14px;
            color: #999;
            margin-top: 4px;
          }
        }
      }
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .chart-container {
    height: 300px;
    width: 100%;
  }
}
</style>
