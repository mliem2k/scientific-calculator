import { FractionNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'
import { cn } from '@/lib/utils'

type JumpFn = (path: CursorSegment[], insertAt: number) => void
interface Props { node: FractionNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number; onCursorJump?: JumpFn }

export function FractionNodeView({ node, cursor, path, nodeIndex, onCursorJump }: Props) {
  return (
    <span className={cn('inline-flex flex-col items-center mx-1 align-middle', path.length > 0 && 'text-[0.85em]')}>
      <span className="px-1 min-w-4 text-center leading-tight">
        <ASTRenderer nodes={node.numerator} cursor={cursor} path={[...path, { nodeIndex, slot: 'numerator' }]} onCursorJump={onCursorJump} />
      </span>
      <span className="h-[0.045em] min-h-px w-full bg-current" />
      <span className="px-1 min-w-4 text-center leading-tight">
        <ASTRenderer nodes={node.denominator} cursor={cursor} path={[...path, { nodeIndex, slot: 'denominator' }]} onCursorJump={onCursorJump} />
      </span>
    </span>
  )
}
