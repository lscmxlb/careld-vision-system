/** 视力值显示工具：5 分制主值 ↔ 小数视力对照（存储层不变，仅显示层转换） */

export const VISION_MAIN_OPTIONS = ['5.3', '5.2', '5.1', '5.0', '4.9', '4.8', '4.7', '4.6', '4.5', '4.4', '4.3', '4.2', '4.1', '4.0']
export const VISION_SUB_OPTIONS = ['+0', '+1', '+2', '+3', '+4', '+5', '-1', '-2', '-3', '-4', '-5']

/** 5 分制主值 → 小数视力对照（4.6 → 0.4） */
export const VISION_DECIMAL_MAP: Record<string, string> = {
  '4.0': '0.1', '4.1': '0.12', '4.2': '0.15', '4.3': '0.2', '4.4': '0.25', '4.5': '0.3', '4.6': '0.4',
  '4.7': '0.5', '4.8': '0.6', '4.9': '0.8', '5.0': '1.0', '5.1': '1.2', '5.2': '1.5', '5.3': '2.0'
}

/** 主值显示文案：4.6 → 4.6/0.4 */
export const visionMainLabel = (main: string): string => {
  const decimal = VISION_DECIMAL_MAP[main]
  return decimal ? `${main}/${decimal}` : main
}

export const VISION_MAIN_LABELS = VISION_MAIN_OPTIONS.map(visionMainLabel)

/** 存储值 → { main, sub }，默认子值 +0 */
export const parseVision = (v?: string): { main: string; sub: string } => {
  const matched = /^(5\.[0-3]|4\.[0-9])([+-]\d+)?$/.exec(v || '')
  return matched ? { main: matched[1]!, sub: matched[2] || '+0' } : { main: '', sub: '+0' }
}

/** 展示用：4.6 → 4.6/0.4；微调 4.8+1 → 4.8/0.6 +1；子值 +0 不显示 */
export const displayVision = (v?: string): string => {
  if (!v) return '-'
  const { main, sub } = parseVision(v)
  if (!main) return v
  const base = visionMainLabel(main)
  return sub !== '+0' ? `${base} ${sub}` : base
}
