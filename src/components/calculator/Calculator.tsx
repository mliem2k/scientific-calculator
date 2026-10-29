import { useCallback, useEffect, useState } from 'react'
import { useCalculator } from '@/hooks/useCalculator'
import { useKeyboard } from '@/hooks/useKeyboard'
import { checkForUpdate, UpdateInfo } from '@/lib/updater'
import { Display } from './Display'
import { ButtonGrid } from './ButtonGrid'
import { HistoryScreen } from '../screens/HistoryScreen'
import { SettingsScreen } from '../screens/SettingsScreen'
import { UpdateDialog } from '../screens/UpdateDialog'

type View = 'calc' | 'history' | 'settings'

export function Calculator() {
  const { state, handleButton, handleCursorJump, handlePaste, handleRestore } = useCalculator()
  const [view, setView] = useState<View>('calc')
  const [update, setUpdate] = useState<UpdateInfo | null>(null)

  useKeyboard(handleButton, view === 'calc')

  useEffect(() => {
    checkForUpdate().then(setUpdate).catch(() => { /* offline: try again next launch */ })
  }, [])

  // Sub screens push a history entry so the Android back button returns to the calculator.
  const open = useCallback((next: View) => {
    history.pushState({ view: next }, '')
    setView(next)
  }, [])
  useEffect(() => {
    const onPop = () => setView('calc')
    window.addEventListener('popstate', onPop)
    return () => window.removeEventListener('popstate', onPop)
  }, [])
  const close = useCallback(() => history.back(), [])

  if (view === 'settings') return <SettingsScreen onBack={close} />
  if (view === 'history') {
    return (
      <HistoryScreen
        history={state.history}
        onBack={close}
        onRestore={(entry) => { handleRestore(entry); close() }}
      />
    )
  }

  return (
    <div
      className="flex h-dvh flex-col bg-bg short:flex-row"
      style={{ paddingBottom: 'env(safe-area-inset-bottom)', paddingTop: 'env(safe-area-inset-top)' }}
    >
      <div className="h-[30dvh] max-h-72 min-h-[12.5rem] shrink-0 short:h-auto short:max-h-none short:min-h-0 short:flex-[2]">
        <Display
          expression={state.expression}
          cursor={state.cursor}
          result={state.result}
          shiftActive={state.shiftActive}
          stoMode={state.stoMode}
          hypActive={state.hypActive}
          angleMode={state.angleMode}
          onAngleToggle={() => handleButton('DEG_RAD')}
          onMove={handleButton}
          onHistory={() => open('history')}
          onSettings={() => open('settings')}
          onCursorJump={handleCursorJump}
        />
      </div>
      <div className="min-h-0 flex-1 short:flex-[3]">
        <ButtonGrid shiftActive={state.shiftActive} onButton={handleButton} onPaste={handlePaste} />
      </div>
      {update && <UpdateDialog info={update} onClose={() => setUpdate(null)} />}
    </div>
  )
}
