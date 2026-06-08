import { FractionNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'

interface Props { node: FractionNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number }

export function FractionNodeView({ node, cursor, path, nodeIndex }: Props) {
  return (
    <span className="inline-flex flex-col items-center mx-0.5 align-middle">
      <span className="px-1 min-w-4 text-center leading-tight">
        <ASTRenderer nodes={node.numerator} cursor={cursor} path={[...path, { nodeIndex, slot: 'numerator' }]} />
      </span>
      <span className="w-full border-t border-current" />
      <span className="px-1 min-w-4 text-center leading-tight">
        <ASTRenderer nodes={node.denominator} cursor={cursor} path={[...path, { nodeIndex, slot: 'denominator' }]} />
      </span>
    </span>
  )
}
