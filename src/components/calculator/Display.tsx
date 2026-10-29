import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { ASTNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ast/ASTRenderer'

interface Props {
  expression: ASTNode[]
  cursor: Cursor
  result: string | null
  shiftActive: boolean
  stoMode: boolean
  hypActive: boolean
  angleMode: 'DEG' | 'RAD'
  onAngleToggle: () => void
  onMove: (dir: 'LEFT' | 'RIGHT' | 'UP' | 'DOWN') => void
  onHistory: () => void
  onSettings: () => void
  onCursorJump: (path: CursorSegment[], insertAt: number) => void
}

const ICON_BTN = 'size-12 flex items-center justify-center text-dim active:text-ink select-none rounded-full'

function Badge({ label }: { label: string }) {
  return <span className="ml-2 rounded-full bg-sci px-2.5 py-1 text-xs font-semibold text-on-sci">{label}</span>
}

export function Display({
  expression, cursor, result,
  shiftActive, stoMode, hypActive, angleMode,
  onAngleToggle, onMove, onHistory, onSettings, onCursorJump,
}: Props) {
  const scrollRef = useRef<HTMLDivElement>(null)
  const drag = useRef<{ x: number; y: number } | null>(null)
  const dragged = useRef(false)
  const innerRef = useRef<HTMLDivElement>(null)
  const [scale, setScale] = useState(1)
  const [boxHeight, setBoxHeight] = useState(0)
  const [copied, setCopied] = useState(false)
  const empty = expression.length === 0 && cursor.path.length === 0 && cursor.insertAt === 0
  const hasResult = result !== null
  const isRad = angleMode === 'RAD'

  // Track the room the expression has, which changes with the window and the result line.
  useEffect(() => {
    const box = scrollRef.current
    if (!box) return
    const observer = new ResizeObserver(() => setBoxHeight(box.clientHeight))
    observer.observe(box)
    return () => observer.disconnect()
  }, [])

  // Shrink the type until stacked fractions fit the display height. Height is linear in
  // font size (everything inside is sized in em), so one measurement gives the new scale.
  useLayoutEffect(() => {
    const inner = innerRef.current
    if (!inner || boxHeight === 0) return
    const natural = inner.offsetHeight / scale
    if (natural <= 0) return
    const next = Math.max(0.3, Math.min(1, boxHeight / natural))
    if (Math.abs(next - scale) > 0.01) setScale(next)
  }, [expression, cursor, boxHeight, scale])

  // Keep the caret visible when the expression is wider than the display.
  useEffect(() => {
    scrollRef.current?.querySelector('.cursor-blink')?.scrollIntoView({ inline: 'nearest', block: 'nearest' })
  }, [expression, cursor])

  // Dragging across the expression steps the caret, so it can enter and leave fractions.
  const STEP = 22
  function onPointerDown(e: React.PointerEvent) {
    drag.current = { x: e.clientX, y: e.clientY }
    dragged.current = false
  }
  function onPointerMove(e: React.PointerEvent) {
    const d = drag.current
    if (!d) return
    const dx = e.clientX - d.x
    const dy = e.clientY - d.y
    if (Math.abs(dx) >= STEP && Math.abs(dx) >= Math.abs(dy)) {
      dragged.current = true
      onMove(dx > 0 ? 'RIGHT' : 'LEFT')
      drag.current = { x: e.clientX, y: e.clientY }
    } else if (Math.abs(dy) >= STEP) {
      dragged.current = true
      onMove(dy > 0 ? 'DOWN' : 'UP')
      drag.current = { x: e.clientX, y: e.clientY }
    }
  }
  function endDrag() {
    drag.current = null
  }

  function handleCopy() {
    if (!result) return
    navigator.clipboard.writeText(result).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 1200)
    }).catch(() => { /* clipboard denied */ })
  }

  return (
    <div className="flex h-full flex-col px-5 pb-1 pt-1">
      <div className="flex items-center">
        <button
          onClick={onAngleToggle}
          aria-label={isRad ? 'Radians, switch to degrees' : 'Degrees, switch to radians'}
          className="min-h-12 min-w-12 select-none text-left text-[0.9375rem] font-semibold tracking-wide text-dim active:text-ink"
        >
          {isRad ? 'Rad' : 'Deg'}
        </button>
        {hypActive && <Badge label="hyperbolic" />}
        {stoMode && <Badge label="Store" />}
        {shiftActive && <Badge label="shift" />}
        <div className="flex-1" />
        <button aria-label="History" onClick={onHistory} className={ICON_BTN}>
          <svg viewBox="0 0 24 24" className="size-[22px]" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d="M3 12a9 9 0 1 0 3-6.7L3 8" /><path d="M3 3v5h5" /><path d="M12 7v5l3 2" />
          </svg>
        </button>
        <button aria-label="Settings" onClick={onSettings} className={ICON_BTN}>
          <svg viewBox="0 0 24 24" className="size-[22px]" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden>
            <path d="M4 7h10M18 7h2M4 17h2M10 17h10" /><circle cx="16" cy="7" r="2" /><circle cx="8" cy="17" r="2" />
          </svg>
        </button>
      </div>

      <div
        ref={scrollRef}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={endDrag}
        onClickCapture={(e) => { if (dragged.current) { e.stopPropagation(); dragged.current = false } }}
        onPointerCancel={endDrag}
        onPointerLeave={endDrag}
        aria-label="Expression. Drag to move the cursor"
        style={{ fontSize: `${2.75 * scale}rem` }}
        className="flex min-h-0 flex-1 touch-none items-end overflow-x-auto overflow-y-hidden font-light leading-none text-ink [scrollbar-width:none]"
      >
        <div ref={innerRef} className="ml-auto flex w-max items-center whitespace-nowrap py-[0.1em]">
          {empty
            ? <span className="text-dim">0</span>
            : <ASTRenderer nodes={expression} cursor={cursor} path={[]} onCursorJump={onCursorJump} />
          }
        </div>
      </div>

      <div className="flex h-9 items-center justify-end">
        {hasResult && (
          <button
            data-testid="result"
            onClick={handleCopy}
            aria-label={`Result ${result}, tap to copy`}
            className="max-w-full overflow-x-auto whitespace-nowrap select-none text-[1.5rem] font-light active:opacity-60"
          >
            <span className={copied ? 'text-accent' : 'text-dim'}>{copied ? 'Copied' : result}</span>
          </button>
        )}
      </div>
    </div>
  )
}
