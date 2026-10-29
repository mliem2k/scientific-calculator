import type { ReactNode } from 'react'

interface Props {
  title: string
  onBack: () => void
  /** Optional trailing header control, for example Clear. */
  action?: ReactNode
  children: ReactNode
}

export function ScreenShell({ title, onBack, action, children }: Props) {
  return (
    <div
      className="flex h-dvh flex-col bg-bg text-ink"
      style={{ paddingTop: 'env(safe-area-inset-top)', paddingBottom: 'env(safe-area-inset-bottom)' }}
    >
      <header className="flex h-14 shrink-0 items-center px-2">
        <button aria-label="Back" onClick={onBack} className="flex h-12 items-center gap-0.5 rounded-full pl-1 pr-3 text-accent">
          <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d="M15 5l-7 7 7 7" />
          </svg>
          <span className="text-[1.0625rem]">Calculator</span>
        </button>
        <div className="flex-1" />
        {action}
      </header>
      <main className="min-h-0 flex-1 overflow-y-auto px-4 pb-8">
        <h1 className="px-1 pb-3 pt-1 text-[2rem] font-bold tracking-tight">{title}</h1>
        {children}
      </main>
    </div>
  )
}
