import { ASTNode, Cursor } from '@/lib/ast/types'
import { ASTRenderer } from '../ast/ASTRenderer'

interface Props {
  expression: ASTNode[]
  cursor: Cursor
  result: string | null
}

export function Display({ expression, cursor, result }: Props) {
  const empty = expression.length === 0 && cursor.path.length === 0 && cursor.insertAt === 0

  return (
    <div className="flex flex-col min-h-28 p-3 gap-2 bg-black border-b border-zinc-800 overflow-hidden">
      <div className="flex-1 overflow-x-auto flex items-center text-white text-2xl font-light">
        {empty
          ? <span className="text-zinc-700">0</span>
          : <ASTRenderer nodes={expression} cursor={cursor} path={[]} />
        }
      </div>
      {result !== null && (
        <div className="text-right text-zinc-400 text-base">
          = {result}
        </div>
      )}
    </div>
  )
}
