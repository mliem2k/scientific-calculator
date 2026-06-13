import { describe, it, expect } from 'vitest'
import { moveCursorLeft, moveCursorRight, moveCursorUp, moveCursorDown } from './cursor'
import { ASTNode, Cursor } from './types'

const C = (insertAt: number, path: Cursor['path'] = []): Cursor => ({ path, insertAt })

const fraction: ASTNode[] = [{
  type: 'fraction',
  numerator: [{ type: 'number', value: '1' }],
  denominator: [{ type: 'number', value: '2' }],
}]

const exprAB: ASTNode[] = [
  { type: 'number', value: 'A' },
  { type: 'number', value: 'B' },
]

describe('moveCursorRight', () => {
  it('moves right in flat list', () => {
    expect(moveCursorRight(exprAB, C(0))).toEqual(C(1))
  })

  it('entering fraction from left → numerator start', () => {
    expect(moveCursorRight(fraction, C(0))).toEqual(
      C(0, [{ nodeIndex: 0, slot: 'numerator' }])
    )
  })

  it('numerator end → denominator start', () => {
    const cur = C(1, [{ nodeIndex: 0, slot: 'numerator' }])
    expect(moveCursorRight(fraction, cur)).toEqual(
      C(0, [{ nodeIndex: 0, slot: 'denominator' }])
    )
  })

  it('denominator end → exits fraction to right', () => {
    const cur = C(1, [{ nodeIndex: 0, slot: 'denominator' }])
    expect(moveCursorRight(fraction, cur)).toEqual(C(1))
  })

  it('stays at root end', () => {
    expect(moveCursorRight(exprAB, C(2))).toEqual(C(2))
  })
})

describe('moveCursorLeft', () => {
  it('moves left in flat list', () => {
    expect(moveCursorLeft(exprAB, C(2))).toEqual(C(1))
  })

  it('denominator start → numerator end', () => {
    const cur = C(0, [{ nodeIndex: 0, slot: 'denominator' }])
    expect(moveCursorLeft(fraction, cur)).toEqual(
      C(1, [{ nodeIndex: 0, slot: 'numerator' }])
    )
  })

  it('numerator start → exits to before fraction', () => {
    const cur = C(0, [{ nodeIndex: 0, slot: 'numerator' }])
    expect(moveCursorLeft(fraction, cur)).toEqual(C(0))
  })

  it('stays at root start', () => {
    expect(moveCursorLeft(exprAB, C(0))).toEqual(C(0))
  })
})

describe('moveCursorUp', () => {
  it('denominator → numerator', () => {
    const cur = C(0, [{ nodeIndex: 0, slot: 'denominator' }])
    const result = moveCursorUp(fraction, cur)
    expect(result.path[result.path.length - 1].slot).toBe('numerator')
  })

  it('does nothing at root', () => {
    expect(moveCursorUp(fraction, C(0))).toEqual(C(0))
  })

  it('does nothing in numerator already', () => {
    const cur = C(0, [{ nodeIndex: 0, slot: 'numerator' }])
    expect(moveCursorUp(fraction, cur)).toEqual(cur)
  })
})

describe('moveCursorDown', () => {
  it('numerator → denominator', () => {
    const cur = C(0, [{ nodeIndex: 0, slot: 'numerator' }])
    const result = moveCursorDown(fraction, cur)
    expect(result.path[result.path.length - 1].slot).toBe('denominator')
  })

  it('does nothing at root', () => {
    expect(moveCursorDown(fraction, C(0))).toEqual(C(0))
  })
})
