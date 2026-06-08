import {
  ASTNode, Cursor, FractionNode, ExponentNode, RadicalNode,
  FunctionNode, FactorialNode, NcrNode, NprNode, ParenNode, FunctionName, INITIAL_CURSOR,
} from './types'

function clone<T>(x: T): T { return JSON.parse(JSON.stringify(x)) }

function getList(root: ASTNode[], path: Cursor['path']): ASTNode[] {
  let cur: any[] = root
  for (const seg of path) {
    cur = (cur[seg.nodeIndex] as any)[seg.slot] as ASTNode[]
  }
  return cur
}

function spliceInsert(root: ASTNode[], cursor: Cursor, node: ASTNode): [ASTNode[], Cursor] {
  const r = clone(root)
  getList(r, cursor.path).splice(cursor.insertAt, 0, node)
  return [r, { path: cursor.path, insertAt: cursor.insertAt + 1 }]
}

export function insertDigit(root: ASTNode[], cursor: Cursor, digit: string): [ASTNode[], Cursor] {
  const r = clone(root)
  const list = getList(r, cursor.path)
  const prev = list[cursor.insertAt - 1]
  if (prev?.type === 'number') { prev.value += digit; return [r, cursor] }
  return spliceInsert(root, cursor, { type: 'number', value: digit })
}

export function insertDecimalPoint(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const r = clone(root)
  const list = getList(r, cursor.path)
  const prev = list[cursor.insertAt - 1]
  if (prev?.type === 'number') {
    if (!prev.value.includes('.')) prev.value += '.'
    return [r, cursor]
  }
  return spliceInsert(root, cursor, { type: 'number', value: '0.' })
}

export function insertOperator(root: ASTNode[], cursor: Cursor, op: '+' | '-' | '×' | '÷'): [ASTNode[], Cursor] {
  return spliceInsert(root, cursor, { type: 'operator', op })
}

export function insertFraction(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const [r, c] = spliceInsert(root, cursor, { type: 'fraction', numerator: [], denominator: [] } as FractionNode)
  return [r, { path: [...c.path, { nodeIndex: c.insertAt - 1, slot: 'numerator' }], insertAt: 0 }]
}

function wrapPrev(root: ASTNode[], cursor: Cursor, makeNode: (prev: ASTNode[]) => ASTNode): [ASTNode[], Cursor, number] {
  const r = clone(root)
  const list = getList(r, cursor.path)
  const hasPrev = cursor.insertAt > 0
  const prev = hasPrev ? list.splice(cursor.insertAt - 1, 1) : []
  const idx = hasPrev ? cursor.insertAt - 1 : cursor.insertAt
  list.splice(idx, 0, makeNode(prev))
  return [r, { path: cursor.path, insertAt: idx + 1 }, idx]
}

export function insertExponent(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const [r, c, idx] = wrapPrev(root, cursor, (prev) => ({ type: 'exponent', base: prev, exponent: [] } as ExponentNode))
  return [r, { path: [...c.path, { nodeIndex: idx, slot: 'exponent' }], insertAt: 0 }]
}

export function insertRadical(root: ASTNode[], cursor: Cursor, cubeRoot: boolean): [ASTNode[], Cursor] {
  const degree = cubeRoot ? [{ type: 'number', value: '3' } as ASTNode] : []
  const [r, c] = spliceInsert(root, cursor, { type: 'radical', degree, radicand: [] } as RadicalNode)
  return [r, { path: [...c.path, { nodeIndex: c.insertAt - 1, slot: 'radicand' }], insertAt: 0 }]
}

export function insertSquare(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const [r, c] = wrapPrev(root, cursor, (prev) => ({
    type: 'exponent', base: prev, exponent: [{ type: 'number', value: '2' }],
  } as ExponentNode))
  return [r, c]
}

export function insertFunction(root: ASTNode[], cursor: Cursor, name: FunctionName): [ASTNode[], Cursor] {
  const [r, c] = spliceInsert(root, cursor, { type: 'function', name, argument: [] } as FunctionNode)
  return [r, { path: [...c.path, { nodeIndex: c.insertAt - 1, slot: 'argument' }], insertAt: 0 }]
}

export function insertConstant(root: ASTNode[], cursor: Cursor, name: 'pi' | 'e' | 'Ans'): [ASTNode[], Cursor] {
  return spliceInsert(root, cursor, { type: 'constant', name })
}

export function insertFactorial(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const [r, c] = wrapPrev(root, cursor, (prev) => ({ type: 'factorial', operand: prev } as FactorialNode))
  return [r, c]
}

export function insertNcr(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const [r, c, idx] = wrapPrev(root, cursor, (prev) => ({ type: 'ncr', n: prev, r: [] } as NcrNode))
  return [r, { path: [...c.path, { nodeIndex: idx, slot: 'r' }], insertAt: 0 }]
}

export function insertNpr(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const [r, c, idx] = wrapPrev(root, cursor, (prev) => ({ type: 'npr', n: prev, r: [] } as NprNode))
  return [r, { path: [...c.path, { nodeIndex: idx, slot: 'r' }], insertAt: 0 }]
}

export function insertParen(root: ASTNode[], cursor: Cursor, side: 'open' | 'close'): [ASTNode[], Cursor] {
  if (side === 'open') {
    const [r, c] = spliceInsert(root, cursor, { type: 'paren', children: [] } as ParenNode)
    return [r, { path: [...c.path, { nodeIndex: c.insertAt - 1, slot: 'children' }], insertAt: 0 }]
  }
  if (cursor.path.length === 0) return [root, cursor]
  const parentPath = cursor.path.slice(0, -1)
  const lastSeg = cursor.path[cursor.path.length - 1]
  return [root, { path: parentPath, insertAt: lastSeg.nodeIndex + 1 }]
}

export function insertExp(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const r = clone(root)
  const list = getList(r, cursor.path)
  const opNode: ASTNode = { type: 'operator', op: '×' }
  const expNode: ExponentNode = {
    type: 'exponent',
    base: [{ type: 'number', value: '10' }],
    exponent: [],
  }
  list.splice(cursor.insertAt, 0, opNode, expNode)
  const expIdx = cursor.insertAt + 1
  return [r, { path: [...cursor.path, { nodeIndex: expIdx, slot: 'exponent' }], insertAt: 0 }]
}

export function insertNegate(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const r = clone(root)
  const list = getList(r, cursor.path)
  const prev = cursor.insertAt > 0 ? list[cursor.insertAt - 1] : null
  if (prev?.type === 'number') {
    prev.value = prev.value.startsWith('-') ? prev.value.slice(1) : '-' + prev.value
    return [r, cursor]
  }
  list.splice(cursor.insertAt, 0, { type: 'operator', op: '-' })
  return [r, { path: cursor.path, insertAt: cursor.insertAt + 1 }]
}

export function insertReciprocal(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const r = clone(root)
  const list = getList(r, cursor.path)
  const hasPrev = cursor.insertAt > 0
  const prev = hasPrev ? list.splice(cursor.insertAt - 1, 1) : []
  const idx = hasPrev ? cursor.insertAt - 1 : cursor.insertAt
  const frac: FractionNode = {
    type: 'fraction',
    numerator: [{ type: 'number', value: '1' }],
    denominator: prev,
  }
  list.splice(idx, 0, frac)
  if (hasPrev) {
    return [r, { path: cursor.path, insertAt: idx + 1 }]
  }
  return [r, { path: [...cursor.path, { nodeIndex: idx, slot: 'denominator' }], insertAt: 0 }]
}

export function deleteCurrent(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  if (cursor.insertAt === 0 && cursor.path.length === 0) return [root, cursor]
  const r = clone(root)
  if (cursor.insertAt === 0) {
    const parentPath = cursor.path.slice(0, -1)
    const lastSeg = cursor.path[cursor.path.length - 1]
    return [r, { path: parentPath, insertAt: lastSeg.nodeIndex }]
  }
  const list = getList(r, cursor.path)
  const node = list[cursor.insertAt - 1]
  if (node.type === 'number' && node.value.length > 1) {
    node.value = node.value.slice(0, -1)
    return [r, cursor]
  }
  list.splice(cursor.insertAt - 1, 1)
  return [r, { path: cursor.path, insertAt: cursor.insertAt - 1 }]
}

export function clearAll(): [ASTNode[], Cursor] {
  return [[], clone(INITIAL_CURSOR)]
}
