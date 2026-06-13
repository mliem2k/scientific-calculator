import { NumberNode } from '@/lib/ast/types'
export function NumberNodeView({ node }: { node: NumberNode }) {
  return <span>{node.value}</span>
}
