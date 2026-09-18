/**
 * 日期 / 时间 / 视力值格式化工具
 */

export function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function todayStr(): string {
  return formatDate(new Date())
}

export function addDays(date: Date, days: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days)
}

/** 时段统一显示 HH:mm（后端可能返回 HH:mm:ss） */
export function formatHm(t?: string): string {
  return t ? t.slice(0, 5) : ''
}

/** 时间戳/日期时间串 → HH:mm */
export function formatTimeHm(value?: string): string {
  if (!value) return ''
  const matched = /(\d{2}:\d{2})/.exec(value)
  return matched ? matched[1] : ''
}

export function formatDateTime(value?: string): string {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

/** 周一~周日 短标签 */
export function weekdayLabel(dateStr: string): string {
  const d = new Date(`${dateStr}T00:00:00`)
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
}

/** 出生日期 → 年龄文案 */
export function ageText(birthDate?: string, age?: number): string {
  if (typeof age === 'number' && age >= 0) return `${age}岁`
  if (!birthDate) return ''
  const birth = new Date(`${birthDate}T00:00:00`)
  if (Number.isNaN(birth.getTime())) return ''
  const now = new Date()
  let months = (now.getFullYear() - birth.getFullYear()) * 12 + (now.getMonth() - birth.getMonth())
  if (now.getDate() < birth.getDate()) months -= 1
  if (months < 0) return ''
  if (months < 12) return `${months}个月`
  return `${Math.floor(months / 12)}岁`
}

/* ---------------- 视力值 ---------------- */

export const VISION_MAIN_OPTIONS = ['5.3', '5.2', '5.1', '5.0', '4.9', '4.8', '4.7', '4.6', '4.5', '4.4', '4.3', '4.2', '4.1', '4.0']
export const VISION_SUB_OPTIONS = ['+0', '+1', '+2', '+3', '+4', '+5', '-1', '-2', '-3', '-4', '-5']
/** 档案裸眼视力下拉（仅主值 5.3~4.0） */
export const VISION_OPTIONS = VISION_MAIN_OPTIONS

/** 主值 + 子值 → 存储值（子值为空则不拼接） */
export function combineVision(main: string, sub: string): string | undefined {
  return main ? main + (sub || '') : undefined
}

/** 存储值 → { main, sub }，默认子值 +0 */
export function parseVision(v?: string): { main: string; sub: string } {
  const matched = /^(5\.[0-3]|4\.[0-9])([+-]\d+)?$/.exec(v || '')
  return matched ? { main: matched[1], sub: matched[2] || '+0' } : { main: '', sub: '+0' }
}

/** 展示用：去掉子值 +0（与 PC 一致） */
export function displayVision(v?: string): string {
  if (!v) return '-'
  return v.replace(/\+0$/, '')
}

/** 数值差展示（后 - 前），正数为提升 */
export function visionDelta(before?: string, after?: string): string {
  const b = parseVisionValue(before)
  const a = parseVisionValue(after)
  if (b === null || a === null) return ''
  const diff = Math.round((a - b) * 100) / 100
  if (diff === 0) return '持平'
  return diff > 0 ? `+${diff.toFixed(1)}` : diff.toFixed(1)
}

/** 视力值 → 数值（含子值符号，如 5.0+2 → 5.02 用于比较） */
export function parseVisionValue(v?: string): number | null {
  const matched = /^(5\.[0-3]|4\.[0-9])([+-]\d+)?$/.exec(v || '')
  if (!matched) return null
  const main = Number(matched[1])
  const sub = matched[2] ? Number(matched[2]) : 0
  return Math.round((main + sub / 100) * 1000) / 1000
}
