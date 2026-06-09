# Reference Layout Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the calculator from a 4-column DPad layout to a 5-column reference layout with memory slots (A/B/X/Y), mixed fractions, absolute value, and tap-on-display cursor positioning.

**Architecture:** The AST, evaluator, and builder layers gain new capabilities (memory variable constants, cube, nth-root, mixed-number, paste). The hook gains memory + stoMode state and a cursor-jump handler exposed separately from handleButton. UI: Display gets a left-side header strip (SHIFT/share/angle) and tap-to-position; ButtonGrid is rewritten to 5 columns; DPad.tsx is deleted.

**Tech Stack:** React 18, TypeScript, Tailwind CSS, mathjs, Vitest + Testing Library

---

## File Map

| File | Action |
|---|---|
| `src/lib/ast/types.ts` | Modify — extend ConstantNode.name, FunctionName, CalculatorState |
| `src/lib/ast/builder.ts` | Modify — add insertCube, insertNthRadical, insertMixed, insertNumberLiteral |
| `src/lib/ast/builder.test.ts` | Modify — tests for new builders |
| `src/lib/evaluator.ts` | Modify — memory scope param, toMixed, fromMixed exports |
| `src/lib/evaluator.test.ts` | Modify — tests for memory eval and fraction helpers |
| `src/hooks/useCalculator.ts` | Modify — memory/stoMode state, all new handlers, handleCursorJump, handlePaste |
| `src/hooks/useCalculator.test.ts` | Modify — tests for Sto/A-Y, CURSOR_JUMP, new actions |
| `src/components/ast/ASTRenderer.tsx` | Modify — thread onCursorJump optional prop |
| `src/components/ast/nodes/FractionNode.tsx` | Modify — accept + forward onCursorJump |
| `src/components/ast/nodes/ExponentNode.tsx` | Modify — accept + forward onCursorJump |
| `src/components/ast/nodes/RadicalNode.tsx` | Modify — accept + forward onCursorJump |
| `src/components/ast/nodes/FunctionNode.tsx` | Modify — accept + forward onCursorJump + add abs label |
| `src/components/ast/nodes/ConstantNode.tsx` | Modify — add A/B/X/Y labels |
| `src/components/calculator/ButtonGrid.tsx` | Rewrite — 5-column, no DPad, new rows |
| `src/components/calculator/ButtonGrid.test.tsx` | Modify — remove HYP/DEG/SHIFT-button tests, add new |
| `src/components/calculator/DPad.tsx` | Delete |
| `src/components/calculator/Display.tsx` | Rewrite — header strip, onCursorJump, result without "= " |
| `src/components/calculator/Display.test.tsx` | Modify — update for new props |
| `src/components/calculator/Calculator.tsx` | Modify — wire new handlers, remove DPad, pass new props |

---

### Task 1: Extend AST types

**Files:**
- Modify: `src/lib/ast/types.ts`

No tests needed — pure type changes. All existing tests should still pass.

- [ ] **Step 1: Apply type changes**

Replace the contents of `src/lib/ast/types.ts` with:

```typescript
export type NumberNode    = { type: 'number';    value: string }
export type OperatorNode  = { type: 'operator';  op: '+' | '-' | '×' | '÷' }
export type ConstantNode  = { type: 'constant';  name: 'pi' | 'e' | 'Ans' | 'A' | 'B' | 'X' | 'Y' }
export type FractionNode  = { type: 'fraction';  numerator: ASTNode[]; denominator: ASTNode[] }
export type ExponentNode  = { type: 'exponent';  base: ASTNode[];      exponent: ASTNode[] }
export type RadicalNode   = { type: 'radical';   degree: ASTNode[];    radicand: ASTNode[] }
export type FunctionNode  = { type: 'function';  name: FunctionName;   argument: ASTNode[] }
export type FactorialNode = { type: 'factorial'; operand: ASTNode[] }
export type NcrNode       = { type: 'ncr';       n: ASTNode[];         r: ASTNode[] }
export type NprNode       = { type: 'npr';       n: ASTNode[];         r: ASTNode[] }
export type ParenNode     = { type: 'paren';     children: ASTNode[] }

export type FunctionName =
  | 'sin' | 'cos' | 'tan'
  | 'asin' | 'acos' | 'atan'
  | 'sinh' | 'cosh' | 'tanh'
  | 'asinh' | 'acosh' | 'atanh'
  | 'log' | 'ln' | 'pow10' | 'exp' | 'abs'

export type ASTNode =
  | NumberNode | OperatorNode | ConstantNode
  | FractionNode | ExponentNode | RadicalNode
  | FunctionNode | FactorialNode | NcrNode | NprNode | ParenNode

export interface CursorSegment {
  nodeIndex: number
  slot: string
}

export interface Cursor {
  path: CursorSegment[]
  insertAt: number
}

export const INITIAL_CURSOR: Cursor = { path: [], insertAt: 0 }

export type MemorySlots = { A: number | null; B: number | null; X: number | null; Y: number | null }

export interface HistoryEntry {
  id: string
  expression: ASTNode[]
  result: string
  timestamp: number
}

export interface CalculatorState {
  expression: ASTNode[]
  cursor: Cursor
  result: string | null
  resultMode: 'decimal' | 'fraction'
  angleMode: 'DEG' | 'RAD'
  shiftActive: boolean
  hypActive: boolean
  history: HistoryEntry[]
  memory: MemorySlots
  stoMode: boolean
}
```

- [ ] **Step 2: Run tests to verify no regressions**

```bash
pnpm test
```
Expected: all tests pass (the union type extension is backward-compatible).

- [ ] **Step 3: Commit**

```bash
git add src/lib/ast/types.ts
git commit -m "feat: extend ConstantNode (A/B/X/Y memory slots) and add abs to FunctionName"
```

---

### Task 2: Add builder functions

**Files:**
- Modify: `src/lib/ast/builder.ts`
- Modify: `src/lib/ast/builder.test.ts`

- [ ] **Step 1: Add import line to builder.test.ts**

In `src/lib/ast/builder.test.ts`, extend the import from `'./builder'` to also import the four new functions:

```typescript
import {
  insertDigit, insertDecimalPoint, insertOperator,
  insertFraction, insertExponent, insertRadical, insertSquare,
  insertFunction, insertConstant, insertFactorial, insertNcr, insertNpr,
  insertParen, deleteCurrent, clearAll,
  insertCube, insertNthRadical, insertMixed, insertNumberLiteral,
} from './builder'
```

- [ ] **Step 2: Append failing tests to builder.test.ts**

Add these four `describe` blocks at the end of `src/lib/ast/builder.test.ts`:

```typescript
describe('insertCube', () => {
  it('wraps previous node as base with exponent 3', () => {
    const init: ASTNode[] = [{ type: 'number', value: '5' }]
    const [nodes] = insertCube(init, C(1))
    expect(nodes).toHaveLength(1)
    expect(nodes[0]).toMatchObject({ type: 'exponent', exponent: [{ type: 'number', value: '3' }] })
    expect((nodes[0] as any).base).toEqual([{ type: 'number', value: '5' }])
  })

  it('inserts empty-base exponent^3 when no previous node', () => {
    const [nodes] = insertCube([], INITIAL_CURSOR)
    expect(nodes[0]).toMatchObject({ type: 'exponent', base: [], exponent: [{ type: 'number', value: '3' }] })
  })
})

describe('insertNthRadical', () => {
  it('inserts radical with empty degree and cursor in degree slot', () => {
    const [nodes, cur] = insertNthRadical([], INITIAL_CURSOR)
    expect(nodes[0]).toMatchObject({ type: 'radical', degree: [], radicand: [] })
    expect(cur.path).toEqual([{ nodeIndex: 0, slot: 'degree' }])
    expect(cur.insertAt).toBe(0)
  })
})

describe('insertMixed', () => {
  it('inserts whole-number 0 then empty fraction, cursor starts in numerator', () => {
    const [nodes, cur] = insertMixed([], INITIAL_CURSOR)
    expect(nodes).toHaveLength(2)
    expect(nodes[0]).toMatchObject({ type: 'number', value: '0' })
    expect(nodes[1]).toMatchObject({ type: 'fraction', numerator: [], denominator: [] })
    expect(cur.path).toEqual([{ nodeIndex: 1, slot: 'numerator' }])
    expect(cur.insertAt).toBe(0)
  })
})

describe('insertNumberLiteral', () => {
  it('inserts a NumberNode with the given string value', () => {
    const [nodes, cur] = insertNumberLiteral([], INITIAL_CURSOR, '3.14')
    expect(nodes[0]).toEqual({ type: 'number', value: '3.14' })
    expect(cur.insertAt).toBe(1)
  })
})
```

- [ ] **Step 3: Run tests to confirm they fail**

```bash
pnpm test 2>&1 | grep -E 'insertCube|insertNthRadical|insertMixed|insertNumberLiteral|FAIL'
```
Expected: 5 failing tests (missing exports).

- [ ] **Step 4: Implement the four functions in builder.ts**

Append to the end of `src/lib/ast/builder.ts`:

```typescript
export function insertCube(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const [r, c] = wrapPrev(root, cursor, (prev) => ({
    type: 'exponent', base: prev, exponent: [{ type: 'number', value: '3' }],
  } as ExponentNode))
  return [r, c]
}

export function insertNthRadical(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const [r, c] = spliceInsert(root, cursor, { type: 'radical', degree: [], radicand: [] } as RadicalNode)
  return [r, { path: [...c.path, { nodeIndex: c.insertAt - 1, slot: 'degree' }], insertAt: 0 }]
}

export function insertMixed(root: ASTNode[], cursor: Cursor): [ASTNode[], Cursor] {
  const r = clone(root)
  const list = getList(r, cursor.path)
  const wholeNode: ASTNode = { type: 'number', value: '0' }
  const fracNode: FractionNode = { type: 'fraction', numerator: [], denominator: [] }
  list.splice(cursor.insertAt, 0, wholeNode, fracNode)
  const fracIndex = cursor.insertAt + 1
  return [r, { path: [...cursor.path, { nodeIndex: fracIndex, slot: 'numerator' }], insertAt: 0 }]
}

export function insertNumberLiteral(root: ASTNode[], cursor: Cursor, value: string): [ASTNode[], Cursor] {
  return spliceInsert(root, cursor, { type: 'number', value })
}
```

- [ ] **Step 5: Run tests to confirm they pass**

```bash
pnpm test
```
Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/lib/ast/builder.ts src/lib/ast/builder.test.ts
git commit -m "feat: add insertCube, insertNthRadical, insertMixed, insertNumberLiteral builders"
```

---

### Task 3: Extend evaluator with memory scope and fraction helpers

**Files:**
- Modify: `src/lib/evaluator.ts`
- Modify: `src/lib/evaluator.test.ts`

- [ ] **Step 1: Append failing tests to evaluator.test.ts**

In `src/lib/evaluator.test.ts`, add to the import line:

```typescript
import { evaluate, toFraction, toMixed, fromMixed } from './evaluator'
```

Then append at the end of the file:

```typescript
describe('evaluate with memory scope', () => {
  it('substitutes memory variable A', () => {
    const nodes: ASTNode[] = [{ type: 'constant', name: 'A' }]
    expect(evaluate(nodes, 'DEG', null, { A: 7, B: null, X: null, Y: null })?.value).toBe('7')
  })

  it('defaults unset memory variable to 0', () => {
    const nodes: ASTNode[] = [{ type: 'constant', name: 'B' }]
    expect(evaluate(nodes, 'DEG', null, { A: null, B: null, X: null, Y: null })?.value).toBe('0')
  })
})

describe('toMixed', () => {
  it('converts improper fraction to mixed', () => {
    expect(toMixed('7/3')).toBe('2 1/3')
  })

  it('returns null for a proper fraction', () => {
    expect(toMixed('1/3')).toBeNull()
  })

  it('returns whole number string when fraction divides evenly', () => {
    expect(toMixed('6/3')).toBe('2')
  })

  it('returns null for non-fraction input', () => {
    expect(toMixed('3.14')).toBeNull()
  })
})

describe('fromMixed', () => {
  it('converts mixed number to improper fraction', () => {
    expect(fromMixed('2 1/3')).toBe('7/3')
  })

  it('returns null for an improper fraction string', () => {
    expect(fromMixed('7/3')).toBeNull()
  })
})
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
pnpm test 2>&1 | grep -E 'toMixed|fromMixed|memory scope|FAIL'
```
Expected: failing — `toMixed` and `fromMixed` not exported; `evaluate` missing memory param.

- [ ] **Step 3: Update evaluator.ts**

Replace the `evaluate` function signature and body, and add two new exports. The full updated file:

```typescript
import { create, all } from 'mathjs'
import { ASTNode, MemorySlots } from './ast/types'
import { serialize } from './ast/serializer'

const mathRad = create(all)
const mathDeg = create(all)

mathDeg.import!({
  sin:  (x: number) => Math.sin(x * Math.PI / 180),
  cos:  (x: number) => Math.cos(x * Math.PI / 180),
  tan:  (x: number) => Math.tan(x * Math.PI / 180),
  sinh: (x: number) => Math.sinh(x * Math.PI / 180),
  cosh: (x: number) => Math.cosh(x * Math.PI / 180),
  tanh: (x: number) => Math.tanh(x * Math.PI / 180),
  asin: (x: number) => Math.asin(x) * 180 / Math.PI,
  acos: (x: number) => Math.acos(x) * 180 / Math.PI,
  atan: (x: number) => Math.atan(x) * 180 / Math.PI,
  asinh: (x: number) => Math.asinh(x) * 180 / Math.PI,
  acosh: (x: number) => Math.acosh(x) * 180 / Math.PI,
  atanh: (x: number) => Math.atanh(x) * 180 / Math.PI,
}, { override: true })

export interface EvalResult {
  value: string
}

export function evaluate(
  nodes: ASTNode[],
  angleMode: 'DEG' | 'RAD',
  lastResult: string | null,
  memory?: MemorySlots,
): EvalResult | null {
  const expr = serialize(nodes)
  if (!expr.trim()) return null

  const withAns = expr.replace(/\bans\b/gi, lastResult ?? '0')
  const engine = angleMode === 'DEG' ? mathDeg : mathRad

  const scope: Record<string, number> = {
    A: memory?.A ?? 0,
    B: memory?.B ?? 0,
    X: memory?.X ?? 0,
    Y: memory?.Y ?? 0,
  }

  try {
    const raw = engine.evaluate(withAns, scope)
    if (raw === undefined || raw === null) return null
    const n = typeof raw === 'number' ? raw : Number(raw)
    if (!isFinite(n)) return { value: 'Math Error' }
    return { value: formatNumber(n) }
  } catch {
    return null
  }
}

export function toFraction(decimalStr: string): string | null {
  try {
    const n = parseFloat(decimalStr)
    if (!isFinite(n) || isNaN(n) || Number.isInteger(n)) return null
    const frac = mathDeg.fraction(n) as unknown as { n: number; d: number; s: number }
    if (frac.d <= 1 || frac.d > 999) return null
    return frac.s < 0 ? `-${frac.n}/${frac.d}` : `${frac.n}/${frac.d}`
  } catch {
    return null
  }
}

export function toMixed(fracStr: string): string | null {
  const match = fracStr.match(/^(-?\d+)\/(\d+)$/)
  if (!match) return null
  const num = parseInt(match[1])
  const den = parseInt(match[2])
  if (den === 0 || Math.abs(num) < den) return null
  const whole = Math.trunc(num / den)
  const remainder = Math.abs(num % den)
  if (remainder === 0) return String(whole)
  return `${whole} ${remainder}/${den}`
}

export function fromMixed(mixedStr: string): string | null {
  const match = mixedStr.match(/^(-?\d+) (\d+)\/(\d+)$/)
  if (!match) return null
  const whole = parseInt(match[1])
  const num = parseInt(match[2])
  const den = parseInt(match[3])
  const improperNum = whole < 0 ? whole * den - num : whole * den + num
  return `${improperNum}/${den}`
}

function formatNumber(n: number): string {
  if (Number.isInteger(n) && Math.abs(n) < 1e15) return String(n)
  const s = n.toPrecision(10)
  return String(parseFloat(s))
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
pnpm test
```
Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/lib/evaluator.ts src/lib/evaluator.test.ts
git commit -m "feat: add memory scope to evaluate, export toMixed and fromMixed"
```

---

### Task 4: Extend useCalculator

**Files:**
- Modify: `src/hooks/useCalculator.ts`
- Modify: `src/hooks/useCalculator.test.ts`

- [ ] **Step 1: Append failing tests to useCalculator.test.ts**

Add to the imports at the top of `src/hooks/useCalculator.test.ts`:

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest'
```

Append these test blocks at the end of the file:

```typescript
  it('Sto sets stoMode true', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('Sto'))
    expect(result.current.state.stoMode).toBe(true)
  })

  it('pressing a non-memory button cancels stoMode', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('Sto'))
    act(() => result.current.handleButton('5'))
    expect(result.current.state.stoMode).toBe(false)
  })

  it('stores result to memory slot A after Sto + A', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('5'))
    act(() => result.current.handleButton('='))
    act(() => result.current.handleButton('Sto'))
    act(() => result.current.handleButton('A'))
    expect(result.current.state.memory.A).toBe(5)
    expect(result.current.state.stoMode).toBe(false)
  })

  it('inserts memory constant when A pressed without stoMode', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('A'))
    expect(result.current.state.expression[0]).toMatchObject({ type: 'constant', name: 'A' })
  })

  it('handleCursorJump moves cursor to given path+insertAt', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleCursorJump([{ nodeIndex: 0, slot: 'numerator' }], 2))
    expect(result.current.state.cursor).toEqual({
      path: [{ nodeIndex: 0, slot: 'numerator' }],
      insertAt: 2,
    })
  })

  it('cube action wraps previous node in exponent^3', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('4'))
    act(() => result.current.handleButton('cube'))
    expect(result.current.state.expression[0]).toMatchObject({ type: 'exponent' })
  })

  it('AC clears expression', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('5'))
    act(() => result.current.handleButton('AC'))
    expect(result.current.state.expression).toEqual([])
  })
```

Note: close the `describe('useCalculator', ...)` block properly — these tests go inside the existing describe block before its closing `})`.

- [ ] **Step 2: Run tests to confirm they fail**

```bash
pnpm test 2>&1 | grep -E 'stoMode|handleCursorJump|cube action|AC clears|FAIL'
```
Expected: failing on new tests.

- [ ] **Step 3: Rewrite useCalculator.ts**

Replace `src/hooks/useCalculator.ts` with:

```typescript
import { useState, useCallback } from 'react'
import { ASTNode, Cursor, CursorSegment, HistoryEntry, CalculatorState, MemorySlots, INITIAL_CURSOR, FunctionName } from '@/lib/ast/types'
import {
  insertDigit, insertDecimalPoint, insertOperator, insertFraction,
  insertExponent, insertRadical, insertSquare, insertFunction,
  insertConstant, insertFactorial, insertNcr, insertNpr, insertParen,
  insertReciprocal, insertExp, insertNegate, deleteCurrent, clearAll,
  insertCube, insertNthRadical, insertMixed, insertNumberLiteral,
} from '@/lib/ast/builder'
import { moveCursorLeft, moveCursorRight, moveCursorUp, moveCursorDown } from '@/lib/ast/cursor'
import { evaluate, toFraction, toMixed, fromMixed } from '@/lib/evaluator'
import { loadHistory, addEntry } from '@/lib/history'

type State = CalculatorState & { lastResult: string | null }

const INIT: State = {
  expression: [], cursor: INITIAL_CURSOR, result: null, resultMode: 'decimal',
  angleMode: 'DEG', shiftActive: false, hypActive: false,
  history: loadHistory(), lastResult: null,
  memory: { A: null, B: null, X: null, Y: null },
  stoMode: false,
}

export function useCalculator() {
  const [state, setState] = useState<State>(INIT)

  const handleButton = useCallback((id: string) => {
    setState(prev => {
      const { expression: e, cursor: c, angleMode, lastResult, history, shiftActive, hypActive, memory } = prev

      if (id === 'SHIFT')   return { ...prev, shiftActive: !shiftActive, hypActive: false }
      if (id === 'HYP')     return { ...prev, hypActive: !hypActive, shiftActive: false }
      if (id === 'DEG_RAD') return { ...prev, angleMode: angleMode === 'DEG' ? 'RAD' : 'DEG', shiftActive: false }

      // Memory store: Sto puts us into store mode
      if (id === 'Sto') return { ...prev, stoMode: true, shiftActive: false, hypActive: false }

      // Memory recall/store: A B X Y
      if (id === 'A' || id === 'B' || id === 'X' || id === 'Y') {
        const deshift = { shiftActive: false, hypActive: false, stoMode: false }
        if (prev.stoMode) {
          const val = prev.result !== null ? parseFloat(prev.result) : NaN
          if (!isNaN(val)) {
            return { ...prev, ...deshift, memory: { ...memory, [id]: val } }
          }
          return { ...prev, ...deshift }
        }
        // Recall: insert constant node with this letter
        const [newExpr, newCursor] = insertConstant(e, c, id as 'A' | 'B' | 'X' | 'Y')
        const evalResult = evaluate(newExpr, angleMode, lastResult, memory)
        return { ...prev, ...deshift, expression: newExpr, cursor: newCursor, result: evalResult?.value ?? null, resultMode: 'decimal' }
      }

      const deshift = { shiftActive: false, hypActive: false, stoMode: false }
      type Pair = [ASTNode[], Cursor]

      // S⇔D / DecFrac: toggle between decimal and fraction
      if (id === 'S_TO_D' || id === 'DecFrac') {
        if (!prev.result) return { ...prev, ...deshift }
        if (prev.resultMode === 'fraction') {
          return { ...prev, ...deshift, result: prev.lastResult, resultMode: 'decimal' }
        }
        const fracStr = toFraction(prev.result)
        if (!fracStr) return { ...prev, ...deshift }
        return { ...prev, ...deshift, lastResult: prev.result, result: fracStr, resultMode: 'fraction' }
      }

      // MixedFrac: toggle between improper fraction and mixed number display
      if (id === 'MixedFrac') {
        if (!prev.result) return { ...prev, ...deshift }
        const mixed = toMixed(prev.result)
        if (mixed) return { ...prev, ...deshift, result: mixed, resultMode: 'fraction' }
        const improper = fromMixed(prev.result)
        if (improper) return { ...prev, ...deshift, result: improper, resultMode: 'fraction' }
        return { ...prev, ...deshift }
      }

      let pair: Pair
      switch (id) {
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
          pair = insertDigit(e, c, id); break
        case '.':        pair = insertDecimalPoint(e, c); break
        case 'plus':     pair = insertOperator(e, c, '+'); break
        case 'minus':    pair = insertOperator(e, c, '-'); break
        case 'multiply': pair = insertOperator(e, c, '×'); break
        case 'divide':   pair = insertOperator(e, c, '÷'); break
        case 'fraction': pair = insertFraction(e, c); break
        case 'exponent': pair = insertExponent(e, c); break
        case 'sqrt':     pair = insertRadical(e, c, false); break
        case 'cbrt':     pair = insertRadical(e, c, true); break
        case 'square':   pair = insertSquare(e, c); break
        case 'cube':     pair = insertCube(e, c); break
        case 'nthRoot':  pair = insertNthRadical(e, c); break
        case 'mix':      pair = insertMixed(e, c); break
        case 'factorial': pair = insertFactorial(e, c); break
        case 'ncr':      pair = insertNcr(e, c); break
        case 'npr':      pair = insertNpr(e, c); break
        case 'paren_open':  pair = insertParen(e, c, 'open'); break
        case 'paren_close': pair = insertParen(e, c, 'close'); break
        case 'pi':         pair = insertConstant(e, c, 'pi'); break
        case 'e_const':    pair = insertConstant(e, c, 'e'); break
        case 'Ans':        pair = insertConstant(e, c, 'Ans'); break
        case 'reciprocal': pair = insertReciprocal(e, c); break
        case 'EXP':        pair = insertExp(e, c); break
        case 'negate':     pair = insertNegate(e, c); break
        case 'abs':
        case 'sin': case 'cos': case 'tan':
        case 'asin': case 'acos': case 'atan':
        case 'sinh': case 'cosh': case 'tanh':
        case 'asinh': case 'acosh': case 'atanh':
        case 'log': case 'ln': case 'pow10': case 'exp':
          pair = insertFunction(e, c, id as FunctionName); break
        case 'DEL':   pair = deleteCurrent(e, c); break
        case 'CLEAR': case 'AC': pair = clearAll(); break
        case 'LEFT':  return { ...prev, cursor: moveCursorLeft(e, c) }
        case 'RIGHT': return { ...prev, cursor: moveCursorRight(e, c) }
        case 'UP':    return { ...prev, cursor: moveCursorUp(e, c) }
        case 'DOWN':  return { ...prev, cursor: moveCursorDown(e, c) }
        case '=': {
          const evalResult = evaluate(e, angleMode, lastResult, memory)
          if (!evalResult) return { ...prev, ...deshift }
          const newHistory = addEntry(history, e, evalResult.value)
          return {
            ...prev, ...deshift,
            history: newHistory,
            result: evalResult.value,
            resultMode: 'decimal',
            lastResult: evalResult.value,
          }
        }
        default: return { ...prev, ...deshift }
      }

      const [newExpr, newCursor] = pair
      const evalResult = evaluate(newExpr, angleMode, lastResult, memory)
      return {
        ...prev, ...deshift,
        expression: newExpr,
        cursor: newCursor,
        result: evalResult?.value ?? null,
        resultMode: 'decimal',
      }
    })
  }, [])

  const handleCursorJump = useCallback((path: CursorSegment[], insertAt: number) => {
    setState(prev => ({ ...prev, cursor: { path, insertAt } }))
  }, [])

  const handlePaste = useCallback(async () => {
    try {
      const text = await navigator.clipboard.readText()
      const trimmed = text.trim()
      if (!trimmed || !/^-?[\d]+(\.[\d]+)?([eE][+-]?[\d]+)?$/.test(trimmed)) return
      setState(prev => {
        const [newExpr, newCursor] = insertNumberLiteral(prev.expression, prev.cursor, trimmed)
        const evalResult = evaluate(newExpr, prev.angleMode, prev.lastResult, prev.memory)
        return {
          ...prev, shiftActive: false, hypActive: false, stoMode: false,
          expression: newExpr, cursor: newCursor,
          result: evalResult?.value ?? null, resultMode: 'decimal',
        }
      })
    } catch { /* clipboard access denied */ }
  }, [])

  const handleRestore = useCallback((entry: HistoryEntry) => {
    setState(prev => ({
      ...prev,
      expression: entry.expression,
      cursor: { path: [], insertAt: entry.expression.length },
      result: entry.result,
      resultMode: 'decimal',
      shiftActive: false,
      hypActive: false,
      stoMode: false,
    }))
  }, [])

  return { state, handleButton, handleCursorJump, handlePaste, handleRestore }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
pnpm test
```
Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/hooks/useCalculator.ts src/hooks/useCalculator.test.ts
git commit -m "feat: add memory/stoMode state, handleCursorJump, handlePaste, new button actions"
```

---

### Task 5: Thread onCursorJump through ASTRenderer and node views

**Files:**
- Modify: `src/components/ast/ASTRenderer.tsx`
- Modify: `src/components/ast/nodes/FractionNode.tsx`
- Modify: `src/components/ast/nodes/ExponentNode.tsx`
- Modify: `src/components/ast/nodes/RadicalNode.tsx`
- Modify: `src/components/ast/nodes/FunctionNode.tsx`
- Modify: `src/components/ast/nodes/ConstantNode.tsx`

No new tests needed — the change is purely additive (optional prop). Existing tests continue to render without `onCursorJump` and pass.

- [ ] **Step 1: Replace ASTRenderer.tsx**

```typescript
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
```

- [ ] **Step 2: Update FractionNode.tsx**

```typescript
import { FractionNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'

type JumpFn = (path: CursorSegment[], insertAt: number) => void
interface Props { node: FractionNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number; onCursorJump?: JumpFn }

export function FractionNodeView({ node, cursor, path, nodeIndex, onCursorJump }: Props) {
  return (
    <span className="inline-flex flex-col items-center mx-0.5 align-middle">
      <span className="px-1 min-w-4 text-center leading-tight">
        <ASTRenderer nodes={node.numerator} cursor={cursor} path={[...path, { nodeIndex, slot: 'numerator' }]} onCursorJump={onCursorJump} />
      </span>
      <span className="w-full border-t border-current" />
      <span className="px-1 min-w-4 text-center leading-tight">
        <ASTRenderer nodes={node.denominator} cursor={cursor} path={[...path, { nodeIndex, slot: 'denominator' }]} onCursorJump={onCursorJump} />
      </span>
    </span>
  )
}
```

- [ ] **Step 3: Update ExponentNode.tsx**

```typescript
import { ExponentNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'

type JumpFn = (path: CursorSegment[], insertAt: number) => void
interface Props { node: ExponentNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number; onCursorJump?: JumpFn }

export function ExponentNodeView({ node, cursor, path, nodeIndex, onCursorJump }: Props) {
  return (
    <span className="inline-flex items-start">
      <ASTRenderer nodes={node.base} cursor={cursor} path={[...path, { nodeIndex, slot: 'base' }]} onCursorJump={onCursorJump} />
      <span className="text-[0.65em] -mt-1 ml-px">
        <ASTRenderer nodes={node.exponent} cursor={cursor} path={[...path, { nodeIndex, slot: 'exponent' }]} onCursorJump={onCursorJump} />
      </span>
    </span>
  )
}
```

- [ ] **Step 4: Update RadicalNode.tsx**

```typescript
import { RadicalNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'

type JumpFn = (path: CursorSegment[], insertAt: number) => void
interface Props { node: RadicalNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number; onCursorJump?: JumpFn }

export function RadicalNodeView({ node, cursor, path, nodeIndex, onCursorJump }: Props) {
  return (
    <span className="inline-flex items-end">
      {node.degree.length > 0 && (
        <span className="text-[0.65em] mb-1 mr-px">
          <ASTRenderer nodes={node.degree} cursor={cursor} path={[...path, { nodeIndex, slot: 'degree' }]} onCursorJump={onCursorJump} />
        </span>
      )}
      <span className="text-xl leading-none">√</span>
      <span className="border-t border-current px-0.5">
        <ASTRenderer nodes={node.radicand} cursor={cursor} path={[...path, { nodeIndex, slot: 'radicand' }]} onCursorJump={onCursorJump} />
      </span>
    </span>
  )
}
```

- [ ] **Step 5: Update FunctionNode.tsx** (also add `abs` label)

```typescript
import { FunctionNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'

const LABELS: Record<string, string> = {
  sin: 'sin', cos: 'cos', tan: 'tan',
  asin: 'sin⁻¹', acos: 'cos⁻¹', atan: 'tan⁻¹',
  sinh: 'sinh', cosh: 'cosh', tanh: 'tanh',
  asinh: 'sinh⁻¹', acosh: 'cosh⁻¹', atanh: 'tanh⁻¹',
  log: 'log', ln: 'ln', pow10: '10ˣ', exp: 'eˣ', abs: 'abs',
}

type JumpFn = (path: CursorSegment[], insertAt: number) => void
interface Props { node: FunctionNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number; onCursorJump?: JumpFn }

export function FunctionNodeView({ node, cursor, path, nodeIndex, onCursorJump }: Props) {
  return (
    <span className="inline-flex items-center">
      <span className="text-sm opacity-90 mr-px">{LABELS[node.name] ?? node.name}(</span>
      <ASTRenderer nodes={node.argument} cursor={cursor} path={[...path, { nodeIndex, slot: 'argument' }]} onCursorJump={onCursorJump} />
      <span className="text-sm opacity-90 ml-px">)</span>
    </span>
  )
}
```

- [ ] **Step 6: Update ConstantNode.tsx** (add A/B/X/Y labels)

```typescript
import { ConstantNode } from '@/lib/ast/types'
const LABELS: Record<string, string> = { pi: 'π', e: 'e', Ans: 'Ans', A: 'A', B: 'B', X: 'X', Y: 'Y' }
export function ConstantNodeView({ node }: { node: ConstantNode }) {
  return <span className="italic">{LABELS[node.name] ?? node.name}</span>
}
```

- [ ] **Step 7: Run tests**

```bash
pnpm test
```
Expected: all pass.

- [ ] **Step 8: Commit**

```bash
git add src/components/ast/ASTRenderer.tsx \
        src/components/ast/nodes/FractionNode.tsx \
        src/components/ast/nodes/ExponentNode.tsx \
        src/components/ast/nodes/RadicalNode.tsx \
        src/components/ast/nodes/FunctionNode.tsx \
        src/components/ast/nodes/ConstantNode.tsx
git commit -m "feat: thread onCursorJump through ASTRenderer and node views"
```

---

### Task 6: Rewrite ButtonGrid (5-column, no DPad)

**Files:**
- Rewrite: `src/components/calculator/ButtonGrid.tsx`
- Modify: `src/components/calculator/ButtonGrid.test.tsx`
- Delete: `src/components/calculator/DPad.tsx`

- [ ] **Step 1: Update ButtonGrid.test.tsx**

Replace `src/components/calculator/ButtonGrid.test.tsx` with:

```typescript
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ButtonGrid } from './ButtonGrid'

const props = { shiftActive: false, onButton: vi.fn(), onPaste: vi.fn() }

describe('ButtonGrid', () => {
  it('renders = button', () => {
    render(<ButtonGrid {...props} />)
    expect(screen.getByText('=')).toBeInTheDocument()
  })

  it('calls onButton with digit id when digit pressed', () => {
    const onButton = vi.fn()
    render(<ButtonGrid {...props} onButton={onButton} />)
    fireEvent.click(screen.getByText('7'))
    expect(onButton).toHaveBeenCalledWith('7')
  })

  it('shows sin( label when shift inactive', () => {
    render(<ButtonGrid {...props} />)
    expect(screen.getByText('sin(')).toBeInTheDocument()
  })

  it('shows asin action when shift active and sin( pressed', () => {
    const onButton = vi.fn()
    render(<ButtonGrid {...props} shiftActive onButton={onButton} />)
    fireEvent.click(screen.getByText('sin⁻¹('))
    expect(onButton).toHaveBeenCalledWith('asin')
  })

  it('renders AC button', () => {
    render(<ButtonGrid {...props} />)
    expect(screen.getByText('AC')).toBeInTheDocument()
  })

  it('AC calls onButton with AC', () => {
    const onButton = vi.fn()
    render(<ButtonGrid {...props} onButton={onButton} />)
    fireEvent.click(screen.getByText('AC'))
    expect(onButton).toHaveBeenCalledWith('AC')
  })
})
```

- [ ] **Step 2: Run updated tests to confirm they fail**

```bash
pnpm test -- --reporter=verbose 2>&1 | grep -E 'ButtonGrid|FAIL'
```
Expected: several ButtonGrid tests fail (layout not yet changed).

- [ ] **Step 3: Delete DPad.tsx**

```bash
rm src/components/calculator/DPad.tsx
```

- [ ] **Step 4: Rewrite ButtonGrid.tsx**

Replace `src/components/calculator/ButtonGrid.tsx` with:

```typescript
import { cn } from '@/lib/utils'

const DIGIT = 'bg-zinc-900 border border-zinc-700/70 text-white'
const FN    = 'bg-zinc-950 border border-zinc-800 text-zinc-300'
const OP    = 'bg-zinc-900 border border-zinc-700/70 text-sky-300'
const DEL   = 'bg-zinc-900 border border-zinc-700/70 text-red-400'
const AC    = 'bg-zinc-900 border border-zinc-700/70 text-red-500 font-semibold'
const EQ    = 'bg-blue-700 border border-blue-600 text-white'

interface BtnDef {
  id: string
  label: string
  shiftLabel?: string
  cls: string
}

const ROWS: BtnDef[][] = [
  // Row 1: sto/root/trig
  [
    { id: 'Sto',         label: 'Sto',    shiftLabel: 'PasteCode', cls: FN },
    { id: 'exponent',    label: 'xʸ',     shiftLabel: 'ⁿ√',        cls: FN },
    { id: 'sin',         label: 'sin(',   shiftLabel: 'sin⁻¹(',    cls: FN },
    { id: 'cos',         label: 'cos(',   shiftLabel: 'cos⁻¹(',    cls: FN },
    { id: 'tan',         label: 'tan(',   shiftLabel: 'tan⁻¹(',    cls: FN },
  ],
  // Row 2: abs/fraction/root/log/ln
  [
    { id: 'abs',         label: 'Abs',                              cls: FN },
    { id: 'fraction',    label: 'a/b',    shiftLabel: 'mix',        cls: FN },
    { id: 'sqrt',        label: '√',      shiftLabel: '³√',         cls: FN },
    { id: 'log',         label: 'log(',   shiftLabel: 'a/b↔cd/e',   cls: FN },
    { id: 'ln',          label: 'ln(',    shiftLabel: 'Dec↔a/b',    cls: FN },
  ],
  // Row 3: combinatorics / power / paren
  [
    { id: 'factorial',   label: 'x!',     shiftLabel: 'A',          cls: FN },
    { id: 'cube',        label: 'x³',     shiftLabel: 'B',          cls: FN },
    { id: 'square',      label: 'x²',     shiftLabel: 'X',          cls: FN },
    { id: 'paren_open',  label: '(',                                cls: FN },
    { id: 'paren_close', label: ')',       shiftLabel: 'Y',          cls: FN },
  ],
  // Row 4: 7 8 9 DEL AC
  [
    { id: '7',   label: '7',   cls: DIGIT },
    { id: '8',   label: '8',   cls: DIGIT },
    { id: '9',   label: '9',   cls: DIGIT },
    { id: 'DEL', label: 'DEL', cls: DEL   },
    { id: 'AC',  label: 'AC',  cls: AC    },
  ],
  // Row 5: 4 5 6 × ÷
  [
    { id: '4',        label: '4',  cls: DIGIT },
    { id: '5',        label: '5',  cls: DIGIT },
    { id: '6',        label: '6',  cls: DIGIT },
    { id: 'multiply', label: '×',  shiftLabel: 'nPr', cls: OP },
    { id: 'divide',   label: '÷',  shiftLabel: 'nCr', cls: OP },
  ],
  // Row 6: 1 2 3 + −
  [
    { id: '1',     label: '1', cls: DIGIT },
    { id: '2',     label: '2', cls: DIGIT },
    { id: '3',     label: '3', cls: DIGIT },
    { id: 'plus',  label: '+', cls: OP    },
    { id: 'minus', label: '−', cls: OP    },
  ],
  // Row 7: 0 . EXP = (−)
  [
    { id: '0',      label: '0',   cls: DIGIT },
    { id: '.',      label: '.',   cls: DIGIT },
    { id: 'EXP',    label: 'EXP',              cls: FN    },
    { id: '=',      label: '=',   shiftLabel: 'S⇔D', cls: EQ },
    { id: 'negate', label: '(−)',              cls: DIGIT },
  ],
]

interface Props {
  shiftActive: boolean
  onButton: (id: string) => void
  onPaste: () => void
}

export function ButtonGrid({ shiftActive, onButton, onPaste }: Props) {
  function displayLabel(btn: BtnDef): string {
    if (shiftActive && btn.shiftLabel) return btn.shiftLabel
    return btn.label
  }

  function actionId(btn: BtnDef): string {
    if (shiftActive) {
      if (btn.id === 'sin')         return 'asin'
      if (btn.id === 'cos')         return 'acos'
      if (btn.id === 'tan')         return 'atan'
      if (btn.id === 'exponent')    return 'nthRoot'
      if (btn.id === 'sqrt')        return 'cbrt'
      if (btn.id === 'log')         return 'MixedFrac'
      if (btn.id === 'ln')          return 'DecFrac'
      if (btn.id === 'fraction')    return 'mix'
      if (btn.id === 'multiply')    return 'npr'
      if (btn.id === 'divide')      return 'ncr'
      if (btn.id === '=')           return 'S_TO_D'
      if (btn.id === 'factorial')   return 'A'
      if (btn.id === 'cube')        return 'B'
      if (btn.id === 'square')      return 'X'
      if (btn.id === 'paren_close') return 'Y'
      if (btn.id === 'Sto')         return 'PasteCode'
    }
    return btn.id
  }

  function handleClick(btn: BtnDef) {
    const id = actionId(btn)
    if (id === 'PasteCode') { onPaste(); return }
    onButton(id)
  }

  function renderBtn(btn: BtnDef, key: string) {
    const secondary = !shiftActive && btn.shiftLabel ? btn.shiftLabel : null
    return (
      <button
        key={key}
        onClick={() => handleClick(btn)}
        className={cn(
          'rounded text-sm font-medium select-none',
          'active:scale-95 active:brightness-75 transition-all duration-75',
          secondary
            ? 'h-12 flex flex-col items-center justify-center gap-0'
            : 'h-11 flex items-center justify-center',
          btn.cls,
        )}
      >
        {secondary && (
          <span className="text-[7px] text-orange-400 leading-none mb-0.5 font-normal tracking-wide">
            {secondary}
          </span>
        )}
        <span className="leading-none">{displayLabel(btn)}</span>
      </button>
    )
  }

  return (
    <div className="grid grid-cols-5 gap-[3px] p-2 bg-zinc-950 flex-shrink-0">
      {ROWS.flat().map((btn, i) => renderBtn(btn, btn.id + i))}
    </div>
  )
}
```

- [ ] **Step 5: Run tests to confirm they pass**

```bash
pnpm test
```
Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/components/calculator/ButtonGrid.tsx src/components/calculator/ButtonGrid.test.tsx
git rm src/components/calculator/DPad.tsx
git commit -m "feat: rewrite ButtonGrid to 5-column reference layout, delete DPad"
```

---

### Task 7: Rewrite Display component

**Files:**
- Rewrite: `src/components/calculator/Display.tsx`
- Modify: `src/components/calculator/Display.test.tsx`

- [ ] **Step 1: Update Display.test.tsx**

Replace `src/components/calculator/Display.test.tsx` with:

```typescript
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { Display } from './Display'
import { INITIAL_CURSOR } from '@/lib/ast/types'

const baseProps = {
  expression: [],
  cursor: INITIAL_CURSOR,
  result: null,
  shiftActive: false,
  stoMode: false,
  hypActive: false,
  angleMode: 'DEG' as const,
  onShiftToggle: vi.fn(),
  onCopy: vi.fn(),
  onAngleToggle: vi.fn(),
  onCursorJump: vi.fn(),
}

describe('Display', () => {
  it('shows placeholder 0 when expression empty', () => {
    render(<Display {...baseProps} />)
    expect(screen.getByText('0')).toBeInTheDocument()
  })

  it('shows result when result provided (no = prefix)', () => {
    render(<Display {...baseProps} result="42" />)
    expect(screen.getByText('42')).toBeInTheDocument()
  })

  it('does not show result when result is null', () => {
    const { container } = render(<Display {...baseProps} />)
    expect(container.querySelector('[data-testid="result"]')).toBeNull()
  })

  it('shows STO indicator when stoMode true', () => {
    render(<Display {...baseProps} stoMode />)
    expect(screen.getByText('STO')).toBeInTheDocument()
  })

  it('calls onShiftToggle when ≡ clicked', () => {
    const onShiftToggle = vi.fn()
    render(<Display {...baseProps} onShiftToggle={onShiftToggle} />)
    fireEvent.click(screen.getByLabelText('toggle shift'))
    expect(onShiftToggle).toHaveBeenCalled()
  })

  it('calls onAngleToggle when angle label clicked', () => {
    const onAngleToggle = vi.fn()
    render(<Display {...baseProps} onAngleToggle={onAngleToggle} />)
    fireEvent.click(screen.getByText('DEG'))
    expect(onAngleToggle).toHaveBeenCalled()
  })
})
```

- [ ] **Step 2: Run updated tests to confirm they fail**

```bash
pnpm test 2>&1 | grep -E 'Display|FAIL'
```
Expected: Display tests fail (old component doesn't have new props).

- [ ] **Step 3: Rewrite Display.tsx**

Replace `src/components/calculator/Display.tsx` with:

```typescript
import { useState } from 'react'
import { ASTNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ast/ASTRenderer'
import { cn } from '@/lib/utils'

interface Props {
  expression: ASTNode[]
  cursor: Cursor
  result: string | null
  shiftActive: boolean
  stoMode: boolean
  hypActive: boolean
  angleMode: 'DEG' | 'RAD'
  onShiftToggle: () => void
  onCopy: () => void
  onAngleToggle: () => void
  onCursorJump: (path: CursorSegment[], insertAt: number) => void
}

export function Display({
  expression, cursor, result,
  shiftActive, stoMode, hypActive, angleMode,
  onShiftToggle, onCopy, onAngleToggle, onCursorJump,
}: Props) {
  const [copied, setCopied] = useState(false)
  const empty = expression.length === 0 && cursor.path.length === 0 && cursor.insertAt === 0

  function handleCopy() {
    if (!result) return
    navigator.clipboard.writeText(result).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 1200)
    })
    onCopy()
  }

  return (
    <div className="flex px-3 pb-3 pt-2 min-h-[7.5rem] overflow-hidden gap-2">
      {/* Left strip: shift toggle, share, angle */}
      <div className="flex flex-col items-center gap-1 pt-0.5 flex-shrink-0">
        <button
          aria-label="toggle shift"
          onClick={onShiftToggle}
          className={cn(
            'text-base leading-none select-none transition-colors',
            shiftActive ? 'text-orange-400' : 'text-zinc-600 hover:text-zinc-400',
          )}
        >
          ≡
        </button>
        <button
          aria-label="share result"
          onClick={handleCopy}
          className="text-xs leading-none text-zinc-600 hover:text-zinc-400 select-none transition-colors"
        >
          ◁
        </button>
        <button
          onClick={onAngleToggle}
          className={cn(
            'text-[9px] tracking-widest select-none transition-colors',
            angleMode === 'RAD' ? 'text-blue-400' : 'text-zinc-600 hover:text-zinc-400',
          )}
        >
          {angleMode}
        </button>
      </div>

      {/* Main display: expression + result */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Status badges */}
        <div className="flex justify-end gap-2 h-4">
          {hypActive && <span className="text-[9px] text-indigo-400">HYP</span>}
          {stoMode   && <span className="text-[9px] text-yellow-400">STO</span>}
        </div>

        {/* Expression */}
        <div className="flex-1 overflow-x-auto flex items-center justify-end font-display text-white text-[1.75rem] leading-snug">
          {empty
            ? <span className="text-zinc-800">0</span>
            : <ASTRenderer nodes={expression} cursor={cursor} path={[]} onCursorJump={onCursorJump} />
          }
        </div>

        {/* Result */}
        {result !== null && (
          <button
            data-testid="result"
            onClick={handleCopy}
            className="self-end mt-1 select-none active:opacity-60 transition-opacity"
            aria-label="copy result"
          >
            <span className={copied ? 'font-display text-base text-green-400' : 'font-display text-base text-zinc-400'}>
              {copied ? '✓ copied' : result}
            </span>
          </button>
        )}
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
pnpm test
```
Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/components/calculator/Display.tsx src/components/calculator/Display.test.tsx
git commit -m "feat: rewrite Display with header strip, tap-to-cursor, and result without = prefix"
```

---

### Task 8: Update Calculator to wire everything together

**Files:**
- Modify: `src/components/calculator/Calculator.tsx`

No new tests needed — Calculator is a thin wiring component; existing integration is covered by the component tests above.

- [ ] **Step 1: Rewrite Calculator.tsx**

Replace `src/components/calculator/Calculator.tsx` with:

```typescript
import { useCalculator } from '@/hooks/useCalculator'
import { useKeyboard } from '@/hooks/useKeyboard'
import { Display } from './Display'
import { ButtonGrid } from './ButtonGrid'
import { HistoryPanel } from './HistoryPanel'

export function Calculator() {
  const { state, handleButton, handleCursorJump, handlePaste, handleRestore } = useCalculator()
  useKeyboard(handleButton)

  return (
    <div className="flex flex-col h-dvh max-w-sm mx-auto bg-black overflow-hidden">
      <div className="flex-shrink-0 bg-[#080c08] border-b-2 border-zinc-800">
        <Display
          expression={state.expression}
          cursor={state.cursor}
          result={state.result}
          shiftActive={state.shiftActive}
          stoMode={state.stoMode}
          hypActive={state.hypActive}
          angleMode={state.angleMode}
          onShiftToggle={() => handleButton('SHIFT')}
          onCopy={() => {}}
          onAngleToggle={() => handleButton('DEG_RAD')}
          onCursorJump={handleCursorJump}
        />
      </div>

      <HistoryPanel history={state.history} onRestore={handleRestore} />

      <div
        className="flex-1 overflow-hidden flex flex-col justify-end"
        style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
      >
        <ButtonGrid
          shiftActive={state.shiftActive}
          onButton={handleButton}
          onPaste={handlePaste}
        />
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Run all tests**

```bash
pnpm test
```
Expected: all tests pass.

- [ ] **Step 3: Build to verify no TypeScript errors**

```bash
pnpm build 2>&1 | tail -20
```
Expected: build succeeds with no type errors.

- [ ] **Step 4: Commit**

```bash
git add src/components/calculator/Calculator.tsx
git commit -m "feat: wire 5-column layout, memory, and tap-cursor into Calculator"
```

---

## Self-Review

**Spec coverage check:**

| Spec requirement | Task |
|---|---|
| 5-column button grid | Task 6 |
| Remove DPad | Task 6 (delete DPad.tsx) |
| ≡ SHIFT toggle in display header | Task 7, Task 8 |
| ◁ share/copy in display header | Task 7 |
| RAD/DEG tap-to-toggle in display header | Task 7, Task 8 |
| Secondary labels always shown | Task 6 (shiftActive render) |
| DEL and AC as separate red buttons | Task 6 |
| Tap-on-display to position cursor | Tasks 4 (handleCursorJump), 5 (ASTRenderer), 7 (Display) |
| Sto + memory slots A/B/X/Y | Tasks 1 (types), 4 (hook) |
| PasteCode (SHIFT+Sto) | Tasks 4 (handlePaste), 6 (onPaste prop) |
| Dec↔a/b (DecFrac) | Tasks 3 (evaluator alias), 4 (hook), 6 (button) |
| a/b↔cd/e (MixedFrac) | Tasks 3 (toMixed/fromMixed), 4 (hook), 6 (button) |
| mix (mixed number insert) | Tasks 2 (builder), 4 (hook), 6 (button) |
| x³ (cube) | Tasks 2 (builder), 4 (hook), 6 (button) |
| ⁿ√ (nth radical) | Tasks 2 (builder), 4 (hook), 6 (button) |
| abs | Tasks 1 (FunctionName), 4 (hook), 5 (label), 6 (button) |
| Memory constant evaluation (A/B/X/Y in expressions) | Tasks 1 (type), 3 (evaluator scope), 4 (hook recall) |
| STO status indicator in display | Task 7 |

All spec items have a corresponding task. No gaps found.

**Placeholder scan:** No TBDs, TODOs, or vague steps found.

**Type consistency:** `MemorySlots` defined in Task 1 (types.ts), used in Tasks 3 (evaluator), 4 (hook). `JumpFn` is defined locally in each node file (consistent signature `(path: CursorSegment[], insertAt: number) => void`). `insertCube`, `insertNthRadical`, `insertMixed`, `insertNumberLiteral` defined in Task 2, imported in Task 4. All consistent.
