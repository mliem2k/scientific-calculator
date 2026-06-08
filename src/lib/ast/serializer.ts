import { ASTNode } from './types'

export function serialize(nodes: ASTNode[]): string {
  return nodes.map(serializeNode).join('')
}

function serializeNode(node: ASTNode): string {
  switch (node.type) {
    case 'number':   return node.value
    case 'operator': return node.op === '×' ? '*' : node.op === '÷' ? '/' : node.op
    case 'constant': return node.name === 'Ans' ? 'ans' : node.name
    case 'fraction': return `(${serialize(node.numerator)})/(${serialize(node.denominator)})`
    case 'exponent': return `(${serialize(node.base)})^(${serialize(node.exponent)})`
    case 'radical':
      return node.degree.length === 0
        ? `sqrt(${serialize(node.radicand)})`
        : `nthRoot(${serialize(node.radicand)}, ${serialize(node.degree)})`
    case 'function':
      if (node.name === 'log')   return `log(${serialize(node.argument)}, 10)`
      if (node.name === 'ln')    return `log(${serialize(node.argument)})`
      if (node.name === 'pow10') return `10^(${serialize(node.argument)})`
      if (node.name === 'exp')   return `e^(${serialize(node.argument)})`
      return `${node.name}(${serialize(node.argument)})`
    case 'factorial': return `factorial(${serialize(node.operand)})`
    case 'ncr':       return `combinations(${serialize(node.n)}, ${serialize(node.r)})`
    case 'npr':       return `permutations(${serialize(node.n)}, ${serialize(node.r)})`
    case 'paren':     return `(${serialize(node.children)})`
  }
}
