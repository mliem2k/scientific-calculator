import { ASTNode, HistoryEntry } from './ast/types'

const KEY = 'calc_history'
const MAX = 50

export function loadHistory(): HistoryEntry[] {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as HistoryEntry[]) : []
  } catch { return [] }
}

export function addEntry(entries: HistoryEntry[], expression: ASTNode[], result: string): HistoryEntry[] {
  const next = [
    { id: crypto.randomUUID(), expression, result, timestamp: Date.now() },
    ...entries,
  ].slice(0, MAX)
  localStorage.setItem(KEY, JSON.stringify(next))
  return next
}

export function clearHistory(): HistoryEntry[] {
  localStorage.removeItem(KEY)
  return []
}
