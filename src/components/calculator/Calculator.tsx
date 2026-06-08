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
      {/* Unified display panel: status strip + expression area share the same background */}
      <div className="flex-shrink-0 bg-[#080c08] border-b-2 border-zinc-800">
        {/* Status strip */}
        <div className="flex items-center justify-between px-4 pt-2 pb-0">
          <div className="flex gap-2.5 items-center">
            <span className={
              state.angleMode === 'RAD'
                ? 'text-[10px] font-display text-blue-400 tracking-widest'
                : 'text-[10px] font-display text-zinc-600 tracking-widest'
            }>
              {state.angleMode}
            </span>
            {state.hypActive && (
              <span className="text-[10px] font-display text-indigo-400 tracking-widest">HYP</span>
            )}
            {state.shiftActive && (
              <span className="text-[10px] font-display text-orange-400 tracking-widest">SHIFT</span>
            )}
          </div>
          <span className="text-[9px] text-zinc-800 tracking-widest uppercase">sci calc</span>
        </div>
        <Display expression={state.expression} cursor={state.cursor} result={state.result} />
      </div>

      <HistoryPanel history={state.history} onRestore={handleRestore} />

      <div
        className="flex-1 overflow-hidden flex flex-col justify-end"
        style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
      >
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
