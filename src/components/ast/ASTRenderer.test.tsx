import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ASTRenderer } from './ASTRenderer'
import { ASTNode, INITIAL_CURSOR } from '@/lib/ast/types'

describe('ASTRenderer', () => {
  it('renders a number node', () => {
    const nodes: ASTNode[] = [{ type: 'number', value: '42' }]
    render(<ASTRenderer nodes={nodes} cursor={INITIAL_CURSOR} path={[]} />)
    expect(screen.getByText('42')).toBeInTheDocument()
  })

  it('renders fraction with top and bottom parts', () => {
    const nodes: ASTNode[] = [{
      type: 'fraction',
      numerator: [{ type: 'number', value: '1' }],
      denominator: [{ type: 'number', value: '2' }],
    }]
    render(<ASTRenderer nodes={nodes} cursor={INITIAL_CURSOR} path={[]} />)
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('renders exponent with superscript', () => {
    const nodes: ASTNode[] = [{
      type: 'exponent',
      base: [{ type: 'number', value: '2' }],
      exponent: [{ type: 'number', value: '3' }],
    }]
    render(<ASTRenderer nodes={nodes} cursor={INITIAL_CURSOR} path={[]} />)
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('renders sin function with parentheses', () => {
    const nodes: ASTNode[] = [{
      type: 'function', name: 'sin',
      argument: [{ type: 'constant', name: 'pi' }],
    }]
    render(<ASTRenderer nodes={nodes} cursor={INITIAL_CURSOR} path={[]} />)
    expect(screen.getByText(/sin\(/)).toBeInTheDocument()
    expect(screen.getByText('π')).toBeInTheDocument()
  })

  it('renders blinking cursor at insertAt position', () => {
    const nodes: ASTNode[] = [{ type: 'number', value: '3' }]
    const cursor = { path: [], insertAt: 0 }
    const { container } = render(<ASTRenderer nodes={nodes} cursor={cursor} path={[]} />)
    expect(container.querySelector('.animate-pulse')).toBeInTheDocument()
  })
})
