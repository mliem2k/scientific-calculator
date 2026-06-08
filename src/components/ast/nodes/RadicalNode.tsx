import { RadicalNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'

interface Props { node: RadicalNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number }

export function RadicalNodeView({ node, cursor, path, nodeIndex }: Props) {
  return (
    <span className="inline-flex items-end">
      {node.degree.length > 0 && (
        <span className="text-[0.65em] mb-1 mr-px">
          <ASTRenderer nodes={node.degree} cursor={cursor} path={[...path, { nodeIndex, slot: 'degree' }]} />
        </span>
      )}
      <span className="text-xl leading-none">√</span>
      <span className="border-t border-current px-0.5">
        <ASTRenderer nodes={node.radicand} cursor={cursor} path={[...path, { nodeIndex, slot: 'radicand' }]} />
      </span>
    </span>
  )
}
