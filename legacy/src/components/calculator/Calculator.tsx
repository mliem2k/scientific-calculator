import { useCalculator } from '@/hooks/useCalculator'
import { useKeyboard } from '@/hooks/useKeyboard'
import { Display } from './Display'
import { ButtonGrid } from './ButtonGrid'
import { HistoryPanel } from './HistoryPanel'

export function Calculator() {
  const { state, handleButton, handleCursorJump, handlePaste, handleRestore } = useCalculator()
  useKeyboard(handleButton)

  return (
    <div className="flex flex-col h-dvh max-w-sm mx-auto bg-black overflow-hidden">
      <div className="flex-shrink-0 bg-[#080c08] border-b-2 border-zinc-800">
        <Display
          expression={state.expression}
          cursor={state.cursor}
          result={state.result}
          shiftActive={state.shiftActive}
          stoMode={state.stoMode}
          hypActive={state.hypActive}
          angleMode={state.angleMode}
          onShiftToggle={() => handleButton('SHIFT')}
          onCopy={() => {}}
          onAngleToggle={() => handleButton('DEG_RAD')}
          onCursorJump={handleCursorJump}
        />
      </div>

      <HistoryPanel history={state.history} onRestore={handleRestore} />

      <div
        className="flex-1 overflow-hidden flex flex-col justify-end"
        style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
      >
        <ButtonGrid
          shiftActive={state.shiftActive}
          onButton={handleButton}
          onPaste={handlePaste}
        />
      </div>
    </div>
  )
}
