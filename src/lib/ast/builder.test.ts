import { describe, it, expect } from 'vitest'
import {
  insertDigit, insertDecimalPoint, insertOperator,
  insertFraction, insertExponent, insertRadical, insertSquare,
  insertFunction, insertConstant, insertFactorial, insertNcr, insertNpr,
  insertParen, deleteCurrent, clearAll,
  insertCube, insertNthRadical, insertMixed, insertNumberLiteral,
} from './builder'
import { ASTNode, Cursor, INITIAL_CURSOR } from './types'

const C = (insertAt: number, path: Cursor['path'] = []): Cursor => ({ path, insertAt })

describe('insertDigit', () => {
  it('inserts NumberNode at empty expression', () => {
    const [nodes, cur] = insertDigit([], INITIAL_CURSOR, '3')
    expect(nodes).toEqual([{ type: 'number', value: '3' }])
    expect(cur).toEqual(C(1))
  })

  it('appends to adjacent NumberNode', () => {
    const init: ASTNode[] = [{ type: 'number', value: '3' }]
    const [nodes, cur] = insertDigit(init, C(1), '5')
    expect(nodes).toEqual([{ type: 'number', value: '35' }])
    expect(cur).toEqual(C(1))
  })

  it('inserts new NumberNode after operator', () => {
    const init: ASTNode[] = [{ type: 'operator', op: '+' }]
    const [nodes] = insertDigit(init, C(1), '5')
    expect(nodes[1]).toEqual({ type: 'number', value: '5' })
  })
})

describe('insertDecimalPoint', () => {
  it('appends dot to existing number', () => {
    const init: ASTNode[] = [{ type: 'number', value: '3' }]
    const [nodes] = insertDecimalPoint(init, C(1))
    expect((nodes[0] as any).value).toBe('3.')
  })

  it('creates 0. node when no prior number', () => {
    const [nodes] = insertDecimalPoint([], INITIAL_CURSOR)
    expect(nodes[0]).toEqual({ type: 'number', value: '0.' })
  })

  it('does not add second dot', () => {
    const init: ASTNode[] = [{ type: 'number', value: '3.' }]
    const [nodes] = insertDecimalPoint(init, C(1))
    expect((nodes[0] as any).value).toBe('3.')
  })
})

describe('insertOperator', () => {
  it('inserts operator node', () => {
    const [nodes] = insertOperator([], INITIAL_CURSOR, '+')
    expect(nodes[0]).toEqual({ type: 'operator', op: '+' })
  })
})

describe('insertFraction', () => {
  it('inserts fraction template and positions cursor in numerator', () => {
    const [nodes, cur] = insertFraction([], INITIAL_CURSOR)
    expect(nodes[0]).toMatchObject({ type: 'fraction', numerator: [], denominator: [] })
    expect(cur).toEqual({ path: [{ nodeIndex: 0, slot: 'numerator' }], insertAt: 0 })
  })
})

describe('insertExponent', () => {
  it('wraps previous node as base, cursor in exponent slot', () => {
    const init: ASTNode[] = [{ type: 'number', value: '2' }]
    const [nodes, cur] = insertExponent(init, C(1))
    expect(nodes[0]).toMatchObject({
      type: 'exponent',
      base: [{ type: 'number', value: '2' }],
      exponent: [],
    })
    expect(cur.path[0].slot).toBe('exponent')
    expect(cur.insertAt).toBe(0)
  })

  it('creates empty base when nothing before cursor', () => {
    const [nodes, cur] = insertExponent([], INITIAL_CURSOR)
    expect((nodes[0] as any).base).toEqual([])
    expect(cur.path[0].slot).toBe('exponent')
  })
})

describe('insertRadical', () => {
  it('inserts sqrt with empty degree, cursor in radicand', () => {
    const [nodes, cur] = insertRadical([], INITIAL_CURSOR, false)
    expect(nodes[0]).toMatchObject({ type: 'radical', degree: [], radicand: [] })
    expect(cur.path[0].slot).toBe('radicand')
  })

  it('inserts cbrt with degree=[3]', () => {
    const [nodes] = insertRadical([], INITIAL_CURSOR, true)
    expect((nodes[0] as any).degree).toEqual([{ type: 'number', value: '3' }])
  })
})

describe('insertSquare', () => {
  it('wraps previous node in exponent with exponent=[2]', () => {
    const init: ASTNode[] = [{ type: 'number', value: '4' }]
    const [nodes, cur] = insertSquare(init, C(1))
    expect(nodes[0]).toMatchObject({
      type: 'exponent',
      base: [{ type: 'number', value: '4' }],
      exponent: [{ type: 'number', value: '2' }],
    })
    expect(cur).toEqual(C(1))
  })
})

describe('insertFunction', () => {
  it('inserts function node, cursor in argument', () => {
    const [nodes, cur] = insertFunction([], INITIAL_CURSOR, 'sin')
    expect(nodes[0]).toMatchObject({ type: 'function', name: 'sin', argument: [] })
    expect(cur.path[0].slot).toBe('argument')
  })
})

describe('insertConstant', () => {
  it('inserts pi constant', () => {
    const [nodes] = insertConstant([], INITIAL_CURSOR, 'pi')
    expect(nodes[0]).toEqual({ type: 'constant', name: 'pi' })
  })
})

describe('insertFactorial', () => {
  it('wraps previous node in factorial', () => {
    const init: ASTNode[] = [{ type: 'number', value: '5' }]
    const [nodes, cur] = insertFactorial(init, C(1))
    expect(nodes[0]).toMatchObject({ type: 'factorial', operand: [{ type: 'number', value: '5' }] })
    expect(cur).toEqual(C(1))
  })
})

describe('insertNcr', () => {
  it('wraps previous node as n, cursor in r slot', () => {
    const init: ASTNode[] = [{ type: 'number', value: '5' }]
    const [nodes, cur] = insertNcr(init, C(1))
    expect(nodes[0]).toMatchObject({ type: 'ncr', n: [{ type: 'number', value: '5' }], r: [] })
    expect(cur.path[0].slot).toBe('r')
  })
})

describe('insertNpr', () => {
  it('wraps previous node as n, cursor in r slot', () => {
    const init: ASTNode[] = [{ type: 'number', value: '5' }]
    const [nodes, cur] = insertNpr(init, C(1))
    expect(nodes[0]).toMatchObject({ type: 'npr', n: [{ type: 'number', value: '5' }], r: [] })
    expect(cur.path[0].slot).toBe('r')
  })
})

describe('insertParen', () => {
  it('open: inserts paren node, cursor in children', () => {
    const [nodes, cur] = insertParen([], INITIAL_CURSOR, 'open')
    expect(nodes[0]).toMatchObject({ type: 'paren', children: [] })
    expect(cur.path[0].slot).toBe('children')
  })

  it('close: exits to parent level after paren node', () => {
    const init: ASTNode[] = [{ type: 'paren', children: [] }]
    const inner: Cursor = { path: [{ nodeIndex: 0, slot: 'children' }], insertAt: 0 }
    const [, cur] = insertParen(init, inner, 'close')
    expect(cur).toEqual(C(1))
  })
})

describe('deleteCurrent', () => {
  it('does nothing when expression empty at root', () => {
    const [nodes, cur] = deleteCurrent([], INITIAL_CURSOR)
    expect(nodes).toEqual([])
    expect(cur).toEqual(INITIAL_CURSOR)
  })

  it('removes last node', () => {
    const init: ASTNode[] = [{ type: 'number', value: '3' }, { type: 'operator', op: '+' }]
    const [nodes, cur] = deleteCurrent(init, C(2))
    expect(nodes).toEqual([{ type: 'number', value: '3' }])
    expect(cur).toEqual(C(1))
  })

  it('removes last digit from multi-digit number', () => {
    const init: ASTNode[] = [{ type: 'number', value: '35' }]
    const [nodes, cur] = deleteCurrent(init, C(1))
    expect((nodes[0] as any).value).toBe('3')
    expect(cur).toEqual(C(1))
  })

  it('exits to parent when at position 0 in a slot', () => {
    const init: ASTNode[] = [{ type: 'fraction', numerator: [], denominator: [] }]
    const inner: Cursor = { path: [{ nodeIndex: 0, slot: 'numerator' }], insertAt: 0 }
    const [, cur] = deleteCurrent(init, inner)
    expect(cur).toEqual(C(0))
  })
})

describe('clearAll', () => {
  it('returns empty expression and reset cursor', () => {
    const [nodes, cur] = clearAll()
    expect(nodes).toEqual([])
    expect(cur).toEqual(INITIAL_CURSOR)
  })
})

describe('insertCube', () => {
  it('wraps previous node as base with exponent 3', () => {
    const init: ASTNode[] = [{ type: 'number', value: '5' }]
    const [nodes] = insertCube(init, C(1))
    expect(nodes).toHaveLength(1)
    expect(nodes[0]).toMatchObject({ type: 'exponent', exponent: [{ type: 'number', value: '3' }] })
    expect((nodes[0] as any).base).toEqual([{ type: 'number', value: '5' }])
  })

  it('inserts empty-base exponent^3 when no previous node', () => {
    const [nodes] = insertCube([], INITIAL_CURSOR)
    expect(nodes[0]).toMatchObject({ type: 'exponent', base: [], exponent: [{ type: 'number', value: '3' }] })
  })
})

describe('insertNthRadical', () => {
  it('inserts radical with empty degree and cursor in degree slot', () => {
    const [nodes, cur] = insertNthRadical([], INITIAL_CURSOR)
    expect(nodes[0]).toMatchObject({ type: 'radical', degree: [], radicand: [] })
    expect(cur.path).toEqual([{ nodeIndex: 0, slot: 'degree' }])
    expect(cur.insertAt).toBe(0)
  })
})

describe('insertMixed', () => {
  it('inserts whole-number 0 then empty fraction, cursor starts in numerator', () => {
    const [nodes, cur] = insertMixed([], INITIAL_CURSOR)
    expect(nodes).toHaveLength(2)
    expect(nodes[0]).toMatchObject({ type: 'number', value: '0' })
    expect(nodes[1]).toMatchObject({ type: 'fraction', numerator: [], denominator: [] })
    expect(cur.path).toEqual([{ nodeIndex: 1, slot: 'numerator' }])
    expect(cur.insertAt).toBe(0)
  })
})

describe('insertNumberLiteral', () => {
  it('inserts a NumberNode with the given string value', () => {
    const [nodes, cur] = insertNumberLiteral([], INITIAL_CURSOR, '3.14')
    expect(nodes[0]).toEqual({ type: 'number', value: '3.14' })
    expect(cur.insertAt).toBe(1)
  })
})
