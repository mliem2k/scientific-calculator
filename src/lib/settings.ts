import { useSyncExternalStore } from 'react'
import type { ThemeId } from './themes'

export interface Settings {
  themeId: ThemeId
  defaultAngleMode: 'DEG' | 'RAD'
  /** Decimal places, 0 means automatic. */
  resultPrecision: number
  showShiftLabels: boolean
  hapticFeedback: boolean
}

const KEY = 'calc_settings'

const DEFAULTS: Settings = {
  themeId: 'amoled',
  defaultAngleMode: 'DEG',
  resultPrecision: 0,
  showShiftLabels: true,
  hapticFeedback: true,
}

function load(): Settings {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? { ...DEFAULTS, ...(JSON.parse(raw) as Partial<Settings>) } : DEFAULTS
  } catch {
    return DEFAULTS
  }
}

let current = load()
const listeners = new Set<() => void>()

export function getSettings(): Settings {
  return current
}

export function updateSettings(patch: Partial<Settings>): void {
  current = { ...current, ...patch }
  try {
    localStorage.setItem(KEY, JSON.stringify(current))
  } catch { /* storage unavailable, keep in memory */ }
  listeners.forEach((l) => l())
}

export function useSettings(): Settings {
  return useSyncExternalStore(
    (cb) => {
      listeners.add(cb)
      return () => listeners.delete(cb)
    },
    getSettings,
  )
}
