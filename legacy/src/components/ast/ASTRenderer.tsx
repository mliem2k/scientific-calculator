import { ASTNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { NumberNodeView } from './nodes/NumberNode'
import { OperatorNodeView } from './nodes/OperatorNode'
import { ConstantNodeView } from './nodes/ConstantNode'
import { FractionNodeView } from './nodes/FractionNode'
import { ExponentNodeView } from './nodes/ExponentNode'
import { RadicalNodeView } from './nodes/RadicalNode'
import { FunctionNodeView } from './nodes/FunctionNode'
import { CursorView } from './Cursor'

type JumpFn = (path: CursorSegment[], insertAt: number) => void

interface Props {
  nodes: ASTNode[]
  cursor: Cursor
  path: CursorSegment[]
  onCursorJump?: JumpFn
}

function pathsEqual(a: CursorSegment[], b: CursorSegment[]): boolean {
  return a.length === b.length && a.every((s, i) => s.nodeIndex === b[i].nodeIndex && s.slot === b[i].slot)
}

export function ASTRenderer({ nodes, cursor, path, onCursorJump }: Props) {
  const isHere = pathsEqual(cursor.path, path)

  return (
    <span className="inline-flex items-center flex-wrap">
      {nodes.map((node, index) => (
        <span key={index} className="inline-flex items-center">
          {isHere && cursor.insertAt === index && <CursorView />}
          <NodeView node={node} cursor={cursor} path={path} nodeIndex={index} onCursorJump={onCursorJump} />
        </span>
      ))}
      {isHere && cursor.insertAt === nodes.length && <CursorView />}
    </span>
  )
}

function NodeView({ node, cursor, path, nodeIndex, onCursorJump }: {
  node: ASTNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number; onCursorJump?: JumpFn
}) {
  const jump = () => onCursorJump?.(path, nodeIndex + 1)

  switch (node.type) {
    case 'number':
      return <span className="cursor-pointer select-none" onClick={jump}><NumberNodeView node={node} /></span>
    case 'operator':
      return <span className="cursor-pointer select-none" onClick={jump}><OperatorNodeView node={node} /></span>
    case 'constant':
      return <span className="cursor-pointer select-none" onClick={jump}><ConstantNodeView node={node} /></span>
    case 'fraction':
      return <FractionNodeView node={node} cursor={cursor} path={path} nodeIndex={nodeIndex} onCursorJump={onCursorJump} />
    case 'exponent':
      return <ExponentNodeView node={node} cursor={cursor} path={path} nodeIndex={nodeIndex} onCursorJump={onCursorJump} />
    case 'radical':
      return <RadicalNodeView  node={node} cursor={cursor} path={path} nodeIndex={nodeIndex} onCursorJump={onCursorJump} />
    case 'function':
      return <FunctionNodeView node={node} cursor={cursor} path={path} nodeIndex={nodeIndex} onCursorJump={onCursorJump} />
    case 'factorial':
      return (
        <span className="inline-flex items-center">
          <ASTRenderer nodes={node.operand} cursor={cursor} path={[...path, { nodeIndex, slot: 'operand' }]} onCursorJump={onCursorJump} />
          <span>!</span>
        </span>
      )
    case 'ncr':
      return (
        <span className="inline-flex items-baseline gap-0.5">
          <ASTRenderer nodes={node.n} cursor={cursor} path={[...path, { nodeIndex, slot: 'n' }]} onCursorJump={onCursorJump} />
          <span className="text-[0.65em]">C</span>
          <ASTRenderer nodes={node.r} cursor={cursor} path={[...path, { nodeIndex, slot: 'r' }]} onCursorJump={onCursorJump} />
        </span>
      )
    case 'npr':
      return (
        <span className="inline-flex items-baseline gap-0.5">
          <ASTRenderer nodes={node.n} cursor={cursor} path={[...path, { nodeIndex, slot: 'n' }]} onCursorJump={onCursorJump} />
          <span className="text-[0.65em]">P</span>
          <ASTRenderer nodes={node.r} cursor={cursor} path={[...path, { nodeIndex, slot: 'r' }]} onCursorJump={onCursorJump} />
        </span>
      )
    case 'paren':
      return (
        <span className="inline-flex items-center">
          <span>(</span>
          <ASTRenderer nodes={node.children} cursor={cursor} path={[...path, { nodeIndex, slot: 'children' }]} onCursorJump={onCursorJump} />
          <span>)</span>
        </span>
      )
  }
}
