import { useCalculator } from '@/hooks/useCalculator'
import { useKeyboard } from '@/hooks/useKeyboard'
import { Display } from './Display'
import { ButtonGrid } from './ButtonGrid'
import { HistoryPanel } from './HistoryPanel'

export function Calculator() {
  const { state, handleButton, handleRestore } = useCalculator()
  useKeyboard(handleButton)

  return (
    <div className="flex flex-col h-dvh max-w-sm mx-auto bg-black overflow-hidden">
      <div className="flex items-center justify-between px-3 py-1 bg-zinc-950 text-zinc-500 text-xs flex-shrink-0">
        <span className={state.angleMode === 'DEG' ? 'text-zinc-300' : 'text-blue-400'}>
          {state.angleMode}
        </span>
        <span className="flex gap-2">
          {state.hypActive   && <span className="text-blue-400">HYP</span>}
          {state.shiftActive && <span className="text-orange-400">SHIFT</span>}
        </span>
      </div>

      <Display expression={state.expression} cursor={state.cursor} result={state.result} />

      <HistoryPanel history={state.history} onRestore={handleRestore} />

      <div className="flex-1 overflow-hidden flex flex-col justify-end">
        <ButtonGrid
          angleMode={state.angleMode}
          shiftActive={state.shiftActive}
          hypActive={state.hypActive}
          onButton={handleButton}
        />
      </div>
    </div>
  )
}
