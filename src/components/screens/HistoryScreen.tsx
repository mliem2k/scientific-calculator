import { HistoryEntry } from '@/lib/ast/types'
import { serialize } from '@/lib/ast/serializer'
import { ASTRenderer } from '../ast/ASTRenderer'
import { ScreenShell } from './ScreenShell'

interface Props {
  history: HistoryEntry[]
  onBack: () => void
  onClear: () => void
  onRestore: (entry: HistoryEntry) => void
}

// A path that matches no list, so the renderer draws the expression without a caret.
const NO_CURSOR = { path: [{ nodeIndex: -1, slot: 'none' }], insertAt: 0 }

export function HistoryScreen({ history, onBack, onClear, onRestore }: Props) {
  return (
    <ScreenShell
      title="History"
      onBack={onBack}
      action={history.length > 0 ? <button onClick={onClear} className="min-h-12 px-3 text-[1.0625rem] text-accent">Clear</button> : undefined}
    >
      {history.length === 0 ? (
        <p className="mt-24 text-center text-dim">No calculations yet</p>
      ) : (
        <ul className="divide-y divide-line overflow-hidden rounded-xl bg-surface">
          {history.map((entry) => (
            <li key={entry.id}>
              <button
                onClick={() => onRestore(entry)}
                aria-label={`Restore ${serialize(entry.expression)} equals ${entry.result}`}
                className="flex min-h-16 w-full flex-col items-end justify-center gap-1 px-4 py-3 text-right active:bg-line"
              >
                <span aria-hidden className="max-w-full overflow-x-auto whitespace-nowrap text-[1.375rem] font-light text-dim [scrollbar-width:none]">
                  <ASTRenderer nodes={entry.expression} cursor={NO_CURSOR} path={[]} />
                </span>
                <span aria-hidden className="max-w-full overflow-x-auto whitespace-nowrap text-[1.75rem] font-light">{entry.result}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </ScreenShell>
  )
}
