# Scientific Calculator PWA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a mobile-first scientific calculator PWA with FX-991-style natural textbook display, using React + shadcn/ui + Vercel dark theming.

**Architecture:** Custom AST renderer where each node type (FractionNode, ExponentNode, RadicalNode, etc.) is a React component. Buttons mutate the AST; a cursor path tracks position within the tree. math.js evaluates the serialised expression string after every change.

**Tech Stack:** Vite + React 18 + TypeScript, shadcn/ui CSS variables, Tailwind CSS v3, math.js, vite-plugin-pwa, Vitest + React Testing Library.

---

## File Map

```
src/
  lib/
    ast/
      types.ts          — all AST node types + Cursor + HistoryEntry
      builder.ts        — pure fns: insertDigit, insertFraction, insertExponent, etc.
      builder.test.ts
      serializer.ts     — AST[] → math.js expression string
      serializer.test.ts
      cursor.ts         — moveCursorLeft/Right/Up/Down
      cursor.test.ts
    evaluator.ts        — math.js wrapper with angle mode
    evaluator.test.ts
    history.ts          — localStorage CRUD for HistoryEntry[]
    history.test.ts
    utils.ts            — cn() utility
  components/
    ast/
      nodes/
        NumberNode.tsx
        OperatorNode.tsx
        ConstantNode.tsx
        FractionNode.tsx
        ExponentNode.tsx
        RadicalNode.tsx
        FunctionNode.tsx
      ASTRenderer.tsx   — recursive tree → JSX
      Cursor.tsx        — blinking cursor
    calculator/
      Display.tsx
      ButtonGrid.tsx
      HistoryPanel.tsx
      Calculator.tsx    — root layout
  hooks/
    useCalculator.ts
    useKeyboard.ts
  test/
    setup.ts
  App.tsx
  main.tsx
  index.css
public/
  icons/
    icon-192.png  (placeholder)
    icon-512.png  (placeholder)
index.html
vite.config.ts
tailwind.config.js
postcss.config.js
tsconfig.json
tsconfig.node.json
package.json
```

---

## Task 1: Project scaffolding

**Files:**
- Create: `package.json`, `tsconfig.json`, `tsconfig.node.json`, `vite.config.ts`, `tailwind.config.js`, `postcss.config.js`, `index.html`, `src/index.css`, `src/test/setup.ts`, `src/lib/utils.ts`

- [ ] **Step 1: Install dependencies**

```bash
cd /Users/mliem/Documents/GitHub/scientific-calculator
npm init -y
npm install react react-dom mathjs
npm install -D vite @vitejs/plugin-react typescript @types/react @types/react-dom
npm install -D tailwindcss@3 postcss autoprefixer
npm install -D vite-plugin-pwa workbox-window
npm install -D vitest @vitest/coverage-v8 jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event
npm install clsx tailwind-merge lucide-react
```

- [ ] **Step 2: Write `package.json` scripts section** (merge into existing)

```json
{
  "name": "scientific-calculator",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "test": "vitest run",
    "test:watch": "vitest",
    "test:coverage": "vitest run --coverage"
  }
}
```

- [ ] **Step 3: Write `tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": { "@/*": ["./src/*"] }
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

- [ ] **Step 4: Write `tsconfig.node.json`**

```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

- [ ] **Step 5: Write `vite.config.ts`**

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import path from 'path'

export default defineConfig(({ mode }) => ({
  plugins: [
    react(),
    mode !== 'test' && VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['icons/*.png'],
      manifest: {
        name: 'Scientific Calc',
        short_name: 'Calc',
        description: 'FX-991 style scientific calculator',
        theme_color: '#000000',
        background_color: '#000000',
        display: 'standalone',
        orientation: 'portrait',
        icons: [
          { src: 'icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png' },
        ],
      },
      workbox: { globPatterns: ['**/*.{js,css,html,ico,png,svg}'] },
    }),
  ].filter(Boolean),
  resolve: { alias: { '@': path.resolve(__dirname, './src') } },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    globals: true,
  },
}))
```

- [ ] **Step 6: Write `tailwind.config.js`**

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['class'],
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        border: 'hsl(var(--border))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        muted: { DEFAULT: 'hsl(var(--muted))', foreground: 'hsl(var(--muted-foreground))' },
        zinc: {
          800: '#27272a',
          900: '#18181b',
        },
      },
      borderRadius: { lg: 'var(--radius)', md: 'calc(var(--radius) - 2px)' },
    },
  },
  plugins: [],
}
```

- [ ] **Step 7: Write `postcss.config.js`**

```javascript
export default {
  plugins: { tailwindcss: {}, autoprefixer: {} },
}
```

- [ ] **Step 8: Write `index.html`**

```html
<!doctype html>
<html lang="en" class="dark">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/icons/icon-192.png" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <meta name="theme-color" content="#000000" />
    <title>Scientific Calc</title>
  </head>
  <body class="dark">
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 9: Write `src/index.css`**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 0 0% 0%;
    --foreground: 0 0% 100%;
    --muted: 0 0% 9%;
    --muted-foreground: 0 0% 64%;
    --border: 0 0% 14%;
    --radius: 0.5rem;
  }
  * { @apply border-border box-sizing-border; }
  html, body, #root { height: 100%; margin: 0; }
  body { @apply bg-background text-foreground; }
}
```

- [ ] **Step 10: Write `src/test/setup.ts`**

```typescript
import '@testing-library/jest-dom'
```

- [ ] **Step 11: Write `src/lib/utils.ts`**

```typescript
import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

- [ ] **Step 12: Create placeholder PWA icons**

```bash
mkdir -p public/icons
# Create minimal 1×1 black PNG as placeholder (192 and 512)
node -e "
const fs = require('fs');
// Minimal 1x1 black PNG bytes
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==', 'base64');
fs.writeFileSync('public/icons/icon-192.png', png);
fs.writeFileSync('public/icons/icon-512.png', png);
"
```

- [ ] **Step 13: Commit scaffold**

```bash
git add -A
git commit -m "feat: scaffold Vite + React + TypeScript + Tailwind + PWA"
```

---

## Task 2: AST types

**Files:**
- Create: `src/lib/ast/types.ts`

- [ ] **Step 1: Write `src/lib/ast/types.ts`**

```typescript
export type NumberNode    = { type: 'number';    value: string }
export type OperatorNode  = { type: 'operator';  op: '+' | '-' | '×' | '÷' }
export type ConstantNode  = { type: 'constant';  name: 'pi' | 'e' | 'Ans' }
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
  | 'log' | 'ln' | 'pow10' | 'exp'

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
  angleMode: 'DEG' | 'RAD'
  shiftActive: boolean
  hypActive: boolean
  history: HistoryEntry[]
  lastResult: string | null
}
```

- [ ] **Step 2: Commit**

```bash
git add src/lib/ast/types.ts
git commit -m "feat: add AST type definitions"
```

---

## Task 3: AST builder (TDD)

**Files:**
- Create: `src/lib/ast/builder.ts`, `src/lib/ast/builder.test.ts`

- [ ] **Step 1: Write the failing tests in `src/lib/ast/builder.test.ts`**

```typescript
import { describe, it, expect } from 'vitest'
import {
  insertDigit, insertDecimalPoint, insertOperator,
  insertFraction, insertExponent, insertRadical, insertSquare,
  insertFunction, insertConstant, insertFactorial, insertNcr, insertNpr,
  insertParen, deleteCurrent, clearAll,
} from './builder'
import { ASTNode, Cursor, INITIAL_CURSOR } from './types'

const C = (insertAt: number, path: Cursor['path'] = []): Cursor => ({ path, insertAt })

describe('insertDigit', () => {
  it('inserts NumberNode at empty expression', () => {
    const [nodes, cur] = insertDigit([], INITIAL_CURSOR, '3')
    expect(nodes).toEqual([{ type: 'number', value: '3' }])
    expect(cur).toEqual(C(1))
  })

  it('appends to adjacent NumberNode', () => {
    const init: ASTNode[] = [{ type: 'number', value: '3' }]
    const [nodes, cur] = insertDigit(init, C(1), '5')
    expect(nodes).toEqual([{ type: 'number', value: '35' }])
    expect(cur).toEqual(C(1))
  })

  it('inserts new NumberNode after operator', () => {
    const init: ASTNode[] = [{ type: 'operator', op: '+' }]
    const [nodes] = insertDigit(init, C(1), '5')
    expect(nodes[1]).toEqual({ type: 'number', value: '5' })
  })
})

describe('insertDecimalPoint', () => {
  it('appends dot to existing number', () => {
    const init: ASTNode[] = [{ type: 'number', value: '3' }]
    const [nodes] = insertDecimalPoint(init, C(1))
    expect((nodes[0] as any).value).toBe('3.')
  })

  it('creates 0. node when no prior number', () => {
    const [nodes] = insertDecimalPoint([], INITIAL_CURSOR)
    expect(nodes[0]).toEqual({ type: 'number', value: '0.' })
  })

  it('does not add second dot', () => {
    const init: ASTNode[] = [{ type: 'number', value: '3.' }]
    const [nodes] = insertDecimalPoint(init, C(1))
    expect((nodes[0] as any).value).toBe('3.')
  })
})

describe('insertOperator', () => {
  it('inserts operator node', () => {
    const [nodes] = insertOperator([], INITIAL_CURSOR, '+')
    expect(nodes[0]).toEqual({ type: 'operator', op: '+' })
  })
})

describe('insertFraction', () => {
  it('inserts fraction template and positions cursor in numerator', () => {
    const [nodes, cur] = insertFraction([], INITIAL_CURSOR)
    expect(nodes[0]).toMatchObject({ type: 'fraction', numerator: [], denominator: [] })
    expect(cur).toEqual({ path: [{ nodeIndex: 0, slot: 'numerator' }], insertAt: 0 })
  })
})

describe('insertExponent', () => {
  it('wraps previous node as base, cursor in exponent slot', () => {
    const init: ASTNode[] = [{ type: 'number', value: '2' }]
    const [nodes, cur] = insertExponent(init, C(1))
    expect(nodes[0]).toMatchObject({
      type: 'exponent',
      base: [{ type: 'number', value: '2' }],
      exponent: [],
    })
    expect(cur.path[0].slot).toBe('exponent')
    expect(cur.insertAt).toBe(0)
  })

  it('creates empty base when nothing before cursor', () => {
    const [nodes, cur] = insertExponent([], INITIAL_CURSOR)
    expect((nodes[0] as any).base).toEqual([])
    expect(cur.path[0].slot).toBe('exponent')
  })
})

describe('insertRadical', () => {
  it('inserts sqrt with empty degree, cursor in radicand', () => {
    const [nodes, cur] = insertRadical([], INITIAL_CURSOR, false)
    expect(nodes[0]).toMatchObject({ type: 'radical', degree: [], radicand: [] })
    expect(cur.path[0].slot).toBe('radicand')
  })

  it('inserts cbrt with degree=[3]', () => {
    const [nodes] = insertRadical([], INITIAL_CURSOR, true)
    expect((nodes[0] as any).degree).toEqual([{ type: 'number', value: '3' }])
  })
})

describe('insertSquare', () => {
  it('wraps previous node in exponent with exponent=[2]', () => {
    const init: ASTNode[] = [{ type: 'number', value: '4' }]
    const [nodes, cur] = insertSquare(init, C(1))
    expect(nodes[0]).toMatchObject({
      type: 'exponent',
      base: [{ type: 'number', value: '4' }],
      exponent: [{ type: 'number', value: '2' }],
    })
    expect(cur).toEqual(C(1))
  })
})

describe('insertFunction', () => {
  it('inserts function node, cursor in argument', () => {
    const [nodes, cur] = insertFunction([], INITIAL_CURSOR, 'sin')
    expect(nodes[0]).toMatchObject({ type: 'function', name: 'sin', argument: [] })
    expect(cur.path[0].slot).toBe('argument')
  })
})

describe('insertConstant', () => {
  it('inserts pi constant', () => {
    const [nodes] = insertConstant([], INITIAL_CURSOR, 'pi')
    expect(nodes[0]).toEqual({ type: 'constant', name: 'pi' })
  })
})

describe('insertFactorial', () => {
  it('wraps previous node in factorial', () => {
    const init: ASTNode[] = [{ type: 'number', value: '5' }]
    const [nodes, cur] = insertFactorial(init, C(1))
    expect(nodes[0]).toMatchObject({ type: 'factorial', operand: [{ type: 'number', value: '5' }] })
    expect(cur).toEqual(C(1))
  })
})

describe('insertNcr', () => {
  it('wraps previous node as n, cursor in r slot', () => {
    const init: ASTNode[] = [{ type: 'number', value: '5' }]
    const [nodes, cur] = insertNcr(init, C(1))
    expect(nodes[0]).toMatchObject({ type: 'ncr', n: [{ type: 'number', value: '5' }], r: [] })
    expect(cur.path[0].slot).toBe('r')
  })
})

describe('insertNpr', () => {
  it('wraps previous node as n, cursor in r slot', () => {
    const init: ASTNode[] = [{ type: 'number', value: '5' }]
    const [nodes, cur] = insertNpr(init, C(1))
    expect(nodes[0]).toMatchObject({ type: 'npr', n: [{ type: 'number', value: '5' }], r: [] })
    expect(cur.path[0].slot).toBe('r')
  })
})

describe('insertParen', () => {
  it('open: inserts paren node, cursor in children', () => {
    const [nodes, cur] = insertParen([], INITIAL_CURSOR, 'open')
    expect(nodes[0]).toMatchObject({ type: 'paren', children: [] })
    expect(cur.path[0].slot).toBe('children')
  })

  it('close: exits to parent level after paren node', () => {
    const init: ASTNode[] = [{ type: 'paren', children: [] }]
    const inner: Cursor = { path: [{ nodeIndex: 0, slot: 'children' }], insertAt: 0 }
    const [, cur] = insertParen(init, inner, 'close')
    expect(cur).toEqual(C(1))
  })
})

describe('deleteCurrent', () => {
  it('does nothing when expression empty at root', () => {
    const [nodes, cur] = deleteCurrent([], INITIAL_CURSOR)
    expect(nodes).toEqual([])
    expect(cur).toEqual(INITIAL_CURSOR)
  })

  it('removes last node', () => {
    const init: ASTNode[] = [{ type: 'number', value: '3' }, { type: 'operator', op: '+' }]
    const [nodes, cur] = deleteCurrent(init, C(2))
    expect(nodes).toEqual([{ type: 'number', value: '3' }])
    expect(cur).toEqual(C(1))
  })

  it('removes last digit from multi-digit number', () => {
    const init: ASTNode[] = [{ type: 'number', value: '35' }]
    const [nodes, cur] = deleteCurrent(init, C(1))
    expect((nodes[0] as any).value).toBe('3')
    expect(cur).toEqual(C(1))
  })

  it('exits to parent when at position 0 in a slot', () => {
    const init: ASTNode[] = [{ type: 'fraction', numerator: [], denominator: [] }]
    const inner: Cursor = { path: [{ nodeIndex: 0, slot: 'numerator' }], insertAt: 0 }
    const [, cur] = deleteCurrent(init, inner)
    expect(cur).toEqual(C(0))
  })
})

describe('clearAll', () => {
  it('returns empty expression and reset cursor', () => {
    const [nodes, cur] = clearAll()
    expect(nodes).toEqual([])
    expect(cur).toEqual(INITIAL_CURSOR)
  })
})
```

- [ ] **Step 2: Run tests — expect all to fail**

```bash
npx vitest run src/lib/ast/builder.test.ts 2>&1 | tail -5
```
Expected: `FAIL` — module not found.

- [ ] **Step 3: Write `src/lib/ast/builder.ts`**

```typescript
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
```

- [ ] **Step 4: Run tests — expect all to pass**

```bash
npx vitest run src/lib/ast/builder.test.ts 2>&1 | tail -5
```
Expected: `✓ src/lib/ast/builder.test.ts` with all tests passing.

- [ ] **Step 5: Commit**

```bash
git add src/lib/ast/builder.ts src/lib/ast/builder.test.ts
git commit -m "feat: AST builder with full TDD coverage"
```

---

## Task 4: AST serializer (TDD)

**Files:**
- Create: `src/lib/ast/serializer.ts`, `src/lib/ast/serializer.test.ts`

- [ ] **Step 1: Write `src/lib/ast/serializer.test.ts`**

```typescript
import { describe, it, expect } from 'vitest'
import { serialize } from './serializer'
import { ASTNode } from './types'

describe('serialize', () => {
  it('number', () => expect(serialize([{ type: 'number', value: '42' }])).toBe('42'))
  it('+ operator', () => expect(serialize([{ type: 'operator', op: '+' }])).toBe('+'))
  it('× operator maps to *', () => expect(serialize([{ type: 'operator', op: '×' }])).toBe('*'))
  it('÷ operator maps to /', () => expect(serialize([{ type: 'operator', op: '÷' }])).toBe('/'))
  it('pi constant', () => expect(serialize([{ type: 'constant', name: 'pi' }])).toBe('pi'))
  it('e constant', () => expect(serialize([{ type: 'constant', name: 'e' }])).toBe('e'))
  it('Ans constant', () => expect(serialize([{ type: 'constant', name: 'Ans' }])).toBe('ans'))

  it('fraction', () => {
    const nodes: ASTNode[] = [{
      type: 'fraction',
      numerator: [{ type: 'number', value: '1' }],
      denominator: [{ type: 'number', value: '2' }],
    }]
    expect(serialize(nodes)).toBe('(1)/(2)')
  })

  it('exponent', () => {
    const nodes: ASTNode[] = [{
      type: 'exponent',
      base: [{ type: 'number', value: '2' }],
      exponent: [{ type: 'number', value: '3' }],
    }]
    expect(serialize(nodes)).toBe('(2)^(3)')
  })

  it('sqrt radical (empty degree)', () => {
    const nodes: ASTNode[] = [{
      type: 'radical',
      degree: [],
      radicand: [{ type: 'number', value: '4' }],
    }]
    expect(serialize(nodes)).toBe('sqrt(4)')
  })

  it('nth root radical', () => {
    const nodes: ASTNode[] = [{
      type: 'radical',
      degree: [{ type: 'number', value: '3' }],
      radicand: [{ type: 'number', value: '8' }],
    }]
    expect(serialize(nodes)).toBe('nthRoot(8, 3)')
  })

  it('sin function', () => {
    const nodes: ASTNode[] = [{ type: 'function', name: 'sin', argument: [{ type: 'constant', name: 'pi' }] }]
    expect(serialize(nodes)).toBe('sin(pi)')
  })

  it('log maps to log base 10', () => {
    const nodes: ASTNode[] = [{ type: 'function', name: 'log', argument: [{ type: 'number', value: '100' }] }]
    expect(serialize(nodes)).toBe('log(100, 10)')
  })

  it('ln maps to natural log', () => {
    const nodes: ASTNode[] = [{ type: 'function', name: 'ln', argument: [{ type: 'number', value: '1' }] }]
    expect(serialize(nodes)).toBe('log(1)')
  })

  it('pow10', () => {
    const nodes: ASTNode[] = [{ type: 'function', name: 'pow10', argument: [{ type: 'number', value: '2' }] }]
    expect(serialize(nodes)).toBe('10^(2)')
  })

  it('exp (e^x)', () => {
    const nodes: ASTNode[] = [{ type: 'function', name: 'exp', argument: [{ type: 'number', value: '1' }] }]
    expect(serialize(nodes)).toBe('e^(1)')
  })

  it('factorial', () => {
    const nodes: ASTNode[] = [{ type: 'factorial', operand: [{ type: 'number', value: '5' }] }]
    expect(serialize(nodes)).toBe('factorial(5)')
  })

  it('nCr', () => {
    const nodes: ASTNode[] = [{
      type: 'ncr',
      n: [{ type: 'number', value: '5' }],
      r: [{ type: 'number', value: '2' }],
    }]
    expect(serialize(nodes)).toBe('combinations(5, 2)')
  })

  it('nPr', () => {
    const nodes: ASTNode[] = [{
      type: 'npr',
      n: [{ type: 'number', value: '5' }],
      r: [{ type: 'number', value: '2' }],
    }]
    expect(serialize(nodes)).toBe('permutations(5, 2)')
  })

  it('paren', () => {
    const nodes: ASTNode[] = [{ type: 'paren', children: [{ type: 'number', value: '3' }] }]
    expect(serialize(nodes)).toBe('(3)')
  })

  it('complex: 1/2 + sin(pi)', () => {
    const nodes: ASTNode[] = [
      { type: 'fraction', numerator: [{ type: 'number', value: '1' }], denominator: [{ type: 'number', value: '2' }] },
      { type: 'operator', op: '+' },
      { type: 'function', name: 'sin', argument: [{ type: 'constant', name: 'pi' }] },
    ]
    expect(serialize(nodes)).toBe('(1)/(2)+sin(pi)')
  })

  it('empty array returns empty string', () => {
    expect(serialize([])).toBe('')
  })
})
```

- [ ] **Step 2: Run — expect FAIL (module not found)**

```bash
npx vitest run src/lib/ast/serializer.test.ts 2>&1 | tail -3
```

- [ ] **Step 3: Write `src/lib/ast/serializer.ts`**

```typescript
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
```

- [ ] **Step 4: Run — expect all to pass**

```bash
npx vitest run src/lib/ast/serializer.test.ts 2>&1 | tail -3
```

- [ ] **Step 5: Commit**

```bash
git add src/lib/ast/serializer.ts src/lib/ast/serializer.test.ts
git commit -m "feat: AST serializer with full TDD coverage"
```

---

## Task 5: AST cursor navigation (TDD)

**Files:**
- Create: `src/lib/ast/cursor.ts`, `src/lib/ast/cursor.test.ts`

- [ ] **Step 1: Write `src/lib/ast/cursor.test.ts`**

```typescript
import { describe, it, expect } from 'vitest'
import { moveCursorLeft, moveCursorRight, moveCursorUp, moveCursorDown } from './cursor'
import { ASTNode, Cursor } from './types'

const C = (insertAt: number, path: Cursor['path'] = []): Cursor => ({ path, insertAt })

const fraction: ASTNode[] = [{
  type: 'fraction',
  numerator: [{ type: 'number', value: '1' }],
  denominator: [{ type: 'number', value: '2' }],
}]

const exprAB: ASTNode[] = [
  { type: 'number', value: 'A' },
  { type: 'number', value: 'B' },
]

describe('moveCursorRight', () => {
  it('moves right in flat list', () => {
    expect(moveCursorRight(exprAB, C(0))).toEqual(C(1))
  })

  it('entering fraction from left → numerator start', () => {
    expect(moveCursorRight(fraction, C(0))).toEqual(
      C(0, [{ nodeIndex: 0, slot: 'numerator' }])
    )
  })

  it('numerator end → denominator start', () => {
    const cur = C(1, [{ nodeIndex: 0, slot: 'numerator' }])
    expect(moveCursorRight(fraction, cur)).toEqual(
      C(0, [{ nodeIndex: 0, slot: 'denominator' }])
    )
  })

  it('denominator end → exits fraction to right', () => {
    const cur = C(1, [{ nodeIndex: 0, slot: 'denominator' }])
    expect(moveCursorRight(fraction, cur)).toEqual(C(1))
  })

  it('stays at root end', () => {
    expect(moveCursorRight(exprAB, C(2))).toEqual(C(2))
  })
})

describe('moveCursorLeft', () => {
  it('moves left in flat list', () => {
    expect(moveCursorLeft(exprAB, C(2))).toEqual(C(1))
  })

  it('denominator start → numerator end', () => {
    const cur = C(0, [{ nodeIndex: 0, slot: 'denominator' }])
    expect(moveCursorLeft(fraction, cur)).toEqual(
      C(1, [{ nodeIndex: 0, slot: 'numerator' }])
    )
  })

  it('numerator start → exits to before fraction', () => {
    const cur = C(0, [{ nodeIndex: 0, slot: 'numerator' }])
    expect(moveCursorLeft(fraction, cur)).toEqual(C(0))
  })

  it('stays at root start', () => {
    expect(moveCursorLeft(exprAB, C(0))).toEqual(C(0))
  })
})

describe('moveCursorUp', () => {
  it('denominator → numerator', () => {
    const cur = C(0, [{ nodeIndex: 0, slot: 'denominator' }])
    const result = moveCursorUp(fraction, cur)
    expect(result.path[result.path.length - 1].slot).toBe('numerator')
  })

  it('does nothing at root', () => {
    expect(moveCursorUp(fraction, C(0))).toEqual(C(0))
  })

  it('does nothing in numerator already', () => {
    const cur = C(0, [{ nodeIndex: 0, slot: 'numerator' }])
    expect(moveCursorUp(fraction, cur)).toEqual(cur)
  })
})

describe('moveCursorDown', () => {
  it('numerator → denominator', () => {
    const cur = C(0, [{ nodeIndex: 0, slot: 'numerator' }])
    const result = moveCursorDown(fraction, cur)
    expect(result.path[result.path.length - 1].slot).toBe('denominator')
  })

  it('does nothing at root', () => {
    expect(moveCursorDown(fraction, C(0))).toEqual(C(0))
  })
})
```

- [ ] **Step 2: Run — expect FAIL**

```bash
npx vitest run src/lib/ast/cursor.test.ts 2>&1 | tail -3
```

- [ ] **Step 3: Write `src/lib/ast/cursor.ts`**

```typescript
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
```

- [ ] **Step 4: Run — expect all pass**

```bash
npx vitest run src/lib/ast/cursor.test.ts 2>&1 | tail -3
```

- [ ] **Step 5: Commit**

```bash
git add src/lib/ast/cursor.ts src/lib/ast/cursor.test.ts
git commit -m "feat: AST cursor navigation with full TDD coverage"
```

---

## Task 6: Evaluator (TDD)

**Files:**
- Create: `src/lib/evaluator.ts`, `src/lib/evaluator.test.ts`

- [ ] **Step 1: Write `src/lib/evaluator.test.ts`**

```typescript
import { describe, it, expect } from 'vitest'
import { evaluate } from './evaluator'
import { ASTNode } from './ast/types'

const num = (v: string): ASTNode => ({ type: 'number', value: v })
const op  = (o: '+' | '-' | '×' | '÷'): ASTNode => ({ type: 'operator', op: o })
const con = (n: 'pi' | 'e' | 'Ans'): ASTNode => ({ type: 'constant', name: n })
const fn  = (name: any, arg: ASTNode[]): ASTNode => ({ type: 'function', name, argument: arg })

describe('evaluate', () => {
  it('returns null for empty expression', () => {
    expect(evaluate([], 'RAD', null)).toBeNull()
  })

  it('returns null for invalid expression (lone operator)', () => {
    expect(evaluate([op('+')], 'RAD', null)).toBeNull()
  })

  it('evaluates simple addition', () => {
    expect(evaluate([num('2'), op('+'), num('3')], 'RAD', null)?.value).toBe('5')
  })

  it('evaluates fraction 1/4 = 0.25', () => {
    const nodes: ASTNode[] = [{
      type: 'fraction',
      numerator: [num('1')],
      denominator: [num('4')],
    }]
    expect(evaluate(nodes, 'RAD', null)?.value).toBe('0.25')
  })

  it('evaluates sin(pi) ≈ 0 in RAD', () => {
    const result = evaluate([fn('sin', [con('pi')])], 'RAD', null)
    expect(Math.abs(parseFloat(result!.value))).toBeLessThan(1e-10)
  })

  it('evaluates sin(90) = 1 in DEG', () => {
    const result = evaluate([fn('sin', [num('90')])], 'DEG', null)
    expect(parseFloat(result!.value)).toBeCloseTo(1, 10)
  })

  it('evaluates cos(0) = 1 in DEG', () => {
    const result = evaluate([fn('cos', [num('0')])], 'DEG', null)
    expect(parseFloat(result!.value)).toBeCloseTo(1, 10)
  })

  it('substitutes Ans', () => {
    const result = evaluate([con('Ans'), op('+'), num('1')], 'RAD', '5')
    expect(result?.value).toBe('6')
  })

  it('uses 0 for Ans when lastResult is null', () => {
    const result = evaluate([con('Ans'), op('+'), num('3')], 'RAD', null)
    expect(result?.value).toBe('3')
  })

  it('evaluates sqrt(4) = 2', () => {
    const nodes: ASTNode[] = [{ type: 'radical', degree: [], radicand: [num('4')] }]
    expect(evaluate(nodes, 'RAD', null)?.value).toBe('2')
  })

  it('evaluates 2^10 = 1024', () => {
    const nodes: ASTNode[] = [{ type: 'exponent', base: [num('2')], exponent: [num('10')] }]
    expect(evaluate(nodes, 'RAD', null)?.value).toBe('1024')
  })

  it('evaluates 5! = 120', () => {
    expect(evaluate([{ type: 'factorial', operand: [num('5')] }], 'RAD', null)?.value).toBe('120')
  })

  it('evaluates 5C2 = 10', () => {
    const nodes: ASTNode[] = [{ type: 'ncr', n: [num('5')], r: [num('2')] }]
    expect(evaluate(nodes, 'RAD', null)?.value).toBe('10')
  })

  it('evaluates log(100) = 2', () => {
    expect(evaluate([fn('log', [num('100')])], 'RAD', null)?.value).toBe('2')
  })

  it('evaluates ln(e) ≈ 1', () => {
    const result = evaluate([fn('ln', [con('e')])], 'RAD', null)
    expect(parseFloat(result!.value)).toBeCloseTo(1, 10)
  })
})
```

- [ ] **Step 2: Run — expect FAIL**

```bash
npx vitest run src/lib/evaluator.test.ts 2>&1 | tail -3
```

- [ ] **Step 3: Write `src/lib/evaluator.ts`**

```typescript
import { create, all } from 'mathjs'
import { ASTNode } from './ast/types'
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

export function evaluate(nodes: ASTNode[], angleMode: 'DEG' | 'RAD', lastResult: string | null): EvalResult | null {
  const expr = serialize(nodes)
  if (!expr.trim()) return null

  const withAns = expr.replace(/\bans\b/gi, lastResult ?? '0')
  const engine = angleMode === 'DEG' ? mathDeg : mathRad

  try {
    const raw = engine.evaluate(withAns)
    if (raw === undefined || raw === null) return null
    const n = typeof raw === 'number' ? raw : Number(raw)
    if (!isFinite(n)) return { value: 'Math Error' }
    return { value: formatNumber(n) }
  } catch {
    return null
  }
}

function formatNumber(n: number): string {
  if (Number.isInteger(n) && Math.abs(n) < 1e15) return String(n)
  const s = n.toPrecision(10)
  return String(parseFloat(s))
}
```

- [ ] **Step 4: Run — expect all pass**

```bash
npx vitest run src/lib/evaluator.test.ts 2>&1 | tail -3
```

- [ ] **Step 5: Commit**

```bash
git add src/lib/evaluator.ts src/lib/evaluator.test.ts
git commit -m "feat: evaluator with angle mode and Ans substitution"
```

---

## Task 7: History (TDD)

**Files:**
- Create: `src/lib/history.ts`, `src/lib/history.test.ts`

- [ ] **Step 1: Write `src/lib/history.test.ts`**

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { loadHistory, addEntry, clearHistory } from './history'
import { ASTNode } from './ast/types'

const store: Record<string, string> = {}
const lsMock = {
  getItem:    vi.fn((k: string) => store[k] ?? null),
  setItem:    vi.fn((k: string, v: string) => { store[k] = v }),
  removeItem: vi.fn((k: string) => { delete store[k] }),
}
Object.defineProperty(globalThis, 'localStorage', { value: lsMock, writable: true })

const expr: ASTNode[] = [{ type: 'number', value: '42' }]

beforeEach(() => { Object.keys(store).forEach(k => delete store[k]); vi.clearAllMocks() })

describe('loadHistory', () => {
  it('returns [] when nothing stored', () => expect(loadHistory()).toEqual([]))
  it('returns [] on corrupted data', () => {
    store['calc_history'] = 'bad json'
    expect(loadHistory()).toEqual([])
  })
})

describe('addEntry', () => {
  it('prepends entry and persists', () => {
    const entries = addEntry([], expr, '42')
    expect(entries[0].result).toBe('42')
    expect(entries[0].expression).toEqual(expr)
    expect(lsMock.setItem).toHaveBeenCalled()
  })

  it('most recent is first', () => {
    const e1 = addEntry([], expr, 'first')
    const e2 = addEntry(e1, expr, 'second')
    expect(e2[0].result).toBe('second')
    expect(e2[1].result).toBe('first')
  })

  it('caps at 50 entries', () => {
    let entries = []
    for (let i = 0; i < 55; i++) entries = addEntry(entries as any, expr, String(i))
    expect(entries).toHaveLength(50)
  })
})

describe('clearHistory', () => {
  it('removes from localStorage and returns []', () => {
    const cleared = clearHistory()
    expect(cleared).toEqual([])
    expect(lsMock.removeItem).toHaveBeenCalled()
  })
})
```

- [ ] **Step 2: Run — expect FAIL**

```bash
npx vitest run src/lib/history.test.ts 2>&1 | tail -3
```

- [ ] **Step 3: Write `src/lib/history.ts`**

```typescript
import { ASTNode, HistoryEntry } from './ast/types'

const KEY = 'calc_history'
const MAX = 50

export function loadHistory(): HistoryEntry[] {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as HistoryEntry[]) : []
  } catch { return [] }
}

export function addEntry(entries: HistoryEntry[], expression: ASTNode[], result: string): HistoryEntry[] {
  const next = [
    { id: crypto.randomUUID(), expression, result, timestamp: Date.now() },
    ...entries,
  ].slice(0, MAX)
  localStorage.setItem(KEY, JSON.stringify(next))
  return next
}

export function clearHistory(): HistoryEntry[] {
  localStorage.removeItem(KEY)
  return []
}
```

- [ ] **Step 4: Run — expect all pass**

```bash
npx vitest run src/lib/history.test.ts 2>&1 | tail -3
```

- [ ] **Step 5: Run all lib tests together**

```bash
npx vitest run src/lib 2>&1 | tail -5
```
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add src/lib/history.ts src/lib/history.test.ts
git commit -m "feat: history with localStorage persistence"
```

---

## Task 8: AST node components (TDD)

**Files:**
- Create: `src/components/ast/nodes/NumberNode.tsx`, `OperatorNode.tsx`, `ConstantNode.tsx`, `FractionNode.tsx`, `ExponentNode.tsx`, `RadicalNode.tsx`, `FunctionNode.tsx`
- Create: `src/components/ast/Cursor.tsx`
- Create: `src/components/ast/ASTRenderer.tsx`
- Create: `src/components/ast/ASTRenderer.test.tsx`

- [ ] **Step 1: Write the leaf node components** (no tests needed — trivial presentational)

`src/components/ast/nodes/NumberNode.tsx`:
```tsx
import { NumberNode } from '@/lib/ast/types'
export function NumberNodeView({ node }: { node: NumberNode }) {
  return <span>{node.value}</span>
}
```

`src/components/ast/nodes/OperatorNode.tsx`:
```tsx
import { OperatorNode } from '@/lib/ast/types'
const SYM: Record<string, string> = { '+': '+', '-': '−', '×': '×', '÷': '÷' }
export function OperatorNodeView({ node }: { node: OperatorNode }) {
  return <span className="px-0.5">{SYM[node.op] ?? node.op}</span>
}
```

`src/components/ast/nodes/ConstantNode.tsx`:
```tsx
import { ConstantNode } from '@/lib/ast/types'
const LABELS: Record<string, string> = { pi: 'π', e: 'e', Ans: 'Ans' }
export function ConstantNodeView({ node }: { node: ConstantNode }) {
  return <span className="italic">{LABELS[node.name]}</span>
}
```

`src/components/ast/Cursor.tsx`:
```tsx
export function CursorView() {
  return <span className="inline-block w-0.5 h-[1.2em] bg-white animate-pulse mx-px align-middle" aria-hidden />
}
```

- [ ] **Step 2: Write the container node components**

`src/components/ast/nodes/FractionNode.tsx`:
```tsx
import { FractionNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'

interface Props { node: FractionNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number }

export function FractionNodeView({ node, cursor, path, nodeIndex }: Props) {
  return (
    <span className="inline-flex flex-col items-center mx-0.5 align-middle">
      <span className="px-1 min-w-4 text-center leading-tight">
        <ASTRenderer nodes={node.numerator} cursor={cursor} path={[...path, { nodeIndex, slot: 'numerator' }]} />
      </span>
      <span className="w-full border-t border-current" />
      <span className="px-1 min-w-4 text-center leading-tight">
        <ASTRenderer nodes={node.denominator} cursor={cursor} path={[...path, { nodeIndex, slot: 'denominator' }]} />
      </span>
    </span>
  )
}
```

`src/components/ast/nodes/ExponentNode.tsx`:
```tsx
import { ExponentNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { ASTRenderer } from '../ASTRenderer'

interface Props { node: ExponentNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number }

export function ExponentNodeView({ node, cursor, path, nodeIndex }: Props) {
  return (
    <span className="inline-flex items-start">
      <ASTRenderer nodes={node.base} cursor={cursor} path={[...path, { nodeIndex, slot: 'base' }]} />
      <span className="text-[0.65em] -mt-1 ml-px">
        <ASTRenderer nodes={node.exponent} cursor={cursor} path={[...path, { nodeIndex, slot: 'exponent' }]} />
      </span>
    </span>
  )
}
```

`src/components/ast/nodes/RadicalNode.tsx`:
```tsx
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
```

`src/components/ast/nodes/FunctionNode.tsx`:
```tsx
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
```

- [ ] **Step 3: Write `src/components/ast/ASTRenderer.tsx`**

```tsx
import { ASTNode, Cursor, CursorSegment } from '@/lib/ast/types'
import { NumberNodeView } from './nodes/NumberNode'
import { OperatorNodeView } from './nodes/OperatorNode'
import { ConstantNodeView } from './nodes/ConstantNode'
import { FractionNodeView } from './nodes/FractionNode'
import { ExponentNodeView } from './nodes/ExponentNode'
import { RadicalNodeView } from './nodes/RadicalNode'
import { FunctionNodeView } from './nodes/FunctionNode'
import { CursorView } from './Cursor'

interface Props {
  nodes: ASTNode[]
  cursor: Cursor
  path: CursorSegment[]
}

function pathsEqual(a: CursorSegment[], b: CursorSegment[]): boolean {
  return a.length === b.length && a.every((s, i) => s.nodeIndex === b[i].nodeIndex && s.slot === b[i].slot)
}

export function ASTRenderer({ nodes, cursor, path }: Props) {
  const isHere = pathsEqual(cursor.path, path)

  return (
    <span className="inline-flex items-center flex-wrap">
      {nodes.map((node, index) => (
        <span key={index} className="inline-flex items-center">
          {isHere && cursor.insertAt === index && <CursorView />}
          <NodeView node={node} cursor={cursor} path={path} nodeIndex={index} />
        </span>
      ))}
      {isHere && cursor.insertAt === nodes.length && <CursorView />}
    </span>
  )
}

function NodeView({ node, cursor, path, nodeIndex }: { node: ASTNode; cursor: Cursor; path: CursorSegment[]; nodeIndex: number }) {
  switch (node.type) {
    case 'number':   return <NumberNodeView node={node} />
    case 'operator': return <OperatorNodeView node={node} />
    case 'constant': return <ConstantNodeView node={node} />
    case 'fraction': return <FractionNodeView node={node} cursor={cursor} path={path} nodeIndex={nodeIndex} />
    case 'exponent': return <ExponentNodeView node={node} cursor={cursor} path={path} nodeIndex={nodeIndex} />
    case 'radical':  return <RadicalNodeView  node={node} cursor={cursor} path={path} nodeIndex={nodeIndex} />
    case 'function': return <FunctionNodeView node={node} cursor={cursor} path={path} nodeIndex={nodeIndex} />
    case 'factorial':
      return (
        <span className="inline-flex items-center">
          <ASTRenderer nodes={node.operand} cursor={cursor} path={[...path, { nodeIndex, slot: 'operand' }]} />
          <span>!</span>
        </span>
      )
    case 'ncr':
      return (
        <span className="inline-flex items-baseline gap-0.5">
          <ASTRenderer nodes={node.n} cursor={cursor} path={[...path, { nodeIndex, slot: 'n' }]} />
          <span className="text-[0.65em]">C</span>
          <ASTRenderer nodes={node.r} cursor={cursor} path={[...path, { nodeIndex, slot: 'r' }]} />
        </span>
      )
    case 'npr':
      return (
        <span className="inline-flex items-baseline gap-0.5">
          <ASTRenderer nodes={node.n} cursor={cursor} path={[...path, { nodeIndex, slot: 'n' }]} />
          <span className="text-[0.65em]">P</span>
          <ASTRenderer nodes={node.r} cursor={cursor} path={[...path, { nodeIndex, slot: 'r' }]} />
        </span>
      )
    case 'paren':
      return (
        <span className="inline-flex items-center">
          <span>(</span>
          <ASTRenderer nodes={node.children} cursor={cursor} path={[...path, { nodeIndex, slot: 'children' }]} />
          <span>)</span>
        </span>
      )
  }
}
```

- [ ] **Step 4: Write `src/components/ast/ASTRenderer.test.tsx`**

```tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ASTRenderer } from './ASTRenderer'
import { ASTNode, INITIAL_CURSOR } from '@/lib/ast/types'

describe('ASTRenderer', () => {
  it('renders a number node', () => {
    const nodes: ASTNode[] = [{ type: 'number', value: '42' }]
    render(<ASTRenderer nodes={nodes} cursor={INITIAL_CURSOR} path={[]} />)
    expect(screen.getByText('42')).toBeInTheDocument()
  })

  it('renders fraction with top and bottom parts', () => {
    const nodes: ASTNode[] = [{
      type: 'fraction',
      numerator: [{ type: 'number', value: '1' }],
      denominator: [{ type: 'number', value: '2' }],
    }]
    render(<ASTRenderer nodes={nodes} cursor={INITIAL_CURSOR} path={[]} />)
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('renders exponent with superscript', () => {
    const nodes: ASTNode[] = [{
      type: 'exponent',
      base: [{ type: 'number', value: '2' }],
      exponent: [{ type: 'number', value: '3' }],
    }]
    render(<ASTRenderer nodes={nodes} cursor={INITIAL_CURSOR} path={[]} />)
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('renders sin function with parentheses', () => {
    const nodes: ASTNode[] = [{
      type: 'function', name: 'sin',
      argument: [{ type: 'constant', name: 'pi' }],
    }]
    render(<ASTRenderer nodes={nodes} cursor={INITIAL_CURSOR} path={[]} />)
    expect(screen.getByText(/sin\(/)).toBeInTheDocument()
    expect(screen.getByText('π')).toBeInTheDocument()
  })

  it('renders blinking cursor at insertAt position', () => {
    const nodes: ASTNode[] = [{ type: 'number', value: '3' }]
    const cursor = { path: [], insertAt: 0 }
    const { container } = render(<ASTRenderer nodes={nodes} cursor={cursor} path={[]} />)
    expect(container.querySelector('.animate-pulse')).toBeInTheDocument()
  })
})
```

- [ ] **Step 5: Run component tests**

```bash
npx vitest run src/components/ast/ASTRenderer.test.tsx 2>&1 | tail -5
```
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add src/components/ast/
git commit -m "feat: AST renderer and node components"
```

---

## Task 9: Calculator components (TDD)

**Files:**
- Create: `src/components/calculator/Display.tsx`, `Display.test.tsx`
- Create: `src/components/calculator/ButtonGrid.tsx`, `ButtonGrid.test.tsx`
- Create: `src/components/calculator/HistoryPanel.tsx`, `HistoryPanel.test.tsx`

- [ ] **Step 1: Write `src/components/calculator/Display.test.tsx`**

```tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Display } from './Display'
import { INITIAL_CURSOR } from '@/lib/ast/types'

describe('Display', () => {
  it('shows placeholder 0 when expression empty', () => {
    render(<Display expression={[]} cursor={INITIAL_CURSOR} result={null} />)
    expect(screen.getByText('0')).toBeInTheDocument()
  })

  it('shows result line when result provided', () => {
    render(<Display expression={[]} cursor={INITIAL_CURSOR} result="42" />)
    expect(screen.getByText(/42/)).toBeInTheDocument()
  })

  it('does not show result line when result is null', () => {
    const { container } = render(<Display expression={[]} cursor={INITIAL_CURSOR} result={null} />)
    expect(container.querySelectorAll('.text-zinc-400').length).toBe(0)
  })
})
```

- [ ] **Step 2: Write `src/components/calculator/Display.tsx`**

```tsx
import { ASTNode, Cursor } from '@/lib/ast/types'
import { ASTRenderer } from '../ast/ASTRenderer'

interface Props {
  expression: ASTNode[]
  cursor: Cursor
  result: string | null
}

export function Display({ expression, cursor, result }: Props) {
  const empty = expression.length === 0 && cursor.path.length === 0 && cursor.insertAt === 0

  return (
    <div className="flex flex-col min-h-28 p-3 gap-2 bg-black border-b border-zinc-800 overflow-hidden">
      <div className="flex-1 overflow-x-auto flex items-center text-white text-2xl font-light">
        {empty
          ? <span className="text-zinc-700">0</span>
          : <ASTRenderer nodes={expression} cursor={cursor} path={[]} />
        }
      </div>
      {result !== null && (
        <div className="text-right text-zinc-400 text-base">
          = {result}
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 3: Run Display tests**

```bash
npx vitest run src/components/calculator/Display.test.tsx 2>&1 | tail -3
```
Expected: all pass.

- [ ] **Step 4: Write `src/components/calculator/ButtonGrid.test.tsx`**

```tsx
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ButtonGrid } from './ButtonGrid'

const props = { angleMode: 'DEG' as const, shiftActive: false, hypActive: false, onButton: vi.fn() }

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

  it('shows sin label when shift inactive', () => {
    render(<ButtonGrid {...props} />)
    expect(screen.getByText('sin')).toBeInTheDocument()
  })

  it('shows sin⁻¹ label when shift active', () => {
    render(<ButtonGrid {...props} shiftActive />)
    expect(screen.getByText('sin⁻¹')).toBeInTheDocument()
  })

  it('shows sinh label when hyp active', () => {
    render(<ButtonGrid {...props} hypActive />)
    expect(screen.getByText('sinh')).toBeInTheDocument()
  })

  it('shows DEG label matching angleMode', () => {
    render(<ButtonGrid {...props} angleMode="RAD" />)
    expect(screen.getByText('RAD')).toBeInTheDocument()
  })
})
```

- [ ] **Step 5: Write `src/components/calculator/ButtonGrid.tsx`**

```tsx
import { cn } from '@/lib/utils'

interface BtnDef {
  id: string; label: string; shiftLabel?: string; hypLabel?: string; shiftHypLabel?: string; cls?: string
}

const ROWS: (BtnDef | null)[][] = [
  [
    { id: 'SHIFT',   label: 'SHIFT',  cls: 'bg-orange-700' },
    { id: 'DEG_RAD', label: 'DEG' },
    { id: 'LEFT',    label: '◄' },
    { id: 'RIGHT',   label: '►' },
  ],
  [
    { id: 'log', label: 'log', shiftLabel: '10ˣ' },
    { id: 'ln',  label: 'ln',  shiftLabel: 'eˣ'  },
    { id: 'HYP', label: 'hyp' },
    { id: 'factorial', label: 'x!' },
  ],
  [
    { id: 'sin', label: 'sin', shiftLabel: 'sin⁻¹', hypLabel: 'sinh', shiftHypLabel: 'sinh⁻¹' },
    { id: 'cos', label: 'cos', shiftLabel: 'cos⁻¹', hypLabel: 'cosh', shiftHypLabel: 'cosh⁻¹' },
    { id: 'tan', label: 'tan', shiftLabel: 'tan⁻¹', hypLabel: 'tanh', shiftHypLabel: 'tanh⁻¹' },
    { id: 'exponent', label: 'xʸ' },
  ],
  [
    { id: 'sqrt',   label: '√',   shiftLabel: 'x²' },
    { id: 'cbrt',   label: '∛' },
    { id: 'fraction', label: 'a/b' },
    { id: 'square', label: 'x²', shiftLabel: 'x³' },
  ],
  [
    { id: 'ncr',         label: 'nCr' },
    { id: 'npr',         label: 'nPr' },
    { id: 'paren_open',  label: '('   },
    { id: 'paren_close', label: ')'   },
  ],
  [
    { id: '7', label: '7' }, { id: '8', label: '8' }, { id: '9', label: '9' },
    { id: 'DEL', label: 'DEL', cls: 'bg-zinc-700' },
  ],
  [
    { id: '4', label: '4' }, { id: '5', label: '5' }, { id: '6', label: '6' },
    { id: 'multiply', label: '×' },
  ],
  [
    { id: '1', label: '1' }, { id: '2', label: '2' }, { id: '3', label: '3' },
    { id: 'divide', label: '÷' },
  ],
  [
    { id: '0', label: '0' }, { id: '.', label: '.' }, { id: 'Ans', label: 'Ans' },
    { id: 'plus', label: '+' },
  ],
  [
    null,
    { id: '=', label: '=', cls: 'bg-blue-700' },
    null,
    { id: 'minus', label: '−' },
  ],
]

interface Props {
  angleMode: 'DEG' | 'RAD'
  shiftActive: boolean
  hypActive: boolean
  onButton: (id: string) => void
}

export function ButtonGrid({ angleMode, shiftActive, hypActive, onButton }: Props) {
  function label(btn: BtnDef): string {
    if (btn.id === 'DEG_RAD') return angleMode
    if (shiftActive && hypActive && btn.shiftHypLabel) return btn.shiftHypLabel
    if (shiftActive && btn.shiftLabel) return btn.shiftLabel
    if (hypActive && btn.hypLabel) return btn.hypLabel
    return btn.label
  }

  function actionId(btn: BtnDef): string {
    if (btn.id === 'sin') {
      if (shiftActive && hypActive) return 'asinh'
      if (shiftActive) return 'asin'
      if (hypActive)   return 'sinh'
      return 'sin'
    }
    if (btn.id === 'cos') {
      if (shiftActive && hypActive) return 'acosh'
      if (shiftActive) return 'acos'
      if (hypActive)   return 'cosh'
      return 'cos'
    }
    if (btn.id === 'tan') {
      if (shiftActive && hypActive) return 'atanh'
      if (shiftActive) return 'atan'
      if (hypActive)   return 'tanh'
      return 'tan'
    }
    if (btn.id === 'log'    && shiftActive) return 'pow10'
    if (btn.id === 'ln'     && shiftActive) return 'exp'
    if (btn.id === 'sqrt'   && shiftActive) return 'square'
    return btn.id
  }

  return (
    <div className="grid grid-cols-4 gap-1 p-2 bg-zinc-950 flex-shrink-0">
      {ROWS.flat().map((btn, i) => {
        if (!btn) return <div key={i} />
        return (
          <button
            key={btn.id + i}
            onClick={() => onButton(actionId(btn))}
            className={cn(
              'h-12 rounded text-white text-sm font-medium active:scale-95 transition-transform select-none',
              btn.cls ?? 'bg-zinc-800 hover:bg-zinc-700',
              btn.id === 'SHIFT' && shiftActive && 'ring-1 ring-orange-400',
              btn.id === 'HYP'   && hypActive   && 'ring-1 ring-blue-400',
            )}
          >
            {label(btn)}
          </button>
        )
      })}
    </div>
  )
}
```

- [ ] **Step 6: Run ButtonGrid tests**

```bash
npx vitest run src/components/calculator/ButtonGrid.test.tsx 2>&1 | tail -3
```

- [ ] **Step 7: Write `src/components/calculator/HistoryPanel.test.tsx`**

```tsx
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { HistoryPanel } from './HistoryPanel'
import { HistoryEntry } from '@/lib/ast/types'

const entry: HistoryEntry = {
  id: '1', result: '42', expression: [{ type: 'number', value: '42' }], timestamp: 0,
}

describe('HistoryPanel', () => {
  it('renders nothing when history empty', () => {
    const { container } = render(<HistoryPanel history={[]} onRestore={vi.fn()} />)
    expect(container.firstChild).toBeNull()
  })

  it('shows collapsed header with count', () => {
    render(<HistoryPanel history={[entry]} onRestore={vi.fn()} />)
    expect(screen.getByText(/History \(1\)/)).toBeInTheDocument()
  })

  it('expands and shows result on toggle', () => {
    render(<HistoryPanel history={[entry]} onRestore={vi.fn()} />)
    fireEvent.click(screen.getByRole('button'))
    expect(screen.getByText('42')).toBeInTheDocument()
  })

  it('calls onRestore when history item clicked', () => {
    const onRestore = vi.fn()
    render(<HistoryPanel history={[entry]} onRestore={onRestore} />)
    fireEvent.click(screen.getByRole('button'))
    fireEvent.click(screen.getByText('42'))
    expect(onRestore).toHaveBeenCalledWith(entry)
  })
})
```

- [ ] **Step 8: Write `src/components/calculator/HistoryPanel.tsx`**

```tsx
import { useState } from 'react'
import { HistoryEntry } from '@/lib/ast/types'

interface Props {
  history: HistoryEntry[]
  onRestore: (entry: HistoryEntry) => void
}

export function HistoryPanel({ history, onRestore }: Props) {
  const [open, setOpen] = useState(false)
  if (history.length === 0) return null

  return (
    <div className="bg-zinc-950 border-t border-zinc-800 flex-shrink-0">
      <button
        onClick={() => setOpen(o => !o)}
        className="w-full flex items-center px-3 py-1.5 text-zinc-500 text-xs hover:text-zinc-300"
      >
        {open ? '▲' : '▾'} History ({history.length})
      </button>
      {open && (
        <div className="max-h-28 overflow-y-auto">
          {history.map(entry => (
            <button
              key={entry.id}
              onClick={() => onRestore(entry)}
              className="w-full text-right px-3 py-1 text-sm text-zinc-300 hover:bg-zinc-800 border-t border-zinc-900"
            >
              {entry.result}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 9: Run HistoryPanel tests**

```bash
npx vitest run src/components/calculator/HistoryPanel.test.tsx 2>&1 | tail -3
```

- [ ] **Step 10: Commit**

```bash
git add src/components/
git commit -m "feat: Display, ButtonGrid, HistoryPanel components with TDD"
```

---

## Task 10: useCalculator hook (integration TDD)

**Files:**
- Create: `src/hooks/useCalculator.ts`, `src/hooks/useCalculator.test.ts`
- Create: `src/hooks/useKeyboard.ts`

- [ ] **Step 1: Write `src/hooks/useCalculator.test.ts`**

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useCalculator } from './useCalculator'

// Mock localStorage
const store: Record<string, string> = {}
Object.defineProperty(globalThis, 'localStorage', {
  value: {
    getItem: (k: string) => store[k] ?? null,
    setItem: (k: string, v: string) => { store[k] = v },
    removeItem: (k: string) => { delete store[k] },
  },
  writable: true,
})

beforeEach(() => Object.keys(store).forEach(k => delete store[k]))

describe('useCalculator', () => {
  it('initial state: empty expression, DEG mode', () => {
    const { result } = renderHook(() => useCalculator())
    expect(result.current.state.expression).toEqual([])
    expect(result.current.state.angleMode).toBe('DEG')
    expect(result.current.state.shiftActive).toBe(false)
  })

  it('pressing digit inserts number', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('3'))
    expect(result.current.state.expression[0]).toMatchObject({ type: 'number', value: '3' })
  })

  it('pressing digits builds number', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => { result.current.handleButton('4'); result.current.handleButton('2') })
    expect((result.current.state.expression[0] as any).value).toBe('42')
  })

  it('pressing = evaluates and adds to history', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => {
      result.current.handleButton('2')
      result.current.handleButton('plus')
      result.current.handleButton('3')
      result.current.handleButton('=')
    })
    expect(result.current.state.result).toBe('5')
    expect(result.current.state.history).toHaveLength(1)
    expect(result.current.state.history[0].result).toBe('5')
  })

  it('SHIFT toggle activates/deactivates', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('SHIFT'))
    expect(result.current.state.shiftActive).toBe(true)
    act(() => result.current.handleButton('SHIFT'))
    expect(result.current.state.shiftActive).toBe(false)
  })

  it('DEG_RAD toggles angle mode', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('DEG_RAD'))
    expect(result.current.state.angleMode).toBe('RAD')
    act(() => result.current.handleButton('DEG_RAD'))
    expect(result.current.state.angleMode).toBe('DEG')
  })

  it('fraction button positions cursor in numerator', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => result.current.handleButton('fraction'))
    expect(result.current.state.cursor.path[0].slot).toBe('numerator')
  })

  it('DEL removes last node', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => { result.current.handleButton('5'); result.current.handleButton('DEL') })
    expect(result.current.state.expression).toEqual([])
  })

  it('restoreExpression loads history entry', () => {
    const { result } = renderHook(() => useCalculator())
    act(() => {
      result.current.handleButton('9')
      result.current.handleButton('=')
    })
    const entry = result.current.state.history[0]
    act(() => result.current.handleRestore(entry))
    expect(result.current.state.result).toBe('9')
  })
})
```

- [ ] **Step 2: Run — expect FAIL**

```bash
npx vitest run src/hooks/useCalculator.test.ts 2>&1 | tail -3
```

- [ ] **Step 3: Write `src/hooks/useCalculator.ts`**

```typescript
import { useState, useCallback } from 'react'
import { ASTNode, Cursor, HistoryEntry, CalculatorState, INITIAL_CURSOR, FunctionName } from '@/lib/ast/types'
import {
  insertDigit, insertDecimalPoint, insertOperator, insertFraction,
  insertExponent, insertRadical, insertSquare, insertFunction,
  insertConstant, insertFactorial, insertNcr, insertNpr, insertParen, deleteCurrent, clearAll,
} from '@/lib/ast/builder'
import { moveCursorLeft, moveCursorRight, moveCursorUp, moveCursorDown } from '@/lib/ast/cursor'
import { evaluate } from '@/lib/evaluator'
import { loadHistory, addEntry } from '@/lib/history'

type State = CalculatorState & { lastResult: string | null }

const INIT: State = {
  expression: [], cursor: INITIAL_CURSOR, result: null,
  angleMode: 'DEG', shiftActive: false, hypActive: false,
  history: loadHistory(), lastResult: null,
}

export function useCalculator() {
  const [state, setState] = useState<State>(INIT)

  const handleButton = useCallback((id: string) => {
    setState(prev => {
      const { expression: e, cursor: c, angleMode, lastResult, history, shiftActive, hypActive } = prev

      if (id === 'SHIFT')   return { ...prev, shiftActive: !shiftActive, hypActive: false }
      if (id === 'HYP')     return { ...prev, hypActive: !hypActive, shiftActive: false }
      if (id === 'DEG_RAD') return { ...prev, angleMode: angleMode === 'DEG' ? 'RAD' : 'DEG', shiftActive: false }

      const deshift = { shiftActive: false, hypActive: false }
      type Pair = [ASTNode[], Cursor]

      let pair: Pair
      switch (id) {
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
          pair = insertDigit(e, c, id); break
        case '.':       pair = insertDecimalPoint(e, c); break
        case 'plus':    pair = insertOperator(e, c, '+'); break
        case 'minus':   pair = insertOperator(e, c, '-'); break
        case 'multiply': pair = insertOperator(e, c, '×'); break
        case 'divide':  pair = insertOperator(e, c, '÷'); break
        case 'fraction': pair = insertFraction(e, c); break
        case 'exponent': pair = insertExponent(e, c); break
        case 'sqrt':    pair = insertRadical(e, c, false); break
        case 'cbrt':    pair = insertRadical(e, c, true); break
        case 'square':  pair = insertSquare(e, c); break
        case 'factorial': pair = insertFactorial(e, c); break
        case 'ncr':     pair = insertNcr(e, c); break
        case 'npr':     pair = insertNpr(e, c); break
        case 'paren_open':  pair = insertParen(e, c, 'open'); break
        case 'paren_close': pair = insertParen(e, c, 'close'); break
        case 'pi':      pair = insertConstant(e, c, 'pi'); break
        case 'Ans':     pair = insertConstant(e, c, 'Ans'); break
        case 'sin': case 'cos': case 'tan':
        case 'asin': case 'acos': case 'atan':
        case 'sinh': case 'cosh': case 'tanh':
        case 'asinh': case 'acosh': case 'atanh':
        case 'log': case 'ln': case 'pow10': case 'exp':
          pair = insertFunction(e, c, id as FunctionName); break
        case 'DEL':   pair = deleteCurrent(e, c); break
        case 'CLEAR': pair = clearAll(); break
        case 'LEFT':  return { ...prev, cursor: moveCursorLeft(e, c) }
        case 'RIGHT': return { ...prev, cursor: moveCursorRight(e, c) }
        case 'UP':    return { ...prev, cursor: moveCursorUp(e, c) }
        case 'DOWN':  return { ...prev, cursor: moveCursorDown(e, c) }
        case '=': {
          const evalResult = evaluate(e, angleMode, lastResult)
          if (!evalResult) return { ...prev, ...deshift }
          const newHistory = addEntry(history, e, evalResult.value)
          return { ...prev, ...deshift, history: newHistory, result: evalResult.value, lastResult: evalResult.value }
        }
        default: return prev
      }

      const [newExpr, newCursor] = pair
      const evalResult = evaluate(newExpr, angleMode, lastResult)
      return { ...prev, ...deshift, expression: newExpr, cursor: newCursor, result: evalResult?.value ?? null }
    })
  }, [])

  const handleRestore = useCallback((entry: HistoryEntry) => {
    setState(prev => ({
      ...prev,
      expression: entry.expression,
      cursor: { path: [], insertAt: entry.expression.length },
      result: entry.result,
      shiftActive: false,
      hypActive: false,
    }))
  }, [])

  return { state, handleButton, handleRestore }
}
```

- [ ] **Step 4: Run — expect all pass**

```bash
npx vitest run src/hooks/useCalculator.test.ts 2>&1 | tail -3
```

- [ ] **Step 5: Write `src/hooks/useKeyboard.ts`** (no separate test — side-effect only)

```typescript
import { useEffect } from 'react'

const KEY_MAP: Record<string, string> = {
  '0':'0','1':'1','2':'2','3':'3','4':'4','5':'5','6':'6','7':'7','8':'8','9':'9',
  '.':'.', '+':'plus', '-':'minus', '*':'multiply', '/':'divide',
  'Enter':'=', '=':'=',
  'Backspace':'DEL', 'Delete':'CLEAR',
  'ArrowLeft':'LEFT', 'ArrowRight':'RIGHT', 'ArrowUp':'UP', 'ArrowDown':'DOWN',
  'Escape':'CLEAR',
}

export function useKeyboard(onButton: (id: string) => void) {
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      const id = KEY_MAP[e.key]
      if (id) { e.preventDefault(); onButton(id) }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onButton])
}
```

- [ ] **Step 6: Commit**

```bash
git add src/hooks/
git commit -m "feat: useCalculator hook and keyboard integration"
```

---

## Task 11: Calculator container + App wiring

**Files:**
- Create: `src/components/calculator/Calculator.tsx`
- Create: `src/App.tsx`, `src/main.tsx`

- [ ] **Step 1: Write `src/components/calculator/Calculator.tsx`**

```tsx
import { useCalculator } from '@/hooks/useCalculator'
import { useKeyboard } from '@/hooks/useKeyboard'
import { Display } from './Display'
import { ButtonGrid } from './ButtonGrid'
import { HistoryPanel } from './HistoryPanel'

export function Calculator() {
  const { state, handleButton, handleRestore } = useCalculator()
  useKeyboard(handleButton)

  return (
    <div className="flex flex-col h-dvh max-w-sm mx-auto bg-black overflow-hidden">
      {/* Status bar */}
      <div className="flex items-center justify-between px-3 py-1 bg-zinc-950 text-zinc-500 text-xs flex-shrink-0">
        <span className={state.angleMode === 'DEG' ? 'text-zinc-300' : 'text-blue-400'}>
          {state.angleMode}
        </span>
        <span className="flex gap-2">
          {state.hypActive   && <span className="text-blue-400">HYP</span>}
          {state.shiftActive && <span className="text-orange-400">SHIFT</span>}
        </span>
      </div>

      {/* Display */}
      <Display expression={state.expression} cursor={state.cursor} result={state.result} />

      {/* History */}
      <HistoryPanel history={state.history} onRestore={handleRestore} />

      {/* Button grid — takes remaining space, scrolls if needed */}
      <div className="flex-1 overflow-hidden flex flex-col justify-end">
        <ButtonGrid
          angleMode={state.angleMode}
          shiftActive={state.shiftActive}
          hypActive={state.hypActive}
          onButton={handleButton}
        />
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Write `src/App.tsx`**

```tsx
import { Calculator } from './components/calculator/Calculator'

export default function App() {
  return <Calculator />
}
```

- [ ] **Step 3: Write `src/main.tsx`**

```tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
```

- [ ] **Step 4: Run all tests**

```bash
npx vitest run 2>&1 | tail -10
```
Expected: all test files pass.

- [ ] **Step 5: Commit**

```bash
git add src/App.tsx src/main.tsx src/components/calculator/Calculator.tsx
git commit -m "feat: wire Calculator container, App, and main entry point"
```

---

## Task 12: Build verification + final polish

- [ ] **Step 1: TypeScript check**

```bash
npx tsc --noEmit 2>&1
```
Expected: no errors. Fix any type errors before proceeding.

- [ ] **Step 2: Production build**

```bash
npm run build 2>&1 | tail -15
```
Expected: `dist/` created, no errors. Service worker and manifest generated.

- [ ] **Step 3: Preview PWA locally**

```bash
npm run preview
```
Open `http://localhost:4173` in browser. Verify:
- Calculator renders on mobile viewport (375px)
- Typing digits works
- `a/b` button creates stacked fraction
- Result line updates live
- `=` adds to history
- History panel expands/collapses
- DEG/RAD toggle works
- SHIFT changes button labels

- [ ] **Step 4: Verify PWA installability**
Open DevTools → Application → Manifest. Confirm name, theme_color, icons are present.

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "feat: scientific calculator PWA — complete implementation"
```
