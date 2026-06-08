import { ConstantNode } from '@/lib/ast/types'
const LABELS: Record<string, string> = { pi: 'π', e: 'e', Ans: 'Ans' }
export function ConstantNodeView({ node }: { node: ConstantNode }) {
  return <span className="italic">{LABELS[node.name]}</span>
}
