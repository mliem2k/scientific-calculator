import { describe, it, expect, beforeEach, vi } from 'vitest'
import { loadHistory, addEntry, clearHistory } from './history'
import { ASTNode } from './ast/types'

const store: Record<string, string> = {}
const lsMock = {
  getItem:    vi.fn((k: string) => store[k] ?? null),
  setItem:    vi.fn((k: string, v: string) => { store[k] = v }),
  removeItem: vi.fn((k: string) => { delete store[k] }),
}
Object.defineProperty(globalThis, 'localStorage', { value: lsMock, writable: true })

const expr: ASTNode[] = [{ type: 'number', value: '42' }]

beforeEach(() => { Object.keys(store).forEach(k => delete store[k]); vi.clearAllMocks() })

describe('loadHistory', () => {
  it('returns [] when nothing stored', () => expect(loadHistory()).toEqual([]))
  it('returns [] on corrupted data', () => {
    store['calc_history'] = 'bad json'
    expect(loadHistory()).toEqual([])
  })
})

describe('addEntry', () => {
  it('prepends entry and persists', () => {
    const entries = addEntry([], expr, '42')
    expect(entries[0].result).toBe('42')
    expect(entries[0].expression).toEqual(expr)
    expect(lsMock.setItem).toHaveBeenCalled()
  })

  it('most recent is first', () => {
    const e1 = addEntry([], expr, 'first')
    const e2 = addEntry(e1, expr, 'second')
    expect(e2[0].result).toBe('second')
    expect(e2[1].result).toBe('first')
  })

  it('caps at 50 entries', () => {
    let entries: any[] = []
    for (let i = 0; i < 55; i++) entries = addEntry(entries, expr, String(i))
    expect(entries).toHaveLength(50)
  })
})

describe('clearHistory', () => {
  it('removes from localStorage and returns []', () => {
    const cleared = clearHistory()
    expect(cleared).toEqual([])
    expect(lsMock.removeItem).toHaveBeenCalled()
  })
})
