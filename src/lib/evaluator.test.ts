import { describe, it, expect } from 'vitest'
import { evaluate } from './evaluator'
import { ASTNode } from './ast/types'

const num = (v: string): ASTNode => ({ type: 'number', value: v })
const op  = (o: '+' | '-' | '×' | '÷'): ASTNode => ({ type: 'operator', op: o })
const con = (n: 'pi' | 'e' | 'Ans'): ASTNode => ({ type: 'constant', name: n })
const fn  = (name: any, arg: ASTNode[]): ASTNode => ({ type: 'function', name, argument: arg })

describe('evaluate', () => {
  it('returns null for empty expression', () => {
    expect(evaluate([], 'RAD', null)).toBeNull()
  })

  it('returns null for invalid expression (lone operator)', () => {
    expect(evaluate([op('+')], 'RAD', null)).toBeNull()
  })

  it('evaluates simple addition', () => {
    expect(evaluate([num('2'), op('+'), num('3')], 'RAD', null)?.value).toBe('5')
  })

  it('evaluates fraction 1/4 = 0.25', () => {
    const nodes: ASTNode[] = [{
      type: 'fraction',
      numerator: [num('1')],
      denominator: [num('4')],
    }]
    expect(evaluate(nodes, 'RAD', null)?.value).toBe('0.25')
  })

  it('evaluates sin(pi) ≈ 0 in RAD', () => {
    const result = evaluate([fn('sin', [con('pi')])], 'RAD', null)
    expect(Math.abs(parseFloat(result!.value))).toBeLessThan(1e-10)
  })

  it('evaluates sin(90) = 1 in DEG', () => {
    const result = evaluate([fn('sin', [num('90')])], 'DEG', null)
    expect(parseFloat(result!.value)).toBeCloseTo(1, 10)
  })

  it('evaluates cos(0) = 1 in DEG', () => {
    const result = evaluate([fn('cos', [num('0')])], 'DEG', null)
    expect(parseFloat(result!.value)).toBeCloseTo(1, 10)
  })

  it('substitutes Ans', () => {
    const result = evaluate([con('Ans'), op('+'), num('1')], 'RAD', '5')
    expect(result?.value).toBe('6')
  })

  it('uses 0 for Ans when lastResult is null', () => {
    const result = evaluate([con('Ans'), op('+'), num('3')], 'RAD', null)
    expect(result?.value).toBe('3')
  })

  it('evaluates sqrt(4) = 2', () => {
    const nodes: ASTNode[] = [{ type: 'radical', degree: [], radicand: [num('4')] }]
    expect(evaluate(nodes, 'RAD', null)?.value).toBe('2')
  })

  it('evaluates 2^10 = 1024', () => {
    const nodes: ASTNode[] = [{ type: 'exponent', base: [num('2')], exponent: [num('10')] }]
    expect(evaluate(nodes, 'RAD', null)?.value).toBe('1024')
  })

  it('evaluates 5! = 120', () => {
    expect(evaluate([{ type: 'factorial', operand: [num('5')] }], 'RAD', null)?.value).toBe('120')
  })

  it('evaluates 5C2 = 10', () => {
    const nodes: ASTNode[] = [{ type: 'ncr', n: [num('5')], r: [num('2')] }]
    expect(evaluate(nodes, 'RAD', null)?.value).toBe('10')
  })

  it('evaluates log(100) = 2', () => {
    expect(evaluate([fn('log', [num('100')])], 'RAD', null)?.value).toBe('2')
  })

  it('evaluates ln(e) ≈ 1', () => {
    const result = evaluate([fn('ln', [con('e')])], 'RAD', null)
    expect(parseFloat(result!.value)).toBeCloseTo(1, 10)
  })
})
