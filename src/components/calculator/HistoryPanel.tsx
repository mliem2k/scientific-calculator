import { useState } from 'react'
import { HistoryEntry } from '@/lib/ast/types'

interface Props {
  history: HistoryEntry[]
  onRestore: (entry: HistoryEntry) => void
}

export function HistoryPanel({ history, onRestore }: Props) {
  const [open, setOpen] = useState(false)
  if (history.length === 0) return null

  return (
    <div className="bg-zinc-950 border-b border-zinc-800/60 flex-shrink-0">
      <button
        onClick={() => setOpen(o => !o)}
        className="w-full flex items-center justify-between px-4 py-1.5 text-zinc-600 text-[11px] hover:text-zinc-400 transition-colors"
      >
        <span>{open ? '▲' : '▾'} History ({history.length})</span>
        {open && <span className="text-[9px] text-zinc-700">tap to restore</span>}
      </button>
      {open && (
        <div className="max-h-28 overflow-y-auto divide-y divide-zinc-900">
          {history.map(entry => (
            <button
              key={entry.id}
              onClick={() => onRestore(entry)}
              className="w-full text-right px-4 py-1.5 hover:bg-zinc-900 transition-colors"
            >
              <span className="font-display text-sm text-zinc-300">{entry.result}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
