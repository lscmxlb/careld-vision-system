/**
 * Careld 医生端 H5 - 类型定义
 */

export interface PageResult<T> {
  list: T[]
  pagination: {
    page: number
    size: number
    total: number
    pages: number
  }
}

/* ---------------- 用户 / 认证 ---------------- */
export interface User {
  id: number
  username: string
  realName: string
  phone: string
  userType: number
  /** 医务人员角色（1 医师 / 2 医生助理，仅 userType=6） */
  staffRole?: number
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
  loginType: number
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
  user: User
}

/* ---------------- 儿童档案 ---------------- */
export interface Child {
  id: number
  childCode?: string
  name: string
  phone: string
  parentName?: string
  relation?: string
  birthDate: string
  age?: number
  gender: number
  homeAddress?: string
  school?: string
  deliveryType?: string
  bedtime?: string
  wakeTime?: string
  eyeCondition: string
  nakedVisionBoth?: string
  nakedVisionLeft?: string
  nakedVisionRight?: string
  medicalHistory?: string
  allergyInfo?: string
  familyHistory?: string
  auditStatus: number
  auditRemark?: string
  storeId?: number
  doctorId?: number
  doctorName?: string
  sourceType?: number
  remainingCount?: number
  careCount?: number
  status: number
  createdAt: string
}

export interface ChildQuery {
  storeId?: number
  auditStatus?: number
  parentUserId?: number
  keyword?: string
  includeDisabled?: boolean
  remainingCountMin?: number
  status?: number
}

export interface CreateChildRequest {
  name: string
  phone: string
  storeId?: number
  parentName?: string
  relation?: string
  doctorId?: number
  doctorName?: string
  birthDate: string
  gender: number
  homeAddress?: string
  school?: string
  deliveryType?: string
  bedtime?: string
  wakeTime?: string
  eyeCondition: string
  nakedVisionBoth?: string
  nakedVisionLeft?: string
  nakedVisionRight?: string
  medicalHistory?: string
  allergyInfo?: string
  familyHistory?: string
}

/** 服务次数变更流水 */
export interface ChildServiceRecord {
  id: number
  childId: number
  changeType: number
  changeCount: number
  remainingAfter?: number
  paymentAmount?: number
  paymentMethod?: string
  doctorId?: number
  doctorName?: string
  remark?: string
  createdAt: string
}

/* ---------------- 工作台统计 ---------------- */
export interface WorkbenchStats {
  /** 儿童档案数量 */
  childCount: number
  /** 本月预约数量（预约日期在本月且状态为已预约） */
  reservedCount: number
  /** 养护次数（已完成养护记录数） */
  completedCareCount: number
}

/* ---------------- 排班 / 预约 ---------------- */
export interface ScheduleSlot {
  id: string
  storeId: number
  slotDate: string
  slotStartTime: string
  slotEndTime: string
  maxCapacity: number
  bookedCount: number
  status: number
}

/** 工作台日历：按天聚合名额（booked=已约 total=当日可约总量，总量固定不随已约变化） */
export interface SlotDailySummary {
  date: string
  booked: number
  total: number
}

export interface Reserve {
  id: number
  childId: number
  childName: string
  scheduleId?: number
  scheduleDate: string
  timeSlotStart: string
  timeSlotEnd: string
  technicianName?: string
  reserveType?: number
  parentName?: string
  parentPhone?: string
  remark?: string
  status: number
  cancelReason?: string
  /** 取消原因类型：1家长原因 2医院原因 */
  cancelReasonType?: number
  slotId?: string
  startTime?: string
  endTime?: string
  executorId?: number
  executorName?: string
  noShowFlag?: number
  refundFlag?: number
  source?: number
  operatorName?: string
  cancelOperatorName?: string
  adjustFlag?: number
  adjustOperatorName?: string
  createdAt?: string
}

export interface AppointmentConfig {
  id?: number
  storeId?: number
  parentCancelHours: number
  doctorCancelHours: number
  noShowBufferMinutes: number
  autoNoShowHours: number
  autoCompleteHours: number
  defaultShowStatuses?: string
}

/* ---------------- 医务人员 / 科室 ---------------- */
export interface MedicalStaff {
  id?: number
  storeId?: number
  name: string
  phone: string
  gender: number
  staffRole: number
  status: number
  createdAt?: string
}

export interface Department {
  id: number
  storeId: number
  deptName: string
  deptType: number
  chargeStandard?: number
  sortOrder: number
  status: number
}

/* ---------------- 养护记录 ---------------- */
export interface CareRecord {
  id: number
  appointmentId?: number
  childId: number
  childName?: string
  parentName?: string
  parentPhone?: string
  storeId?: number
  careDate: string
  timeSlot?: string
  visionBeforeLeft?: string
  visionBeforeRight?: string
  visionBeforeBoth?: string
  visionAfterLeft?: string
  visionAfterRight?: string
  visionAfterBoth?: string
  executorId?: number
  executorName?: string
  status: number
  childGender?: number
  childPhone?: string
  careCount?: number
  remainingCount?: number
  nakedVisionBoth?: string
  remark?: string
  createdAt?: string
}
