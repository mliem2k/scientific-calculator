import { useState, type ReactNode } from 'react'
import { updateSettings, useSettings } from '@/lib/settings'
import { THEMES, THEME_IDS, applyTheme } from '@/lib/themes'
import { buildVersionString, checkForUpdate, UpdateInfo } from '@/lib/updater'
import { cn } from '@/lib/utils'
import { ScreenShell } from './ScreenShell'
import { UpdateDialog } from './UpdateDialog'

const REPO_URL = 'https://github.com/mliem2k/scientific-calculator'
const PRECISIONS = [0, 4, 6, 8, 10]

function Section({ label, children }: { label: string; children: ReactNode }) {
  return (
    <section className="mb-6">
      <h2 className="px-4 pb-1.5 text-[0.8125rem] text-dim">{label}</h2>
      <div className="divide-y divide-line overflow-hidden rounded-xl bg-surface">{children}</div>
    </section>
  )
}

const ROW = 'flex min-h-14 w-full items-center justify-between gap-3 px-4 py-2 text-left'

function Toggle({ label, hint, checked, onChange }: { label: string; hint: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <button role="switch" aria-checked={checked} onClick={() => onChange(!checked)} className={ROW}>
      <span>
        <span className="block">{label}</span>
        <span className="block text-sm text-dim">{hint}</span>
      </span>
      <span className={cn('flex h-8 w-[3.25rem] shrink-0 items-center rounded-full p-0.5 transition-colors', checked ? 'bg-[#34c759]' : 'bg-line')}>
        <span className={cn('size-7 rounded-full bg-white shadow transition-transform', checked && 'translate-x-5')} />
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
      <section className="mb-6">
        <h2 className="px-4 pb-1.5 text-[0.8125rem] text-dim">Appearance</h2>
        <div className="grid grid-cols-3 gap-3" role="radiogroup" aria-label="Theme">
          {THEME_IDS.map((id) => {
            const { name, colors } = THEMES[id]
            const active = settings.themeId === id
            return (
              <button
                key={id}
                role="radio"
                aria-checked={active}
                onClick={() => { updateSettings({ themeId: id }); applyTheme(id) }}
                className="flex flex-col items-center gap-2 rounded-xl bg-surface px-2 pb-3 pt-3"
              >
                <span className="flex size-[4.5rem] flex-col justify-end gap-1 rounded-xl p-2" style={{ background: colors.background, outline: `1px solid ${colors.line}` }} aria-hidden>
                  <span className="flex gap-1">
                    <span className="size-3 rounded-full" style={{ background: colors.utilBg }} />
                    <span className="size-3 rounded-full" style={{ background: colors.digitBg, outline: `1px solid ${colors.line}` }} />
                    <span className="size-3 rounded-full" style={{ background: colors.opBg }} />
                  </span>
                </span>
                <span className={cn('text-sm', active ? 'font-semibold text-accent' : 'text-ink')}>{name}</span>
              </button>
            )
          })}
        </div>
      </section>

      <Section label="Calculator">
        <div className={ROW}>
          <span id="angle-label">Default angle mode</span>
          <div role="radiogroup" aria-labelledby="angle-label" className="flex rounded-lg bg-line p-0.5">
            {(['DEG', 'RAD'] as const).map((mode) => (
              <button
                key={mode}
                role="radio"
                aria-checked={settings.defaultAngleMode === mode}
                onClick={() => updateSettings({ defaultAngleMode: mode })}
                className={cn('min-h-11 min-w-16 rounded-[0.4rem] px-3 text-sm font-medium', settings.defaultAngleMode === mode ? 'bg-surface text-ink shadow' : 'text-dim')}
              >
                {mode === 'DEG' ? 'Degrees' : 'Radians'}
              </button>
            ))}
          </div>
        </div>
        <label className={ROW}>
          <span>Result precision</span>
          <select
            value={settings.resultPrecision}
            onChange={(e) => updateSettings({ resultPrecision: Number(e.target.value) })}
            className="min-h-11 rounded-lg bg-transparent px-1 text-right text-dim"
          >
            {PRECISIONS.map((p) => (
              <option key={p} value={p}>{p === 0 ? 'Automatic' : `${p} decimals`}</option>
            ))}
          </select>
        </label>
        <Toggle label="Haptic feedback" hint="Vibrate on key press" checked={settings.hapticFeedback} onChange={(v) => updateSettings({ hapticFeedback: v })} />
      </Section>

      <Section label="About">
        <div className="flex items-center gap-3.5 px-4 py-3">
          <img src="/icon.png" alt="" width={56} height={56} className="rounded-[0.9rem]" />
          <div>
            <p className="text-base font-semibold">Scientific Calculator</p>
            <p className="text-[0.8125rem] text-dim">{buildVersionString()}</p>
          </div>
        </div>
        <button className={ROW} onClick={handleCheck} disabled={checking}>
          <span>
            <span className="block">Check for updates</span>
            <span className="block text-sm text-dim">{checking ? 'Checking...' : 'Compare with the latest GitHub release'}</span>
          </span>
        </button>
        <a className={ROW} href={REPO_URL} target="_blank" rel="noopener noreferrer">
          <span>
            <span className="block">Source code</span>
            <span className="block text-sm text-dim">github.com/mliem2k/scientific-calculator</span>
          </span>
        </a>
      </Section>

      {dialog && <UpdateDialog info={dialog.info} failed={dialog.failed} onClose={() => setDialog(null)} />}
    </ScreenShell>
  )
}
