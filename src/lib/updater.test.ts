import { describe, it, expect } from 'vitest'
import { formatTag } from './updater'

describe('formatTag', () => {
  it('turns a nightly tag into a readable date', () => {
    expect(formatTag('nightly-20260616-1781592056')).toBe('Nightly, Jun 16, 2026')
  })

  it('returns unknown tags unchanged', () => {
    expect(formatTag('v1.2.3')).toBe('v1.2.3')
  })
})
