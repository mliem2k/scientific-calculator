import { useState } from 'react'
import { ASTNode, Cursor } from '@/lib/ast/types'
import { ASTRenderer } from '../ast/ASTRenderer'

interface Props {
  expression: ASTNode[]
  cursor: Cursor
  result: string | null
}

export function Display({ expression, cursor, result }: Props) {
  const [copied, setCopied] = useState(false)
  const empty = expression.length === 0 && cursor.path.length === 0 && cursor.insertAt === 0

  function copyResult() {
    if (!result) return
    navigator.clipboard.writeText(result).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 1200)
    })
  }

  return (
    <div className="flex flex-col px-4 pb-3 pt-2 min-h-[7.5rem] overflow-hidden">
      {/* Expression — right-aligned, LCD font */}
      <div className="flex-1 overflow-x-auto flex items-center justify-end font-display text-white text-[1.75rem] leading-snug">
        {empty
          ? <span className="text-zinc-800">0</span>
          : <ASTRenderer nodes={expression} cursor={cursor} path={[]} />
        }
      </div>

      {/* Result line — tap to copy */}
      {result !== null && (
        <button
          onClick={copyResult}
          className="self-end mt-1 select-none active:opacity-60 transition-opacity"
          aria-label="copy result"
        >
          <span className={copied ? 'font-display text-base text-green-400' : 'font-display text-base text-zinc-500'}>
            {copied ? '✓ copied' : `= ${result}`}
          </span>
        </button>
      )}
    </div>
  )
}
