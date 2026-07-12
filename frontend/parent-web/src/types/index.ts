/**
 * Careld Vision System - 家长Web端类型定义
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
}

export interface LoginRequest {
  phone: string
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

// ==================== 儿童档案相关 ====================
export interface Child {
  id: number
  childCode: string
  storeId: number
  storeName?: string
  /** 脱敏姓名（后端 nameMask） */
  nameMask: string
  /** 兼容旧字段，等同于 nameMask */
  name?: string
  /** 脱敏手机号（后端 phoneMask） */
  phoneMask: string
  /** 兼容旧字段，等同于 phoneMask */
  phone?: string
  birthDate: string
  /** 年龄（前端根据 birthDate 计算，后端不直接返回） */
  age?: number
  gender: number
  eyeCondition: string
  medicalHistory?: string
  allergyInfo?: string
  familyHistory?: string
  auditStatus: number
  auditRemark?: string
  parentUserId?: number
  lastVisionTest?: {
    testTime: string
    leftEye: string
    rightEye: string
  }
  createdAt: string
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

export interface VisionRecordDetail {
  id: number
  recordCode: string
  childId: number
  childName: string
  storeId?: number
  storeName?: string
  reserveId?: number
  testType: number
  eyeType: number
  visionLevel: string
  visionDecimal?: number
  testerName: string
  testTime: string
  remark?: string
  createdAt: string
  leftEye?: string
  rightEye?: string
  leftDecimal?: number
  rightDecimal?: number
  scatter?: string
  axis?: string
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

export interface CreateReserveRequest {
  childId: number
  storeId: number
  scheduleId: number
  reserveType: number
  remark?: string
}

// ==================== 通用类型 ====================
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
  code: number
  message: string
  data: {
    list: T[]
    pagination: Pagination
  }
  timestamp: number
}
