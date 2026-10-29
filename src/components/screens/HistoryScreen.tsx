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
        <p className="mt-24 text-center text-dim">No history yet</p>
      ) : (
        <ul>
          {history.map((entry) => (
            <li key={entry.id}>
              <button
                onClick={() => onRestore(entry)}
                className="flex min-h-14 w-full flex-col items-end justify-center gap-0.5 px-4 py-2 text-right active:bg-keyline"
              >
                <span className="font-mono-display text-xl text-result">{entry.result}</span>
                <span className="max-w-full truncate text-[0.9375rem] text-dim">{serialize(entry.expression)}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </ScreenShell>
  )
}
