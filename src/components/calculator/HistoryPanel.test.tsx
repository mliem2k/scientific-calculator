import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { HistoryPanel } from './HistoryPanel'
import { HistoryEntry } from '@/lib/ast/types'

const entry: HistoryEntry = {
  id: '1', result: '42', expression: [{ type: 'number', value: '42' }], timestamp: 0,
}

describe('HistoryPanel', () => {
  it('renders nothing when history empty', () => {
    const { container } = render(<HistoryPanel history={[]} onRestore={vi.fn()} />)
    expect(container.firstChild).toBeNull()
  })

  it('shows collapsed header with count', () => {
    render(<HistoryPanel history={[entry]} onRestore={vi.fn()} />)
    expect(screen.getByText(/History \(1\)/)).toBeInTheDocument()
  })

  it('expands and shows result on toggle', () => {
    render(<HistoryPanel history={[entry]} onRestore={vi.fn()} />)
    fireEvent.click(screen.getByRole('button'))
    expect(screen.getByText('42')).toBeInTheDocument()
  })

  it('calls onRestore when history item clicked', () => {
    const onRestore = vi.fn()
    render(<HistoryPanel history={[entry]} onRestore={onRestore} />)
    fireEvent.click(screen.getByRole('button'))
    fireEvent.click(screen.getByText('42'))
    expect(onRestore).toHaveBeenCalledWith(entry)
  })
})
