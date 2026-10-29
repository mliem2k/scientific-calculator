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
  onAngleToggle: vi.fn(),
  onMove: vi.fn(),
  onHistory: vi.fn(),
  onSettings: vi.fn(),
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
    expect(screen.getByText('Sto')).toBeInTheDocument()
  })

  it('shows SHIFT and HYP badges when active', () => {
    render(<Display {...baseProps} shiftActive hypActive />)
    expect(screen.getByText('shift')).toBeInTheDocument()
    expect(screen.getByText('hyp')).toBeInTheDocument()
  })

  it('calls onHistory and onSettings from the header buttons', () => {
    const onHistory = vi.fn()
    const onSettings = vi.fn()
    render(<Display {...baseProps} onHistory={onHistory} onSettings={onSettings} />)
    fireEvent.click(screen.getByLabelText('History'))
    fireEvent.click(screen.getByLabelText('Settings'))
    expect(onHistory).toHaveBeenCalled()
    expect(onSettings).toHaveBeenCalled()
  })

  it('calls onAngleToggle when angle label clicked', () => {
    const onAngleToggle = vi.fn()
    render(<Display {...baseProps} onAngleToggle={onAngleToggle} />)
    fireEvent.click(screen.getByText('Deg'))
    expect(onAngleToggle).toHaveBeenCalled()
  })
})
