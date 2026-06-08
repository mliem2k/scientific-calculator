import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Display } from './Display'
import { INITIAL_CURSOR } from '@/lib/ast/types'

describe('Display', () => {
  it('shows placeholder 0 when expression empty', () => {
    render(<Display expression={[]} cursor={INITIAL_CURSOR} result={null} />)
    expect(screen.getByText('0')).toBeInTheDocument()
  })

  it('shows result line when result provided', () => {
    render(<Display expression={[]} cursor={INITIAL_CURSOR} result="42" />)
    expect(screen.getByText(/42/)).toBeInTheDocument()
  })

  it('does not show result line when result is null', () => {
    const { container } = render(<Display expression={[]} cursor={INITIAL_CURSOR} result={null} />)
    expect(container.querySelectorAll('.text-zinc-400').length).toBe(0)
  })
})
