import { OperatorNode } from '@/lib/ast/types'
const SYM: Record<string, string> = { '+': '+', '-': '−', '×': '×', '÷': '÷' }
export function OperatorNodeView({ node }: { node: OperatorNode }) {
  return <span className="px-0.5">{SYM[node.op] ?? node.op}</span>
}
