import { ASTNode, Cursor } from './types'

function getSlot(node: ASTNode, slot: string): ASTNode[] | null {
  const n = node as any
  return (n[slot] as ASTNode[]) ?? null
}

function getList(root: ASTNode[], path: Cursor['path']): ASTNode[] {
  let cur: ASTNode[] = root
  for (const seg of path) {
    cur = getSlot(cur[seg.nodeIndex], seg.slot)!
  }
  return cur
}

function parentList(root: ASTNode[], cursor: Cursor): ASTNode[] {
  return getList(root, cursor.path.slice(0, -1))
}

export function moveCursorRight(root: ASTNode[], cursor: Cursor): Cursor {
  const list = getList(root, cursor.path)

  if (cursor.insertAt < list.length) {
    const next = list[cursor.insertAt]
    if (next.type === 'fraction') {
      return { path: [...cursor.path, { nodeIndex: cursor.insertAt, slot: 'numerator' }], insertAt: 0 }
    }
    if (next.type === 'exponent') {
      return { path: [...cursor.path, { nodeIndex: cursor.insertAt, slot: 'base' }], insertAt: 0 }
    }
    if (next.type === 'radical') {
      return { path: [...cursor.path, { nodeIndex: cursor.insertAt, slot: 'radicand' }], insertAt: 0 }
    }
    if (next.type === 'paren') {
      return { path: [...cursor.path, { nodeIndex: cursor.insertAt, slot: 'children' }], insertAt: 0 }
    }
    return { path: cursor.path, insertAt: cursor.insertAt + 1 }
  }

  if (cursor.path.length === 0) return cursor
  const parentSeg = cursor.path[cursor.path.length - 1]
  const parentPath = cursor.path.slice(0, -1)

  if (parentSeg.slot === 'numerator') {
    return { path: [...parentPath, { nodeIndex: parentSeg.nodeIndex, slot: 'denominator' }], insertAt: 0 }
  }
  if (parentSeg.slot === 'base') {
    return { path: [...parentPath, { nodeIndex: parentSeg.nodeIndex, slot: 'exponent' }], insertAt: 0 }
  }
  return { path: parentPath, insertAt: parentSeg.nodeIndex + 1 }
}

export function moveCursorLeft(root: ASTNode[], cursor: Cursor): Cursor {
  if (cursor.insertAt > 0) {
    const list = getList(root, cursor.path)
    const prev = list[cursor.insertAt - 1]
    if (prev.type === 'fraction') {
      return {
        path: [...cursor.path, { nodeIndex: cursor.insertAt - 1, slot: 'denominator' }],
        insertAt: prev.denominator.length,
      }
    }
    if (prev.type === 'exponent') {
      return {
        path: [...cursor.path, { nodeIndex: cursor.insertAt - 1, slot: 'exponent' }],
        insertAt: prev.exponent.length,
      }
    }
    if (prev.type === 'radical') {
      return {
        path: [...cursor.path, { nodeIndex: cursor.insertAt - 1, slot: 'radicand' }],
        insertAt: prev.radicand.length,
      }
    }
    if (prev.type === 'paren') {
      return {
        path: [...cursor.path, { nodeIndex: cursor.insertAt - 1, slot: 'children' }],
        insertAt: prev.children.length,
      }
    }
    return { path: cursor.path, insertAt: cursor.insertAt - 1 }
  }

  if (cursor.path.length === 0) return cursor
  const parentSeg = cursor.path[cursor.path.length - 1]
  const parentPath = cursor.path.slice(0, -1)

  if (parentSeg.slot === 'denominator') {
    const pList = parentList(root, cursor)
    const frac = pList[parentSeg.nodeIndex] as any
    return {
      path: [...parentPath, { nodeIndex: parentSeg.nodeIndex, slot: 'numerator' }],
      insertAt: frac.numerator.length,
    }
  }
  if (parentSeg.slot === 'exponent') {
    const pList = parentList(root, cursor)
    const exp = pList[parentSeg.nodeIndex] as any
    return {
      path: [...parentPath, { nodeIndex: parentSeg.nodeIndex, slot: 'base' }],
      insertAt: exp.base.length,
    }
  }
  return { path: parentPath, insertAt: parentSeg.nodeIndex }
}

export function moveCursorUp(root: ASTNode[], cursor: Cursor): Cursor {
  if (cursor.path.length === 0) return cursor
  const parentSeg = cursor.path[cursor.path.length - 1]
  const parentPath = cursor.path.slice(0, -1)

  if (parentSeg.slot === 'denominator') {
    const pList = getList(root, parentPath)
    const frac = pList[parentSeg.nodeIndex] as any
    return {
      path: [...parentPath, { nodeIndex: parentSeg.nodeIndex, slot: 'numerator' }],
      insertAt: Math.min(cursor.insertAt, frac.numerator.length),
    }
  }
  if (parentSeg.slot === 'exponent') {
    const pList = getList(root, parentPath)
    const exp = pList[parentSeg.nodeIndex] as any
    return {
      path: [...parentPath, { nodeIndex: parentSeg.nodeIndex, slot: 'base' }],
      insertAt: Math.min(cursor.insertAt, exp.base.length),
    }
  }
  return cursor
}

export function moveCursorDown(root: ASTNode[], cursor: Cursor): Cursor {
  if (cursor.path.length === 0) return cursor
  const parentSeg = cursor.path[cursor.path.length - 1]
  const parentPath = cursor.path.slice(0, -1)

  if (parentSeg.slot === 'numerator') {
    const pList = getList(root, parentPath)
    const frac = pList[parentSeg.nodeIndex] as any
    return {
      path: [...parentPath, { nodeIndex: parentSeg.nodeIndex, slot: 'denominator' }],
      insertAt: Math.min(cursor.insertAt, frac.denominator.length),
    }
  }
  if (parentSeg.slot === 'base') {
    const pList = getList(root, parentPath)
    const exp = pList[parentSeg.nodeIndex] as any
    return {
      path: [...parentPath, { nodeIndex: parentSeg.nodeIndex, slot: 'exponent' }],
      insertAt: Math.min(cursor.insertAt, exp.exponent.length),
    }
  }
  return cursor
}
