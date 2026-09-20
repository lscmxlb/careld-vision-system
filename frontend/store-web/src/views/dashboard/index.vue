<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #fff7e6;">
              <el-icon :size="28" color="#faad14"><User /></el-icon>
            </div>
            <div class="stat-metrics">
              <div class="metric">
                <div class="stat-value">{{ stats.monthChildCount }}</div>
                <div class="stat-label">本月新增</div>
              </div>
              <div class="metric">
                <div class="stat-value">{{ stats.totalChildCount }}</div>
                <div class="stat-label">档案总数</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6f7ff;">
              <el-icon :size="28" color="#1890ff"><Calendar /></el-icon>
            </div>
            <div class="stat-metrics">
              <div class="metric">
                <div class="stat-value">{{ stats.monthReserveCount }}</div>
                <div class="stat-label">本月预约</div>
              </div>
              <div class="metric">
                <div class="stat-value">{{ stats.totalReserveCount }}</div>
                <div class="stat-label">总预约数量</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f6ffed;">
              <el-icon :size="28" color="#52c41a"><Monitor /></el-icon>
            </div>
            <div class="stat-metrics">
              <div class="metric">
                <div class="stat-value">{{ stats.monthCareCount }}</div>
                <div class="stat-label">本月养护</div>
              </div>
              <div class="metric">
                <div class="stat-value">{{ stats.totalCareCount }}</div>
                <div class="stat-label">总养护数量</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
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
    </el-row>

    <!-- 最近预约 / 养护记录 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近预约</span>
              <el-button type="primary" text @click="$router.push('/schedule')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentReserves" v-loading="reserveLoading" stripe scrollbar-always-on>
            <el-table-column prop="scheduleDate" label="日期" min-width="105" />
            <el-table-column label="时段" min-width="110">
              <template #default="{ row }">{{ formatHm(row.timeSlotStart) }}-{{ formatHm(row.timeSlotEnd) }}</template>
            </el-table-column>
            <el-table-column prop="childName" label="儿童姓名" min-width="90" />
            <el-table-column label="性别" min-width="56" align="center">
              <template #default="{ row }">
                {{ row.childGender === 1 ? '男' : row.childGender === 0 ? '女' : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="年龄" min-width="56" align="center">
              <template #default="{ row }">{{ row.childAge ?? '-' }}</template>
            </el-table-column>
            <el-table-column prop="parentPhone" label="家长电话" min-width="120" />
            <el-table-column label="状态" min-width="90">
              <template #default="{ row }">
                <el-tag v-if="row.noShowFlag === 1" type="danger" size="small">已爽约</el-tag>
                <el-tag v-else :type="getReserveStatusType(row.status)" size="small">
                  {{ getReserveStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>养护记录</span>
              <el-button type="primary" text @click="$router.push('/care-record')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentCareRecords" v-loading="careLoading" stripe scrollbar-always-on>
            <el-table-column label="日期" width="120">
              <template #default="{ row }">{{ row.careDate || '-' }}</template>
            </el-table-column>
            <el-table-column label="时段" width="120">
              <template #default="{ row }">{{ row.timeSlot || '-' }}</template>
            </el-table-column>
            <el-table-column label="儿童姓名" width="100">
              <template #default="{ row }">{{ row.childName || `儿童#${row.childId}` }}</template>
            </el-table-column>
            <el-table-column label="手机号码" width="130">
              <template #default="{ row }">{{ row.childPhone || '-' }}</template>
            </el-table-column>
            <el-table-column label="养护人" width="100">
              <template #default="{ row }">{{ row.executorName || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 2 ? 'success' : 'warning'" size="small">
                  {{ row.status === 2 ? '已完成' : '养护中' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreDashboard' })
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Calendar, User, Monitor } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { Reserve, CareRecord, DashboardStats } from '@/types'
import { statisticsApi, reserveApi, careRecordApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ==================== 统计数据 ====================
const stats = reactive<DashboardStats>({
  todayReserves: 0,
  pendingChildren: 0,
  activeDevices: 0,
  todayTests: 0,
  monthReserveCount: 0,
  totalReserveCount: 0,
  monthChildCount: 0,
  totalChildCount: 0,
  monthCareCount: 0,
  totalCareCount: 0
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

// ==================== 最近预约 ====================
const reserveLoading = ref(false)
const recentReserves = ref<Reserve[]>([])

const getReserveStatusType = (status: number) => {
  const map: Record<number, string> = { 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' }
  return map[status] || 'info'
}

const getReserveStatusText = (status: number) => {
  const map: Record<number, string> = { 1: '已预约', 2: '养护中', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

/** 时段显示统一为 HH:mm（后端返回 HH:mm:ss 时去掉秒） */
const formatHm = (t?: string) => (t ? t.slice(0, 5) : '')

const fetchRecentReserves = async () => {
  reserveLoading.value = true
  try {
    const res = await reserveApi.getReserveList({
      storeId: userStore.storeId,
      page: 1,
      size: 5,
      orderDesc: true
    })
    recentReserves.value = res.list
  } catch {
    // 错误已在拦截器处理
  } finally {
    reserveLoading.value = false
  }
}

// ==================== 养护记录 ====================
const careLoading = ref(false)
const recentCareRecords = ref<CareRecord[]>([])

const fetchRecentCareRecords = async () => {
  careLoading.value = true
  try {
    const res = await careRecordApi.getRecordPage({
      storeId: userStore.storeId,
      page: 1,
      size: 5
    })
    recentCareRecords.value = res.list
  } catch {
    // 错误已在拦截器处理
  } finally {
    careLoading.value = false
  }
}

// ==================== 窗口resize ====================
const handleResize = () => {
  trendChart?.resize()
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await nextTick()
  fetchDashboardStats()
  fetchWeeklyTrend()
  fetchRecentReserves()
  fetchRecentCareRecords()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
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

        /* 双指标：每个指标数字在上、文字在下 */
        .stat-metrics {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: space-evenly;

          .metric {
            text-align: center;

            & + .metric {
              border-left: 1px solid #f0f0f0;
              padding-left: 24px;
            }

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
