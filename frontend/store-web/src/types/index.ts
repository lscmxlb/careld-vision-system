/**
 * Careld Vision System - 医院Web端类型定义
 */

// ==================== 通用响应 ====================
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

// ==================== 用户相关 ====================
export interface User {
  id: number
  username: string
  realName: string
  phone: string
  userType: number
  storeId?: number
  storeName?: string
  status: number
  roles: string[]
  permissions: string[]
}

export interface LoginRequest {
  username?: string
  phone?: string
  password: string
  loginType: number // 1=用户名 2=手机号
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
  user: User
}

// ==================== 医院相关 ====================
export interface Store {
  id: number
  storeCode: string
  storeName: string
  address: string
  contactName: string
  contactPhone: string
  businessHours: string
}

// ==================== 儿童档案相关 ====================
export interface Child {
  id: number
  childCode: string
  name: string
  phone: string
  birthDate: string
  age: number
  gender: number
  eyeCondition: string
  medicalHistory?: string
  allergyInfo?: string
  familyHistory?: string
  auditStatus: number // 0=待审核 1=已通过 2=已驳回
  auditRemark?: string
  storeId?: number
  createdAt: string
}

export interface ChildQuery {
  page?: number
  size?: number
  storeId?: number
  auditStatus?: number
  keyword?: string
}

export interface CreateChildRequest {
  name: string
  phone: string
  birthDate: string
  gender: number
  eyeCondition: string
  medicalHistory?: string
  allergyInfo?: string
  familyHistory?: string
}

// ==================== 排班预约相关 ====================
export interface Schedule {
  id: number
  scheduleDate: string
  timeSlotStart: string
  timeSlotEnd: string
  technicianId: number
  technicianName: string
  maxCapacity: number
  reservedCount: number
  availableCount: number
  status: number
}

export interface Reserve {
  id: number
  childId: number
  childName: string
  scheduleId: number
  scheduleDate: string
  timeSlotStart: string
  timeSlotEnd: string
  technicianName: string
  reserveType: number // 1=初次检测 2=复查 3=养护
  parentName: string
  parentPhone: string
  remark?: string
  status: number // 0=待服务 1=已完成 2=已取消
  cancelReason?: string
}

export interface BatchScheduleRequest {
  startDate: string
  endDate: string
  technicianId: number
  timeSlots: Array<{
    start: string
    end: string
    capacity: number
  }>
  weekDays: number[]
}

// ==================== 视力检测相关 ====================
export interface VisionRecord {
  id: number
  childId: number
  childName: string
  testType: number // 1=裸眼 2=矫正
  eyeType: number // 0=双眼 1=左眼 2=右眼
  leftEye: string
  rightEye: string
  visionLevel: string
  testTime: string
  testerName: string
  remark?: string
  beforeAfter: string // before=养护前 after=养护后
  improvement?: string
  createdAt: string
}

export interface VisionCompare {
  childId: number
  childName: string
  beforeTest: {
    leftEye: string
    rightEye: string
  }
  afterTest: {
    leftEye: string
    rightEye: string
  }
  improvement: {
    leftEye: string
    rightEye: string
  }
}

// ==================== TV设备相关 ====================
export interface Device {
  id: number
  deviceCode: string
  deviceName: string
  status: number // 1=在线 0=离线
  calibrationStatus: number // 1=已校准 0=未校准
  lastSyncTime?: string
}

export interface DeviceBindRequest {
  storeId: number
  deviceCode: string
  deviceName: string
}

// ==================== 科室相关 ====================
export interface Department {
  id: number
  storeId: number
  storeName?: string
  deptCode: string
  deptName: string
  deptType: number // 1=门诊 2=养护 3=检测 4=其他
  sortOrder: number
  status: number // 1=启用 0=禁用
  remark?: string
  createdAt: string
}

// ==================== 数据统计相关 ====================
export interface DashboardStats {
  todayReserves: number
  pendingChildren: number
  activeDevices: number
  todayTests: number
}

export interface WeeklyTrend {
  dates: string[]
  reserveCounts: number[]
  testCounts: number[]
}

export interface VisionStatistics {
  labels: string[]
  beforeValues: number[]
  afterValues: number[]
}

// ==================== 通用分页 ====================
export interface Pagination {
  page: number
  size: number
  total: number
  pages: number
}

export interface PageResult<T> {
  list: T[]
  pagination: Pagination
}
