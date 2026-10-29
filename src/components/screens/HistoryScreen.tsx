import { HistoryEntry } from '@/lib/ast/types'
import { serialize } from '@/lib/ast/serializer'
import { ScreenShell } from './ScreenShell'

interface Props {
  history: HistoryEntry[]
  onBack: () => void
  onRestore: (entry: HistoryEntry) => void
}

export function HistoryScreen({ history, onBack, onRestore }: Props) {
  return (
    <ScreenShell title="History" onBack={onBack}>
      {history.length === 0 ? (
        <p className="mt-24 text-center text-dim">No calculations yet</p>
      ) : (
        <ul className="divide-y divide-line overflow-hidden rounded-xl bg-surface">
          {history.map((entry) => (
            <li key={entry.id}>
              <button
                onClick={() => onRestore(entry)}
                className="flex min-h-16 w-full flex-col items-end justify-center gap-1 px-4 py-3 text-right active:bg-line"
              >
                <span className="max-w-full truncate text-[0.9375rem] text-dim">{serialize(entry.expression)}</span>
                <span className="text-[1.5rem] font-light">{entry.result}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </ScreenShell>
  )
}
