import { ExponentNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'

interface Props { node: ExponentNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number }

export function ExponentNodeView({ node, cursor, path, nodeIndex }: Props) {
  return (
    <span className="inline-flex items-start">
      <ASTRenderer nodes={node.base} cursor={cursor} path={[...path, { nodeIndex, slot: 'base' }]} />
      <span className="text-[0.65em] -mt-1 ml-px">
        <ASTRenderer nodes={node.exponent} cursor={cursor} path={[...path, { nodeIndex, slot: 'exponent' }]} />
      </span>
    </span>
  )
}
