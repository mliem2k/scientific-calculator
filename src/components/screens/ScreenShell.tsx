import type { ReactNode } from 'react'

interface Props {
  title: string
  onBack: () => void
  children: ReactNode
}

export function ScreenShell({ title, onBack, children }: Props) {
  return (
    <div
      className="flex h-dvh flex-col bg-bg text-expr"
      style={{ paddingTop: 'env(safe-area-inset-top)', paddingBottom: 'env(safe-area-inset-bottom)' }}
    >
      <header className="flex h-14 shrink-0 items-center gap-1 px-1">
        <button aria-label="Back" onClick={onBack} className="flex size-12 items-center justify-center rounded-full">
          <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d="M15 5l-7 7 7 7" />
          </svg>
        </button>
        <h1 className="text-xl font-medium">{title}</h1>
      </header>
      <main className="min-h-0 flex-1 overflow-y-auto">{children}</main>
    </div>
  )
}
