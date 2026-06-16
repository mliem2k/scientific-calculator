import { describe, it, expect, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useCalculator } from './useCalculator'

const store: Record<string, string> = {}
Object.defineProperty(globalThis, 'localStorage', {
  value: {
    getItem: (k: string) => store[k] ?? null,
    setItem: (k: string, v: string) => { store[k] = v },
    removeItem: (k: string) => { delete store[k] },
  },
  writable: true,
})

beforeEach(() => Object.keys(store).forEach(k => delete store[k]))

describe('useCalculator', () => {
  it('initial state: empty expression, DEG mode', () => {
    const { result } = renderHook(() => useCalculator())
    expect(result.current.state.expression).toEqual([])
    expect(result.current.state.angleMode).toBe('DEG')
    expect(result.current.state.shiftActive).toBe(false)
  })

  it('pressing digit inserts number', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('3'))
    expect(result.current.state.expression[0]).toMatchObject({ type: 'number', value: '3' })
  })

  it('pressing digits builds number', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => { result.current.handleButton('4'); result.current.handleButton('2') })
    expect((result.current.state.expression[0] as any).value).toBe('42')
  })

  it('pressing = evaluates and adds to history', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => {
      result.current.handleButton('2')
      result.current.handleButton('plus')
      result.current.handleButton('3')
      result.current.handleButton('=')
    })
    expect(result.current.state.result).toBe('5')
    expect(result.current.state.history).toHaveLength(1)
    expect(result.current.state.history[0].result).toBe('5')
  })

  it('SHIFT toggle activates/deactivates', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('SHIFT'))
    expect(result.current.state.shiftActive).toBe(true)
    act(() => result.current.handleButton('SHIFT'))
    expect(result.current.state.shiftActive).toBe(false)
  })

  it('DEG_RAD toggles angle mode', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('DEG_RAD'))
    expect(result.current.state.angleMode).toBe('RAD')
    act(() => result.current.handleButton('DEG_RAD'))
    expect(result.current.state.angleMode).toBe('DEG')
  })

  it('fraction button positions cursor in numerator', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('fraction'))
    expect(result.current.state.cursor.path[0].slot).toBe('numerator')
  })

  it('DEL removes last node', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => { result.current.handleButton('5'); result.current.handleButton('DEL') })
    expect(result.current.state.expression).toEqual([])
  })

  it('restoreExpression loads history entry', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => {
      result.current.handleButton('9')
      result.current.handleButton('=')
    })
    const entry = result.current.state.history[0]
    act(() => result.current.handleRestore(entry))
    expect(result.current.state.result).toBe('9')
  })

  it('Sto sets stoMode true', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('Sto'))
    expect(result.current.state.stoMode).toBe(true)
  })

  it('pressing a non-memory button cancels stoMode', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('Sto'))
    act(() => result.current.handleButton('5'))
    expect(result.current.state.stoMode).toBe(false)
  })

  it('stores result to memory slot A after Sto + A', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('5'))
    act(() => result.current.handleButton('='))
    act(() => result.current.handleButton('Sto'))
    act(() => result.current.handleButton('A'))
    expect(result.current.state.memory.A).toBe(5)
    expect(result.current.state.stoMode).toBe(false)
  })

  it('inserts memory constant when A pressed without stoMode', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('A'))
    expect(result.current.state.expression[0]).toMatchObject({ type: 'constant', name: 'A' })
  })

  it('handleCursorJump moves cursor to given path+insertAt', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleCursorJump([{ nodeIndex: 0, slot: 'numerator' }], 2))
    expect(result.current.state.cursor).toEqual({
      path: [{ nodeIndex: 0, slot: 'numerator' }],
      insertAt: 2,
    })
  })

  it('cube action wraps previous node in exponent^3', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('4'))
    act(() => result.current.handleButton('cube'))
    expect(result.current.state.expression[0]).toMatchObject({ type: 'exponent' })
  })

  it('AC clears expression', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('5'))
    act(() => result.current.handleButton('AC'))
    expect(result.current.state.expression).toEqual([])
  })
})
