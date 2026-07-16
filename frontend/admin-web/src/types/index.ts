/**
 * Careld Vision System - 类型定义
 * 运营中心Web端
 */

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
  lastLoginTime?: string
  createdAt: string
  password?: string
}

export interface LoginRequest {
  username: string
  password: string
  captcha?: string
  captchaKey?: string
  loginType: number
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
  user: User
}

// ==================== 门店相关 ====================
export interface Store {
  id: number
  storeCode: string
  storeName: string
  provinceCode: string
  provinceName: string
  cityCode: string
  cityName: string
  districtCode: string
  districtName: string
  address: string
  longitude?: number
  latitude?: number
  contactName: string
  contactPhone: string
  businessHours: string
  networkType: number
  status: number
  deviceCount: number
  staffCount: number
  openTime?: string
  createdAt: string
}

export interface StoreQuery {
  page?: number
  size?: number
  status?: number
  provinceCode?: string
  keyword?: string
}

// ==================== 儿童档案相关 ====================
export interface Child {
  id: number
  childCode: string
  storeId: number
  storeName: string
  name: string
  phone: string
  birthDate: string
  age: number
  gender: number
  eyeCondition: string
  medicalHistory?: string
  allergyInfo?: string
  familyHistory?: string
  auditStatus: number
  auditRemark?: string
  auditedBy?: string
  auditedAt?: string
  parentUserId?: number
  lastVisionTest?: {
    testTime: string
    leftEye: string
    rightEye: string
  }
  createdAt: string
}

export interface ChildQuery {
  page?: number
  size?: number
  storeId?: number
  auditStatus?: number
  keyword?: string
  startDate?: string
  endDate?: string
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

export interface AuditRequest {
  auditStatus: number
  auditRemark: string
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
  storeName?: string
}

export interface Reserve {
  id: number
  orderNo: string
  childId: number
  childName: string
  storeId?: number
  storeName?: string
  scheduleId: number
  scheduleDate: string
  timeSlotStart: string
  timeSlotEnd: string
  technicianName: string
  reserveType: number // 1=视力检测 2=养护 3=复查
  parentName: string
  parentPhone: string
  source?: number // 1=小程序 2=门店 3=电话
  remark?: string
  cancelReason?: string
  status: number // 1=待到店 2=已到店 3=服务中 4=已完成 5=已取消
  arriveTime?: string
  createdAt: string
}

export interface ReserveQuery {
  storeId?: number
  scheduleId?: number
  status?: number
  date?: string
  startDate?: string
  endDate?: string
  keyword?: string
  page?: number
  size?: number
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
  updatedAt?: string
}

export interface DepartmentQuery {
  storeId?: number
  status?: number
  keyword?: string
  page?: number
  size?: number
}

// ==================== 视力检测相关 ====================
export interface VisionRecord {
  id: number
  recordCode: string
  childId: number
  childName: string
  storeId?: number
  storeName?: string
  reserveId?: number
  testType: number // 1=检测前 2=检测后
  eyeType: number // 1=左眼 2=右眼 3=双眼
  visionLevel: string
  visionDecimal?: number
  testerName: string
  testTime: string
  remark?: string
  createdAt: string
}

export interface VisionRecordQuery {
  childId?: number
  storeId?: number
  keyword?: string
  testType?: number
  eyeType?: number
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface VisionCompare {
  childId: number
  childName: string
  reserveId: number
  reserveDate: string
  beforeTest: {
    testTime: string
    leftEye: string
    rightEye: string
    testerName: string
  }
  afterTest: {
    testTime: string
    leftEye: string
    rightEye: string
    testerName: string
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
  storeId: number
  storeName: string
  status: number
  calibrationStatus: number
  calibrationData?: {
    pixelPerMm: number
    screenWidthMm: number
    screenHeightMm: number
    calibrationTime: string
    calibratedBy: string
  }
  lastSyncTime?: string
  createdAt: string
}

export interface DeviceBindRequest {
  storeId: number
  deviceCode: string
  deviceName: string
}

// ==================== 统计相关 ====================
export interface StoreTraffic {
  summary: {
    totalVisits: number
    avgDaily: number
    maxDaily: number
    growthRate: string
  }
  details: Array<{
    date: string
    visitCount: number
    newChildren: number
    returnChildren: number
  }>
}

export interface VisionImprovement {
  storeId: number
  storeName: string
  totalTests: number
  improvedCount: number
  improvementRate: string
  avgImprovement: string
}

export interface NationalSummary {
  storeCount: number
  totalChildren: number
  monthlyVisits: number
  avgImprovement: string
  topStores: Array<{
    storeId: number
    storeName: string
    visitCount: number
    improvementRate: string
  }>
}

// ==================== 操作日志相关 ====================
export interface OperationLog {
  id: number
  logType: number // 1=操作日志 2=登录日志 3=异常日志
  userId?: number
  userName?: string
  storeId?: number
  storeName?: string
  module: string
  action: string
  requestMethod: string
  requestUrl: string
  requestParams?: string
  responseData?: string
  ipAddress: string
  executeTime: number // 毫秒
  status: number // 1=成功 0=失败
  errorMsg?: string
  createdAt: string
}

export interface OperationLogQuery {
  logType?: number
  module?: string
  keyword?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

// ==================== 通用分页 ====================
export interface Pagination {
  page: number
  size: number
  total: number
  pages: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
  traceId?: string
}

export interface PageResponse<T> {
  list: T[]
  pagination: Pagination
}
