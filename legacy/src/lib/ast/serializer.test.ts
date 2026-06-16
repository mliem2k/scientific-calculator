import { describe, it, expect } from 'vitest'
import { serialize } from './serializer'
import { ASTNode } from './types'

describe('serialize', () => {
  it('number', () => expect(serialize([{ type: 'number', value: '42' }])).toBe('42'))
  it('+ operator', () => expect(serialize([{ type: 'operator', op: '+' }])).toBe('+'))
  it('× operator maps to *', () => expect(serialize([{ type: 'operator', op: '×' }])).toBe('*'))
  it('÷ operator maps to /', () => expect(serialize([{ type: 'operator', op: '÷' }])).toBe('/'))
  it('pi constant', () => expect(serialize([{ type: 'constant', name: 'pi' }])).toBe('pi'))
  it('e constant', () => expect(serialize([{ type: 'constant', name: 'e' }])).toBe('e'))
  it('Ans constant', () => expect(serialize([{ type: 'constant', name: 'Ans' }])).toBe('ans'))

  it('fraction', () => {
    const nodes: ASTNode[] = [{
      type: 'fraction',
      numerator: [{ type: 'number', value: '1' }],
      denominator: [{ type: 'number', value: '2' }],
    }]
    expect(serialize(nodes)).toBe('(1)/(2)')
  })

  it('exponent', () => {
    const nodes: ASTNode[] = [{
      type: 'exponent',
      base: [{ type: 'number', value: '2' }],
      exponent: [{ type: 'number', value: '3' }],
    }]
    expect(serialize(nodes)).toBe('(2)^(3)')
  })

  it('sqrt radical (empty degree)', () => {
    const nodes: ASTNode[] = [{
      type: 'radical',
      degree: [],
      radicand: [{ type: 'number', value: '4' }],
    }]
    expect(serialize(nodes)).toBe('sqrt(4)')
  })

  it('nth root radical', () => {
    const nodes: ASTNode[] = [{
      type: 'radical',
      degree: [{ type: 'number', value: '3' }],
      radicand: [{ type: 'number', value: '8' }],
    }]
    expect(serialize(nodes)).toBe('nthRoot(8, 3)')
  })

  it('sin function', () => {
    const nodes: ASTNode[] = [{ type: 'function', name: 'sin', argument: [{ type: 'constant', name: 'pi' }] }]
    expect(serialize(nodes)).toBe('sin(pi)')
  })

  it('log maps to log base 10', () => {
    const nodes: ASTNode[] = [{ type: 'function', name: 'log', argument: [{ type: 'number', value: '100' }] }]
    expect(serialize(nodes)).toBe('log(100, 10)')
  })

  it('ln maps to natural log', () => {
    const nodes: ASTNode[] = [{ type: 'function', name: 'ln', argument: [{ type: 'number', value: '1' }] }]
    expect(serialize(nodes)).toBe('log(1)')
  })

  it('pow10', () => {
    const nodes: ASTNode[] = [{ type: 'function', name: 'pow10', argument: [{ type: 'number', value: '2' }] }]
    expect(serialize(nodes)).toBe('10^(2)')
  })

  it('exp (e^x)', () => {
    const nodes: ASTNode[] = [{ type: 'function', name: 'exp', argument: [{ type: 'number', value: '1' }] }]
    expect(serialize(nodes)).toBe('e^(1)')
  })

  it('factorial', () => {
    const nodes: ASTNode[] = [{ type: 'factorial', operand: [{ type: 'number', value: '5' }] }]
    expect(serialize(nodes)).toBe('factorial(5)')
  })

  it('nCr', () => {
    const nodes: ASTNode[] = [{
      type: 'ncr',
      n: [{ type: 'number', value: '5' }],
      r: [{ type: 'number', value: '2' }],
    }]
    expect(serialize(nodes)).toBe('combinations(5, 2)')
  })

  it('nPr', () => {
    const nodes: ASTNode[] = [{
      type: 'npr',
      n: [{ type: 'number', value: '5' }],
      r: [{ type: 'number', value: '2' }],
    }]
    expect(serialize(nodes)).toBe('permutations(5, 2)')
  })

  it('paren', () => {
    const nodes: ASTNode[] = [{ type: 'paren', children: [{ type: 'number', value: '3' }] }]
    expect(serialize(nodes)).toBe('(3)')
  })

  it('complex: 1/2 + sin(pi)', () => {
    const nodes: ASTNode[] = [
      { type: 'fraction', numerator: [{ type: 'number', value: '1' }], denominator: [{ type: 'number', value: '2' }] },
      { type: 'operator', op: '+' },
      { type: 'function', name: 'sin', argument: [{ type: 'constant', name: 'pi' }] },
    ]
    expect(serialize(nodes)).toBe('(1)/(2)+sin(pi)')
  })

  it('empty array returns empty string', () => {
    expect(serialize([])).toBe('')
  })
})
