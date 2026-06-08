import { create, all } from 'mathjs'
import { ASTNode, MemorySlots } from './ast/types'
import { serialize } from './ast/serializer'

const mathRad = create(all)
const mathDeg = create(all)

mathDeg.import!({
  sin:  (x: number) => Math.sin(x * Math.PI / 180),
  cos:  (x: number) => Math.cos(x * Math.PI / 180),
  tan:  (x: number) => Math.tan(x * Math.PI / 180),
  sinh: (x: number) => Math.sinh(x * Math.PI / 180),
  cosh: (x: number) => Math.cosh(x * Math.PI / 180),
  tanh: (x: number) => Math.tanh(x * Math.PI / 180),
  asin: (x: number) => Math.asin(x) * 180 / Math.PI,
  acos: (x: number) => Math.acos(x) * 180 / Math.PI,
  atan: (x: number) => Math.atan(x) * 180 / Math.PI,
  asinh: (x: number) => Math.asinh(x) * 180 / Math.PI,
  acosh: (x: number) => Math.acosh(x) * 180 / Math.PI,
  atanh: (x: number) => Math.atanh(x) * 180 / Math.PI,
}, { override: true })

export interface EvalResult {
  value: string
}

export function evaluate(
  nodes: ASTNode[],
  angleMode: 'DEG' | 'RAD',
  lastResult: string | null,
  memory?: MemorySlots,
): EvalResult | null {
  const expr = serialize(nodes)
  if (!expr.trim()) return null

  const withAns = expr.replace(/\bans\b/gi, lastResult ?? '0')
  const engine = angleMode === 'DEG' ? mathDeg : mathRad

  const scope: Record<string, number> = {
    A: memory?.A ?? 0,
    B: memory?.B ?? 0,
    X: memory?.X ?? 0,
    Y: memory?.Y ?? 0,
  }

  try {
    const raw = engine.evaluate(withAns, scope)
    if (raw === undefined || raw === null) return null
    const n = typeof raw === 'number' ? raw : Number(raw)
    if (!isFinite(n)) return { value: 'Math Error' }
    return { value: formatNumber(n) }
  } catch {
    return null
  }
}

export function toFraction(decimalStr: string): string | null {
  try {
    const n = parseFloat(decimalStr)
    if (!isFinite(n) || isNaN(n) || Number.isInteger(n)) return null
    const frac = mathDeg.fraction(n) as unknown as { n: number; d: number; s: number }
    if (frac.d <= 1 || frac.d > 999) return null
    return frac.s < 0 ? `-${frac.n}/${frac.d}` : `${frac.n}/${frac.d}`
  } catch {
    return null
  }
}

export function toMixed(fracStr: string): string | null {
  const match = fracStr.match(/^(-?\d+)\/(\d+)$/)
  if (!match) return null
  const num = parseInt(match[1])
  const den = parseInt(match[2])
  if (den === 0 || Math.abs(num) < den) return null
  const whole = Math.trunc(num / den)
  const remainder = Math.abs(num % den)
  if (remainder === 0) return String(whole)
  return `${whole} ${remainder}/${den}`
}

export function fromMixed(mixedStr: string): string | null {
  const match = mixedStr.match(/^(-?\d+) (\d+)\/(\d+)$/)
  if (!match) return null
  const whole = parseInt(match[1])
  const num = parseInt(match[2])
  const den = parseInt(match[3])
  const improperNum = whole < 0 ? whole * den - num : whole * den + num
  return `${improperNum}/${den}`
}

function formatNumber(n: number): string {
  if (Number.isInteger(n) && Math.abs(n) < 1e15) return String(n)
  const s = n.toPrecision(10)
  return String(parseFloat(s))
}
