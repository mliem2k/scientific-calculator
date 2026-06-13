import { ExponentNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'

type JumpFn = (path: CursorSegment[], insertAt: number) => void
interface Props { node: ExponentNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number; onCursorJump?: JumpFn }

export function ExponentNodeView({ node, cursor, path, nodeIndex, onCursorJump }: Props) {
  return (
    <span className="inline-flex items-start">
      <ASTRenderer nodes={node.base} cursor={cursor} path={[...path, { nodeIndex, slot: 'base' }]} onCursorJump={onCursorJump} />
      <span className="text-[0.65em] -mt-1 ml-px">
        <ASTRenderer nodes={node.exponent} cursor={cursor} path={[...path, { nodeIndex, slot: 'exponent' }]} onCursorJump={onCursorJump} />
      </span>
    </span>
  )
}
