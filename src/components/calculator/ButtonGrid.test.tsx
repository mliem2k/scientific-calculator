import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ButtonGrid } from './ButtonGrid'

const props = { angleMode: 'DEG' as const, shiftActive: false, hypActive: false, onButton: vi.fn() }

describe('ButtonGrid', () => {
  it('renders = button', () => {
    render(<ButtonGrid {...props} />)
    expect(screen.getByText('=')).toBeInTheDocument()
  })

  it('calls onButton with digit id when digit pressed', () => {
    const onButton = vi.fn()
    render(<ButtonGrid {...props} onButton={onButton} />)
    fireEvent.click(screen.getByText('7'))
    expect(onButton).toHaveBeenCalledWith('7')
  })

  it('shows sin label when shift inactive', () => {
    render(<ButtonGrid {...props} />)
    expect(screen.getByText('sin')).toBeInTheDocument()
  })

  it('shows sin⁻¹ label when shift active', () => {
    render(<ButtonGrid {...props} shiftActive />)
    expect(screen.getByText('sin⁻¹')).toBeInTheDocument()
  })

  it('shows sinh label when hyp active', () => {
    render(<ButtonGrid {...props} hypActive />)
    expect(screen.getByText('sinh')).toBeInTheDocument()
  })

  it('shows DEG label matching angleMode', () => {
    render(<ButtonGrid {...props} angleMode="RAD" />)
    expect(screen.getByText('RAD')).toBeInTheDocument()
  })
})
