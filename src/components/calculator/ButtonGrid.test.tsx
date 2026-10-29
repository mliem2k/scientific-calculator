import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ButtonGrid } from './ButtonGrid'

const props = { shiftActive: false, onButton: vi.fn(), onPaste: vi.fn() }

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

  it('shows sin( label when shift inactive', () => {
    render(<ButtonGrid {...props} />)
    expect(screen.getByText('sin(')).toBeInTheDocument()
  })

  it('shows asin action when shift active and sin( pressed', () => {
    const onButton = vi.fn()
    render(<ButtonGrid {...props} shiftActive onButton={onButton} />)
    fireEvent.click(screen.getByText('sin⁻¹('))
    expect(onButton).toHaveBeenCalledWith('asin')
  })

  it('renders AC button', () => {
    render(<ButtonGrid {...props} />)
    expect(screen.getByText('AC')).toBeInTheDocument()
  })

  it('AC calls onButton with AC', () => {
    const onButton = vi.fn()
    render(<ButtonGrid {...props} onButton={onButton} />)
    fireEvent.click(screen.getByText('AC'))
    expect(onButton).toHaveBeenCalledWith('AC')
  })
})
