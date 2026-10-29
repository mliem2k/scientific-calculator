import { describe, it, expect } from 'vitest'
import { getSettings, updateSettings } from './settings'
import { evaluate } from './evaluator'
import { insertDigit, insertDecimalPoint, insertOperator } from './ast/builder'
import { INITIAL_CURSOR, ASTNode, Cursor } from './ast/types'

describe('settings', () => {
  it('merges a patch and persists it', () => {
    updateSettings({ resultPrecision: 4 })
    expect(getSettings().resultPrecision).toBe(4)
    expect(JSON.parse(localStorage.getItem('calc_settings')!).resultPrecision).toBe(4)
    updateSettings({ resultPrecision: 0 })
  })
})

describe('result precision', () => {
  function oneThird(precision: number) {
    let pair: [ASTNode[], Cursor] = [[], INITIAL_CURSOR]
    pair = insertDigit(pair[0], pair[1], '1')
    pair = insertOperator(pair[0], pair[1], '÷')
    pair = insertDigit(pair[0], pair[1], '3')
    return evaluate(pair[0], 'DEG', null, undefined, precision)?.value
  }

  it('auto keeps ten significant digits', () => {
    expect(oneThird(0)).toBe('0.3333333333')
  })

  it('rounds to the chosen decimal places', () => {
    expect(oneThird(4)).toBe('0.3333')
  })

  it('drops trailing zeros on exact results', () => {
    let pair: [ASTNode[], Cursor] = insertDigit([], INITIAL_CURSOR, '2')
    pair = insertDecimalPoint(pair[0], pair[1])
    pair = insertDigit(pair[0], pair[1], '5')
    expect(evaluate(pair[0], 'DEG', null, undefined, 6)?.value).toBe('2.5')
  })
})
