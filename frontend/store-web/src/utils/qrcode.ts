/**
 * 轻量二维码编码器（ISO/IEC 18004）
 *
 * 仅实现本系统用到的部分：字节模式、纠错级别 M、版本 1-10（最多 213 字节）。
 * 用于把微信 Native 支付的 code_url 渲染为扫码二维码；项目未引入二维码依赖，
 * 因此内联实现：数据位流 → Reed-Solomon 纠错 → 矩阵布局 → 8 种掩码按标准罚分择优。
 */

/** 纠错级别指示位（写进格式信息） */
export type EccLevel = 'L' | 'M' | 'Q' | 'H'

const ECC_BITS: Record<EccLevel, number> = { L: 1, M: 0, Q: 3, H: 2 }

interface BlockSpec {
  /** 每块纠错码字数 */
  ecPerBlock: number
  /** 数据块分组：[块数, 每块数据码字数] */
  groups: Array<[number, number]>
}

/** 版本 1-10、纠错级别 M 的纠错块结构（ISO/IEC 18004 表 9） */
const BLOCKS_M: BlockSpec[] = [
  { ecPerBlock: 10, groups: [[1, 16]] },
  { ecPerBlock: 16, groups: [[1, 28]] },
  { ecPerBlock: 26, groups: [[1, 44]] },
  { ecPerBlock: 18, groups: [[2, 32]] },
  { ecPerBlock: 24, groups: [[2, 43]] },
  { ecPerBlock: 16, groups: [[4, 27]] },
  { ecPerBlock: 18, groups: [[4, 31]] },
  { ecPerBlock: 22, groups: [[2, 38], [2, 39]] },
  { ecPerBlock: 22, groups: [[3, 36], [2, 37]] },
  { ecPerBlock: 26, groups: [[4, 43], [1, 44]] },
]

/** 校正图案中心坐标（版本 1-10） */
const ALIGNMENT_POSITIONS: number[][] = [
  [],
  [6, 18],
  [6, 22],
  [6, 26],
  [6, 30],
  [6, 34],
  [6, 22, 38],
  [6, 24, 42],
  [6, 26, 46],
  [6, 28, 50],
]

/** 二维码矩阵：true 为深色模块 */
export interface QrMatrix {
  version: number
  size: number
  modules: boolean[][]
}

// ==================== GF(256) 有限域 ====================

const EXP = new Uint8Array(512)
const LOG = new Uint8Array(256)
{
  let x = 1
  for (let i = 0; i < 255; i++) {
    EXP[i] = x
    LOG[x] = i
    x <<= 1
    if (x & 0x100) x ^= 0x11d
  }
  for (let i = 255; i < 512; i++) EXP[i] = EXP[i - 255]
}

function gfMul(a: number, b: number): number {
  if (a === 0 || b === 0) return 0
  return EXP[LOG[a] + LOG[b]]
}

/** 生成 n 次 Reed-Solomon 生成多项式，返回最高次系数之外的 n 个系数 */
function rsGenerator(n: number): number[] {
  let poly = [1]
  for (let i = 0; i < n; i++) {
    const next = new Array<number>(poly.length + 1).fill(0)
    for (let j = 0; j < poly.length; j++) {
      next[j] ^= poly[j]
      next[j + 1] ^= gfMul(poly[j], EXP[i])
    }
    poly = next
  }
  return poly.slice(1)
}

/** 计算纠错码字（多项式除法取余） */
function rsEncode(data: number[], ecPerBlock: number): number[] {
  const gen = rsGenerator(ecPerBlock)
  const remainder = new Array<number>(ecPerBlock).fill(0)
  for (const byte of data) {
    const factor = byte ^ remainder[0]
    remainder.shift()
    remainder.push(0)
    if (factor !== 0) {
      for (let i = 0; i < ecPerBlock; i++) {
        remainder[i] ^= gfMul(gen[i], factor)
      }
    }
  }
  return remainder
}

// ==================== 数据编码 ====================

function totalDataCodewords(version: number, spec: BlockSpec): number {
  return spec.groups.reduce((sum, [blocks, dataPerBlock]) => sum + blocks * dataPerBlock, 0)
}

/** 字节模式下该版本可容纳的字节数 */
function byteCapacity(version: number, spec: BlockSpec): number {
  const countBits = version >= 10 ? 16 : 8
  return Math.floor((totalDataCodewords(version, spec) * 8 - 4 - countBits) / 8)
}

function utf8Bytes(text: string): number[] {
  const bytes: number[] = []
  for (const char of text) {
    const code = char.codePointAt(0) as number
    if (code < 0x80) {
      bytes.push(code)
    } else if (code < 0x800) {
      bytes.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f))
    } else if (code < 0x10000) {
      bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f))
    } else {
      bytes.push(
        0xf0 | (code >> 18),
        0x80 | ((code >> 12) & 0x3f),
        0x80 | ((code >> 6) & 0x3f),
        0x80 | (code & 0x3f),
      )
    }
  }
  return bytes
}

/** 生成完整码字序列（数据码字 + 纠错码字，按块交织） */
function buildCodewords(bytes: number[], version: number, spec: BlockSpec): number[] {
  const dataCodewords = totalDataCodewords(version, spec)
  const countBits = version >= 10 ? 16 : 8

  const bits: number[] = []
  const put = (value: number, length: number) => {
    for (let i = length - 1; i >= 0; i--) bits.push((value >>> i) & 1)
  }
  put(0b0100, 4)
  put(bytes.length, countBits)
  for (const byte of bytes) put(byte, 8)

  const capacityBits = dataCodewords * 8
  for (let i = 0; i < 4 && bits.length < capacityBits; i++) bits.push(0)
  while (bits.length % 8 !== 0) bits.push(0)
  const padBytes = [0xec, 0x11]
  let padIndex = 0
  while (bits.length < capacityBits) put(padBytes[padIndex++ % 2], 8)

  const dataCw: number[] = []
  for (let i = 0; i < bits.length; i += 8) {
    let value = 0
    for (let j = 0; j < 8; j++) value = (value << 1) | bits[i + j]
    dataCw.push(value)
  }

  const blocks: Array<{ data: number[]; ec: number[] }> = []
  let offset = 0
  for (const [count, dataPerBlock] of spec.groups) {
    for (let i = 0; i < count; i++) {
      const data = dataCw.slice(offset, offset + dataPerBlock)
      offset += dataPerBlock
      blocks.push({ data, ec: rsEncode(data, spec.ecPerBlock) })
    }
  }

  const result: number[] = []
  const maxData = Math.max(...blocks.map((block) => block.data.length))
  for (let i = 0; i < maxData; i++) {
    for (const block of blocks) {
      if (i < block.data.length) result.push(block.data[i])
    }
  }
  for (let i = 0; i < spec.ecPerBlock; i++) {
    for (const block of blocks) result.push(block.ec[i])
  }
  return result
}

// ==================== 矩阵布局 ====================

function placeFinder(modules: (boolean | null)[][], row: number, col: number, size: number) {
  for (let r = -1; r <= 7; r++) {
    if (row + r < 0 || row + r >= size) continue
    for (let c = -1; c <= 7; c++) {
      if (col + c < 0 || col + c >= size) continue
      const inRing = (r >= 0 && r <= 6 && (c === 0 || c === 6)) || (c >= 0 && c <= 6 && (r === 0 || r === 6))
      const inCore = r >= 2 && r <= 4 && c >= 2 && c <= 4
      modules[row + r][col + c] = inRing || inCore
    }
  }
}

function placeAlignment(modules: (boolean | null)[][], row: number, col: number) {
  for (let r = -2; r <= 2; r++) {
    for (let c = -2; c <= 2; c++) {
      modules[row + r][col + c] = r === -2 || r === 2 || c === -2 || c === 2 || (r === 0 && c === 0)
    }
  }
}

function formatInfoBits(ecc: EccLevel, mask: number): number {
  const data = (ECC_BITS[ecc] << 3) | mask
  let value = data << 10
  while (bitLength(value) >= 11) {
    value ^= 0x537 << (bitLength(value) - 11)
  }
  return ((data << 10) | value) ^ 0x5412
}

function versionInfoBits(version: number): number {
  let value = version << 12
  while (bitLength(value) >= 13) {
    value ^= 0x1f25 << (bitLength(value) - 13)
  }
  return (version << 12) | value
}

function bitLength(value: number): number {
  let length = 0
  while (value !== 0) {
    value >>>= 1
    length++
  }
  return length
}

function maskFn(mask: number, row: number, col: number): boolean {
  switch (mask) {
    case 0:
      return (row + col) % 2 === 0
    case 1:
      return row % 2 === 0
    case 2:
      return col % 3 === 0
    case 3:
      return (row + col) % 3 === 0
    case 4:
      return (Math.floor(row / 2) + Math.floor(col / 3)) % 2 === 0
    case 5:
      return ((row * col) % 2) + ((row * col) % 3) === 0
    case 6:
      return (((row * col) % 2) + ((row * col) % 3)) % 2 === 0
    default:
      return (((row * col) % 3) + ((row + col) % 2)) % 2 === 0
  }
}

/** 按掩码生成完整矩阵（含功能图形、格式信息、掩码后的数据区） */
function buildMatrix(codewords: number[], version: number, ecc: EccLevel, mask: number): boolean[][] {
  const size = version * 4 + 17
  const modules: (boolean | null)[][] = Array.from({ length: size }, () =>
    new Array<boolean | null>(size).fill(null),
  )

  placeFinder(modules, 0, 0, size)
  placeFinder(modules, size - 7, 0, size)
  placeFinder(modules, 0, size - 7, size)

  // 校正图案（须先于定时图案：位于定时行列上的校正图案不能被跳过）
  const positions = ALIGNMENT_POSITIONS[version - 1]
  for (const row of positions) {
    for (const col of positions) {
      if (modules[row][col] !== null) continue
      placeAlignment(modules, row, col)
    }
  }

  // 定时图案（只填尚未占用的模块）
  for (let i = 8; i < size - 8; i++) {
    if (modules[i][6] === null) modules[i][6] = i % 2 === 0
    if (modules[6][i] === null) modules[6][i] = i % 2 === 0
  }

  // 格式信息：占位（值为 false，随后按所选掩码写入）
  for (let i = 0; i < 15; i++) {
    if (i < 6) modules[i][8] = false
    else if (i < 8) modules[i + 1][8] = false
    else modules[size - 15 + i][8] = false

    if (i < 8) modules[8][size - i - 1] = false
    else if (i < 9) modules[8][15 - i] = false
    else modules[8][15 - i - 1] = false
  }
  modules[size - 8][8] = true

  // 版本信息（版本 7 起）
  if (version >= 7) {
    const bits = versionInfoBits(version)
    for (let i = 0; i < 18; i++) {
      const dark = ((bits >> i) & 1) === 1
      modules[Math.floor(i / 3)][(i % 3) + size - 11] = dark
      modules[(i % 3) + size - 11][Math.floor(i / 3)] = dark
    }
  }

  // 数据位流按「列对」蛇形填充（跳过功能图形与第 6 列）
  const totalBits = codewords.length * 8
  let bitIndex = 0
  let upward = true
  for (let col = size - 1; col > 0; col -= 2) {
    if (col === 6) col -= 1
    for (let i = 0; i < size; i++) {
      const row = upward ? size - 1 - i : i
      for (let c = 0; c < 2; c++) {
        if (modules[row][col - c] !== null) continue
        let dark = false
        if (bitIndex < totalBits) {
          dark = ((codewords[bitIndex >> 3] >>> (7 - (bitIndex & 7))) & 1) === 1
        }
        bitIndex++
        modules[row][col - c] = dark !== maskFn(mask, row, col - c)
      }
    }
    upward = !upward
  }

  // 格式信息按掩码写入
  const formatBits = formatInfoBits(ecc, mask)
  for (let i = 0; i < 15; i++) {
    const dark = ((formatBits >> i) & 1) === 1
    if (i < 6) modules[i][8] = dark
    else if (i < 8) modules[i + 1][8] = dark
    else modules[size - 15 + i][8] = dark

    if (i < 8) modules[8][size - i - 1] = dark
    else if (i < 9) modules[8][15 - i] = dark
    else modules[8][15 - i - 1] = dark
  }

  return modules as boolean[][]
}

/** 标准罚分：连续同色 / 2×2 同色块 / 伪定位图案 / 深浅比例 */
function penalty(modules: boolean[][], size: number): number {
  let score = 0

  const runScore = (run: number) => (run >= 5 ? 3 + (run - 5) : 0)
  for (let row = 0; row < size; row++) {
    let run = 1
    for (let col = 1; col < size; col++) {
      if (modules[row][col] === modules[row][col - 1]) {
        run++
      } else {
        score += runScore(run)
        run = 1
      }
    }
    score += runScore(run)
  }
  for (let col = 0; col < size; col++) {
    let run = 1
    for (let row = 1; row < size; row++) {
      if (modules[row][col] === modules[row - 1][col]) {
        run++
      } else {
        score += runScore(run)
        run = 1
      }
    }
    score += runScore(run)
  }

  for (let row = 0; row < size - 1; row++) {
    for (let col = 0; col < size - 1; col++) {
      const value = modules[row][col]
      if (value === modules[row][col + 1] && value === modules[row + 1][col] && value === modules[row + 1][col + 1]) {
        score += 3
      }
    }
  }

  // 伪定位图案：1011101 前后有 4 个浅色模块
  const pattern = [true, false, true, true, true, false, true]
  const matches = (get: (offset: number) => boolean, start: number) => {
    for (let i = 0; i < 7; i++) {
      if (get(start + i) !== pattern[i]) return false
    }
    const before = [start - 4, start - 3, start - 2, start - 1]
    const after = [start + 7, start + 8, start + 9, start + 10]
    const allLight = (offsets: number[]) => offsets.every((offset) => !get(offset))
    return allLight(before) || allLight(after)
  }
  for (let row = 0; row < size; row++) {
    const get = (col: number) => (col < 0 || col >= size ? true : modules[row][col])
    for (let col = 0; col + 6 < size; col++) {
      if (matches(get, col)) score += 40
    }
  }
  for (let col = 0; col < size; col++) {
    const get = (row: number) => (row < 0 || row >= size ? true : modules[row][col])
    for (let row = 0; row + 6 < size; row++) {
      if (matches(get, row)) score += 40
    }
  }

  let dark = 0
  for (const row of modules) {
    for (const value of row) if (value) dark++
  }
  const percent = Math.floor((dark * 100) / (size * size))
  score += Math.floor(Math.abs(percent - 50) / 5) * 10

  return score
}

// ==================== 对外接口 ====================

/** 生成二维码矩阵（自动挑选版本与掩码） */
export function createQrMatrix(text: string, ecc: EccLevel = 'M'): QrMatrix {
  if (ecc !== 'M') {
    throw new Error('qrcode: 仅支持纠错级别 M')
  }
  const bytes = utf8Bytes(text)
  let version = 0
  for (let candidate = 1; candidate <= BLOCKS_M.length; candidate++) {
    if (bytes.length <= byteCapacity(candidate, BLOCKS_M[candidate - 1])) {
      version = candidate
      break
    }
  }
  if (version === 0) {
    throw new Error(`qrcode: 内容过长（${bytes.length} 字节，最多 213 字节）`)
  }
  const spec = BLOCKS_M[version - 1]
  const codewords = buildCodewords(bytes, version, spec)

  let best: boolean[][] | null = null
  let bestScore = Number.POSITIVE_INFINITY
  for (let mask = 0; mask < 8; mask++) {
    const modules = buildMatrix(codewords, version, ecc, mask)
    const score = penalty(modules, version * 4 + 17)
    if (score < bestScore) {
      bestScore = score
      best = modules
    }
  }
  return { version, size: version * 4 + 17, modules: best as boolean[][] }
}

/** 渲染到 canvas（含 4 模块静默区） */
export function drawQrMatrix(canvas: HTMLCanvasElement, text: string, pixelSize = 220): QrMatrix {
  const qr = createQrMatrix(text)
  const margin = 4
  const total = qr.size + margin * 2
  const scale = Math.max(1, Math.floor(pixelSize / total))
  const pixels = total * scale
  canvas.width = pixels
  canvas.height = pixels
  const ctx = canvas.getContext('2d') as CanvasRenderingContext2D
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, pixels, pixels)
  ctx.fillStyle = '#000000'
  for (let row = 0; row < qr.size; row++) {
    for (let col = 0; col < qr.size; col++) {
      if (!qr.modules[row][col]) continue
      ctx.fillRect((col + margin) * scale, (row + margin) * scale, scale, scale)
    }
  }
  return qr
}
