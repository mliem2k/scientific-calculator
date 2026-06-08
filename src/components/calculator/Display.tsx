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
    <div className="flex flex-col min-h-28 p-3 gap-2 bg-black border-b border-zinc-800 overflow-hidden">
      <div className="flex-1 overflow-x-auto flex items-center text-white text-2xl font-light">
        {empty
          ? <span className="text-zinc-700">0</span>
          : <ASTRenderer nodes={expression} cursor={cursor} path={[]} />
        }
      </div>
      {result !== null && (
        <button
          onClick={copyResult}
          className="text-right text-base select-none active:scale-95 transition-all"
          aria-label="copy result"
        >
          <span className={copied ? 'text-green-400' : 'text-zinc-400'}>
            {copied ? 'copied!' : `= ${result}`}
          </span>
        </button>
      )}
    </div>
  )
}
