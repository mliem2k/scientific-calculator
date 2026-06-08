import { FunctionNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'

const LABELS: Record<string, string> = {
  sin: 'sin', cos: 'cos', tan: 'tan',
  asin: 'sin⁻¹', acos: 'cos⁻¹', atan: 'tan⁻¹',
  sinh: 'sinh', cosh: 'cosh', tanh: 'tanh',
  asinh: 'sinh⁻¹', acosh: 'cosh⁻¹', atanh: 'tanh⁻¹',
  log: 'log', ln: 'ln', pow10: '10ˣ', exp: 'eˣ',
}

interface Props { node: FunctionNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number }

export function FunctionNodeView({ node, cursor, path, nodeIndex }: Props) {
  return (
    <span className="inline-flex items-center">
      <span className="text-sm opacity-90 mr-px">{LABELS[node.name] ?? node.name}(</span>
      <ASTRenderer nodes={node.argument} cursor={cursor} path={[...path, { nodeIndex, slot: 'argument' }]} />
      <span className="text-sm opacity-90 ml-px">)</span>
    </span>
  )
}
