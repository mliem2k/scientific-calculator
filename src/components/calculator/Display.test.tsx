import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { Display } from './Display'
import { INITIAL_CURSOR } from '@/lib/ast/types'

const baseProps = {
  expression: [],
  cursor: INITIAL_CURSOR,
  result: null,
  shiftActive: false,
  stoMode: false,
  hypActive: false,
  angleMode: 'DEG' as const,
  onShiftToggle: vi.fn(),
  onCopy: vi.fn(),
  onAngleToggle: vi.fn(),
  onCursorJump: vi.fn(),
}

describe('Display', () => {
  it('shows placeholder 0 when expression empty', () => {
    render(<Display {...baseProps} />)
    expect(screen.getByText('0')).toBeInTheDocument()
  })

  it('shows result when result provided (no = prefix)', () => {
    render(<Display {...baseProps} result="42" />)
    expect(screen.getByText('42')).toBeInTheDocument()
  })

  it('does not show result when result is null', () => {
    const { container } = render(<Display {...baseProps} />)
    expect(container.querySelector('[data-testid="result"]')).toBeNull()
  })

  it('shows STO indicator when stoMode true', () => {
    render(<Display {...baseProps} stoMode />)
    expect(screen.getByText('STO')).toBeInTheDocument()
  })

  it('calls onShiftToggle when ≡ clicked', () => {
    const onShiftToggle = vi.fn()
    render(<Display {...baseProps} onShiftToggle={onShiftToggle} />)
    fireEvent.click(screen.getByLabelText('toggle shift'))
    expect(onShiftToggle).toHaveBeenCalled()
  })

  it('calls onAngleToggle when angle label clicked', () => {
    const onAngleToggle = vi.fn()
    render(<Display {...baseProps} onAngleToggle={onAngleToggle} />)
    fireEvent.click(screen.getByText('DEG'))
    expect(onAngleToggle).toHaveBeenCalled()
  })
})
