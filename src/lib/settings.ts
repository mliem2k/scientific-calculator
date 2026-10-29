import { useSyncExternalStore } from 'react'
import { THEMES, type ThemeId } from './themes'

export interface Settings {
  themeId: ThemeId
  defaultAngleMode: 'DEG' | 'RAD'
  /** Decimal places, 0 means automatic. */
  resultPrecision: number
  hapticFeedback: boolean
}

const KEY = 'calc_settings'

const DEFAULTS: Settings = {
  themeId: 'dark',
  defaultAngleMode: 'DEG',
  resultPrecision: 0,
  hapticFeedback: true,
}

function load(): Settings {
  try {
    const raw = localStorage.getItem(KEY)
    const saved = raw ? (JSON.parse(raw) as Partial<Settings>) : {}
    // A theme id from an older version (for example retro) falls back to the default.
    return { ...DEFAULTS, ...saved, themeId: saved.themeId && saved.themeId in THEMES ? saved.themeId : DEFAULTS.themeId }
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
