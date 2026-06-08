import { useState } from 'react'
import { ASTNode, Cursor, CursorSegment } from '@/lib/ast/types'
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
  onShiftToggle: () => void
  onCopy: () => void
  onAngleToggle: () => void
  onCursorJump: (path: CursorSegment[], insertAt: number) => void
}

export function Display({
  expression, cursor, result,
  shiftActive, stoMode, hypActive, angleMode,
  onShiftToggle, onCopy, onAngleToggle, onCursorJump,
}: Props) {
  const [copied, setCopied] = useState(false)
  const empty = expression.length === 0 && cursor.path.length === 0 && cursor.insertAt === 0

  function handleCopy() {
    if (!result) return
    navigator.clipboard.writeText(result).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 1200)
    })
    onCopy()
  }

  return (
    <div className="flex px-3 pb-3 pt-2 min-h-[7.5rem] overflow-hidden gap-2">
      {/* Left strip: shift toggle, share, angle */}
      <div className="flex flex-col items-center gap-1 pt-0.5 flex-shrink-0">
        <button
          aria-label="toggle shift"
          onClick={onShiftToggle}
          className={cn(
            'text-base leading-none select-none transition-colors',
            shiftActive ? 'text-orange-400' : 'text-zinc-600 hover:text-zinc-400',
          )}
        >
          ≡
        </button>
        <button
          aria-label="share result"
          onClick={handleCopy}
          className="text-xs leading-none text-zinc-600 hover:text-zinc-400 select-none transition-colors"
        >
          ◁
        </button>
        <button
          onClick={onAngleToggle}
          className={cn(
            'text-[9px] tracking-widest select-none transition-colors',
            angleMode === 'RAD' ? 'text-blue-400' : 'text-zinc-600 hover:text-zinc-400',
          )}
        >
          {angleMode}
        </button>
      </div>

      {/* Main display: expression + result */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Status badges */}
        <div className="flex justify-end gap-2 h-4">
          {hypActive && <span className="text-[9px] text-indigo-400">HYP</span>}
          {stoMode   && <span className="text-[9px] text-yellow-400">STO</span>}
        </div>

        {/* Expression */}
        <div className="flex-1 overflow-x-auto flex items-center justify-end font-display text-white text-[1.75rem] leading-snug">
          {empty
            ? <span className="text-zinc-800">0</span>
            : <ASTRenderer nodes={expression} cursor={cursor} path={[]} onCursorJump={onCursorJump} />
          }
        </div>

        {/* Result */}
        {result !== null && (
          <button
            data-testid="result"
            onClick={handleCopy}
            className="self-end mt-1 select-none active:opacity-60 transition-opacity"
            aria-label="copy result"
          >
            <span className={copied ? 'font-display text-base text-green-400' : 'font-display text-base text-zinc-400'}>
              {copied ? '✓ copied' : result}
            </span>
          </button>
        )}
      </div>
    </div>
  )
}
