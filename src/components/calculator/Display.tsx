import { useState } from 'react'
import { ASTNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { contrastText } from '@/lib/themes'
import { ASTRenderer } from '../ast/ASTRenderer'
import { cn } from '@/lib/utils'

interface Props {
  expression: ASTNode[]
  cursor: Cursor
  result: string | null
  shiftActive: boolean
  stoMode: boolean
  hypActive: boolean
  angleMode: 'DEG' | 'RAD'
  onAngleToggle: () => void
  onHistory: () => void
  onSettings: () => void
  onCursorJump: (path: CursorSegment[], insertAt: number) => void
}

const ICON_BTN = 'size-12 flex items-center justify-center text-dim hover:text-expr select-none rounded-full'

function Badge({ label, bg }: { label: string; bg: string }) {
  return (
    <span
      className="ml-1 rounded px-1.5 py-0.5 text-sm font-semibold"
      style={{ background: bg, color: contrastText(bg) }}
    >
      {label}
    </span>
  )
}

export function Display({
  expression, cursor, result,
  shiftActive, stoMode, hypActive, angleMode,
  onAngleToggle, onHistory, onSettings, onCursorJump,
}: Props) {
  const [copied, setCopied] = useState(false)
  const empty = expression.length === 0 && cursor.path.length === 0 && cursor.insertAt === 0
  const hasResult = result !== null
  const isRad = angleMode === 'RAD'

  function handleCopy() {
    if (!result) return
    navigator.clipboard.writeText(result).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 1200)
    }).catch(() => { /* clipboard denied */ })
  }

  return (
    <div className="flex h-full flex-col bg-display px-3 pb-3 pt-1">
      <div className="flex items-center">
        <button
          onClick={onAngleToggle}
          aria-label={isRad ? 'Radians, switch to degrees' : 'Degrees, switch to radians'}
          className={cn('min-h-12 min-w-12 select-none text-[15px] font-bold', isRad ? 'text-op' : 'text-dim')}
        >
          {angleMode}
        </button>
        {shiftActive && <Badge label="SHIFT" bg="var(--c-shift-active)" />}
        {hypActive && <Badge label="HYP" bg="var(--c-status-badge)" />}
        {stoMode && <Badge label="STO" bg="var(--c-status-badge)" />}
        <div className="flex-1" />
        <button aria-label="History" onClick={onHistory} className={ICON_BTN}>
          <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d="M3 12a9 9 0 1 0 3-6.7L3 8" /><path d="M3 3v5h5" /><path d="M12 7v5l3 2" />
          </svg>
        </button>
        <button aria-label="Settings" onClick={onSettings} className={ICON_BTN}>
          <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden>
            <path d="M4 7h10M18 7h2M4 17h2M10 17h10" /><circle cx="16" cy="7" r="2" /><circle cx="8" cy="17" r="2" />
          </svg>
        </button>
      </div>

      <div className="flex-1" />

      <div
        className={cn(
          'flex items-center justify-end overflow-x-auto font-mono-display font-bold text-expr',
          hasResult ? 'text-[1.75rem]' : 'text-[2.5rem]',
        )}
      >
        {empty
          ? <span className="text-dim">0</span>
          : <ASTRenderer nodes={expression} cursor={cursor} path={[]} onCursorJump={onCursorJump} />
        }
      </div>

      {hasResult && (
        <button
          data-testid="result"
          onClick={handleCopy}
          aria-label={`Result ${result}, tap to copy`}
          className="mt-1 max-w-full self-end overflow-x-auto whitespace-nowrap select-none font-mono-display text-[2.125rem] font-medium active:opacity-60"
        >
          <span className={copied ? 'text-shift' : 'text-result'}>{copied ? 'Copied' : result}</span>
        </button>
      )}
    </div>
  )
}
