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
    <div className="bg-zinc-950 border-t border-zinc-800 flex-shrink-0">
      <button
        onClick={() => setOpen(o => !o)}
        className="w-full flex items-center px-3 py-1.5 text-zinc-500 text-xs hover:text-zinc-300"
      >
        {open ? '▲' : '▾'} History ({history.length})
      </button>
      {open && (
        <div className="max-h-28 overflow-y-auto">
          {history.map(entry => (
            <button
              key={entry.id}
              onClick={() => onRestore(entry)}
              className="w-full text-right px-3 py-1 text-sm text-zinc-300 hover:bg-zinc-800 border-t border-zinc-900"
            >
              {entry.result}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
