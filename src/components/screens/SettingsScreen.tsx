import { useState, type ReactNode } from 'react'
import { updateSettings, useSettings } from '@/lib/settings'
import { THEMES, THEME_IDS } from '@/lib/themes'
import { applyTheme } from '@/lib/themes'
import { buildVersionString, checkForUpdate, UpdateInfo } from '@/lib/updater'
import { cn } from '@/lib/utils'
import { ScreenShell } from './ScreenShell'
import { UpdateDialog } from './UpdateDialog'

const REPO_URL = 'https://github.com/mliem2k/scientific-calculator'
const PRECISIONS = [0, 4, 6, 8, 10]

function Section({ label, children }: { label: string; children: ReactNode }) {
  return (
    <section className="mb-2">
      <h2 className="px-4 pb-2 pt-6 text-[0.8125rem] font-semibold tracking-[0.08em] text-op">{label}</h2>
      {children}
    </section>
  )
}

const ROW = 'flex min-h-14 w-full items-center justify-between gap-3 bg-key px-4 py-2 text-left'

function Toggle({ label, hint, checked, onChange }: { label: string; hint: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <button role="switch" aria-checked={checked} onClick={() => onChange(!checked)} className={ROW}>
      <span>
        <span className="block text-expr">{label}</span>
        <span className="block text-sm text-result">{hint}</span>
      </span>
      <span className={cn('flex h-7 w-12 shrink-0 items-center rounded-full px-0.5 transition-colors', checked ? 'bg-shift' : 'bg-keyline')}>
        <span className={cn('size-6 rounded-full bg-expr transition-transform', checked && 'translate-x-5')} />
      </span>
    </button>
  )
}

export function SettingsScreen({ onBack }: { onBack: () => void }) {
  const settings = useSettings()
  const [checking, setChecking] = useState(false)
  const [dialog, setDialog] = useState<{ info: UpdateInfo | null; failed?: boolean } | null>(null)

  async function handleCheck() {
    setChecking(true)
    try {
      setDialog({ info: await checkForUpdate() })
    } catch {
      setDialog({ info: null, failed: true })
    }
    setChecking(false)
  }

  return (
    <ScreenShell title="Settings" onBack={onBack}>
      <Section label="APPEARANCE">
        <div className="grid grid-cols-2 gap-2 px-4" role="radiogroup" aria-label="Theme">
          {THEME_IDS.map((id) => {
            const { name, colors } = THEMES[id]
            const active = settings.themeId === id
            return (
              <button
                key={id}
                role="radio"
                aria-checked={active}
                onClick={() => { updateSettings({ themeId: id }); applyTheme(id) }}
                className="relative flex min-h-16 flex-col items-start justify-center gap-1.5 rounded-lg px-3 py-2 outline outline-1 outline-keyline"
                style={{ background: colors.background, color: colors.expressionText }}
              >
                <span className="text-sm font-semibold">{name}</span>
                <span className="flex gap-1" aria-hidden>
                  {[colors.buttonBg, colors.opText, colors.eqBg].map((c, i) => (
                    <span key={i} className="size-4 rounded-[3px]" style={{ background: c, outline: `1px solid ${colors.buttonBorder}` }} />
                  ))}
                </span>
                {active && (
                  <svg viewBox="0 0 24 24" className="absolute right-2 top-2 size-[18px]" fill="none" stroke={colors.shiftActive} strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" aria-label="Selected">
                    <path d="M5 12l5 5 9-10" />
                  </svg>
                )}
              </button>
            )
          })}
        </div>
      </Section>

      <Section label="CALCULATOR">
        <div className={ROW}>
          <span id="angle-label">Default angle mode</span>
          <div role="radiogroup" aria-labelledby="angle-label" className="flex overflow-hidden rounded-lg outline outline-1 outline-keyline">
            {(['DEG', 'RAD'] as const).map((mode) => (
              <button
                key={mode}
                role="radio"
                aria-checked={settings.defaultAngleMode === mode}
                onClick={() => updateSettings({ defaultAngleMode: mode })}
                className={cn('min-h-12 min-w-16 px-3 text-sm font-semibold', settings.defaultAngleMode === mode ? 'bg-shift text-on-eq' : 'bg-key text-expr')}
              >
                {mode}
              </button>
            ))}
          </div>
        </div>
        <label className={ROW}>
          <span>Result precision</span>
          <select
            value={settings.resultPrecision}
            onChange={(e) => updateSettings({ resultPrecision: Number(e.target.value) })}
            className="min-h-12 rounded-lg bg-key px-2 text-expr outline outline-1 outline-keyline"
          >
            {PRECISIONS.map((p) => (
              <option key={p} value={p}>{p === 0 ? 'Auto' : `${p} dec`}</option>
            ))}
          </select>
        </label>
      </Section>

      <Section label="DISPLAY">
        <Toggle label="Show shift labels" hint="Display secondary function labels on buttons" checked={settings.showShiftLabels} onChange={(v) => updateSettings({ showShiftLabels: v })} />
      </Section>

      <Section label="BEHAVIOR">
        <Toggle label="Haptic feedback" hint="Vibrate on button press" checked={settings.hapticFeedback} onChange={(v) => updateSettings({ hapticFeedback: v })} />
      </Section>

      <Section label="ABOUT">
        <div className="flex items-center gap-3.5 px-4 py-3">
          <img src="/icon.png" alt="" width={56} height={56} className="rounded-xl" />
          <div>
            <p className="text-base font-semibold">Scientific Calculator</p>
            <p className="text-[0.8125rem] text-dim">{buildVersionString()}</p>
          </div>
        </div>
        <button className={ROW} onClick={handleCheck} disabled={checking}>
          <span>
            <span className="block text-expr">Check for updates</span>
            <span className="block text-sm text-result">{checking ? 'Checking...' : 'Compare with latest GitHub release'}</span>
          </span>
        </button>
        <a className={ROW} href={REPO_URL} target="_blank" rel="noopener noreferrer">
          <span>
            <span className="block text-expr">Source code</span>
            <span className="block text-sm text-result">github.com/mliem2k/scientific-calculator</span>
          </span>
        </a>
      </Section>
      <div className="h-6" />

      {dialog && <UpdateDialog info={dialog.info} failed={dialog.failed} onClose={() => setDialog(null)} />}
    </ScreenShell>
  )
}
