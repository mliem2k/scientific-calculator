# Scientific Calculator PWA — Design Spec

**Date:** 2026-06-08  
**Status:** Approved

---

## Overview

A mobile-first scientific calculator PWA that replicates the natural textbook display experience of the Casio FX-991, built with React, shadcn/ui, and Vercel dark theming. Expressions render as stacked fractions, superscript exponents, and radical symbols — not flat strings.

---

## Tech Stack

| Concern | Choice |
|---|---|
| Build | Vite + TypeScript |
| UI framework | React 18 |
| Components | shadcn/ui + Tailwind CSS |
| Theme | Vercel dark (black bg, white text, zinc grays) |
| Math evaluation | math.js |
| PWA | vite-plugin-pwa (service worker + manifest) |
| Testing | Vitest + React Testing Library |

---

## Features

**SHIFT key:** Latching toggle — first press activates (highlights button), second press deactivates. Changes button labels: sin↔sin⁻¹, cos↔cos⁻¹, tan↔tan⁻¹, sinh↔sinh⁻¹, log↔10^x, ln↔e^x, √↔x². Deactivates automatically after one function is pressed.

**Scientific functions (extended scope):**
- Arithmetic: +, −, ×, ÷, parentheses
- Powers: x², xʸ, √, ∛, nth root
- Trig: sin, cos, tan + inverses (sin⁻¹, cos⁻¹, tan⁻¹) via SHIFT
- Hyperbolic trig: sinh, cosh, tanh + inverses via SHIFT
- Logarithms: log (base 10), ln (natural), 10^x, e^x via SHIFT
- Combinatorics: nCr, nPr, x! (factorial)
- Constants: π, e, Ans
- Angle mode: DEG / RAD toggle

**Fraction input:**
- Dedicated `a/b` button creates a stacked fraction template
- Cursor starts in numerator; right arrow at end of numerator moves to denominator
- Supports nested fractions (fraction inside fraction's numerator/denominator)

**Natural display (FX-991 style):**
- Fractions: numerator above a horizontal bar, denominator below
- Exponents: superscript with vertical offset
- Radicals: √ symbol over radicand with overline
- Cursor: blinking `|` positioned at current tree location
- Result line: right-aligned below expression, muted color

**History:**
- Collapsible strip above button grid
- Scrollable list of past calculations (expression + result)
- Tap any row to restore that expression and result into the display
- Persisted to localStorage

**PWA:**
- Installable (web manifest: name "Scientific Calc", theme #000)
- Fully offline (service worker pre-caches all assets)

---

## Expression AST

The expression is stored as a tree of typed nodes. Buttons mutate the tree; the cursor path tracks position within it.

### Node types

```ts
type NumberNode    = { type: 'number';    value: string }
type OperatorNode  = { type: 'operator';  op: '+' | '-' | '×' | '÷' }
type ConstantNode  = { type: 'constant';  name: 'pi' | 'e' | 'Ans' }
type FractionNode  = { type: 'fraction';  numerator: ASTNode[]; denominator: ASTNode[] }
type ExponentNode  = { type: 'exponent';  base: ASTNode[];      exponent: ASTNode[] }
type RadicalNode   = { type: 'radical';   degree: ASTNode[];    radicand: ASTNode[] }
type FunctionNode  = { type: 'function';  name: string;         argument: ASTNode[] }
type FactorialNode = { type: 'factorial'; operand: ASTNode[] }
type NcrNode       = { type: 'ncr';       n: ASTNode[];         r: ASTNode[] }
type NprNode       = { type: 'npr';       n: ASTNode[];         r: ASTNode[] }
type ParenNode     = { type: 'paren';     children: ASTNode[] }

type ASTNode = NumberNode | OperatorNode | ConstantNode | FractionNode
             | ExponentNode | RadicalNode | FunctionNode | FactorialNode
             | NcrNode | NprNode | ParenNode
```

### Cursor path

```ts
type CursorSegment = { index: number; slot?: string }
type CursorPath    = CursorSegment[]
```

A path like `[{ index: 2 }, { slot: 'numerator', index: 1 }]` means: root node at index 2, inside its numerator, at index 1.

### Cursor navigation rules

- **Left / Right** — move between siblings at the current level
- **Up** — from denominator → numerator in `FractionNode`; from exponent → base in `ExponentNode`
- **Down** — from numerator → denominator in `FractionNode`; from base → exponent in `ExponentNode`
- **Entering a template** — cursor lands in the first slot (numerator, base, radicand)
- **Right at end of first slot** — cursor jumps to the second slot (denominator, exponent)
- **DEL** — removes node at cursor; collapses empty templates into a single `NumberNode("")`

---

## Data Flow

```
Button press
    ↓
handleButton(id)  [useCalculator hook]
    ↓
builder.ts  →  mutates AST + advances CursorPath
    ↓
ASTRenderer  →  re-renders expression tree
    ↓
serializer.ts  →  AST → math.js expression string
    ↓
evaluator.ts  →  math.js eval → result string (live, after every change)
    ↓
On [=]  →  push HistoryEntry, persist to localStorage
```

---

## File Structure

```
src/
  components/
    calculator/
      Calculator.tsx       # root layout, wires useCalculator to children
      Display.tsx          # expression area + result line
      ButtonGrid.tsx       # all buttons; SHIFT changes labels
      HistoryPanel.tsx     # collapsible history list
    ast/
      ASTRenderer.tsx      # recursive tree → JSX
      Cursor.tsx           # blinking cursor component
      nodes/
        FractionNode.tsx
        ExponentNode.tsx
        RadicalNode.tsx
        FunctionNode.tsx
        NumberNode.tsx
        OperatorNode.tsx
        ConstantNode.tsx
    ui/                    # shadcn components
  lib/
    ast/
      types.ts             # all node + cursor type definitions
      builder.ts           # insertDigit, insertFraction, insertExponent,
                           # insertFunction, deleteCurrent, moveCursor
      serializer.ts        # AST → math.js expression string
      cursor.ts            # cursor navigation logic
    evaluator.ts           # math.js wrapper; angle mode; result formatting
    history.ts             # HistoryEntry CRUD + localStorage persistence
  hooks/
    useCalculator.ts       # all calculator state + dispatch
    useKeyboard.ts         # physical keyboard → button id mapping
  App.tsx
  main.tsx
public/
  manifest.json
  icons/
```

---

## Layout (Mobile-first, 375px base)

```
┌─────────────────────────────┐
│  DEG/RAD           SHIFT    │  status bar (zinc-900)
├─────────────────────────────┤
│                             │
│   1                         │
│  ─── + sin(π)               │  expression (natural display, scrollable)
│   2                         │
│              = 1.5707...    │  result (right-aligned, muted)
├─────────────────────────────┤
│  ▾ History  (3 items)       │  collapsible
│   1/2 + sin(π) = 1.5707     │
│   3² = 9                    │
├─────────────────────────────┤
│ [SHIFT] [DEG/RAD] [◄] [►]  │   ← cursor arrow keys
│ [log]   [ln]    [hyp]  [x!] │
│ [sin]   [cos]   [tan]  [^]  │
│ [√]     [∛]     [a/b]  [x²] │
│ [nCr]   [nPr]   [(]   [)]   │
│  [7]     [8]    [9]   [DEL] │
│  [4]     [5]    [6]    [×]  │
│  [1]     [2]    [3]    [÷]  │
│  [0]     [.]   [Ans]   [+]  │
│          [=]           [−]  │
└─────────────────────────────┘
```

---

## State Shape

```ts
interface CalculatorState {
  expression:  ASTNode[]
  cursor:      CursorPath
  result:      string | null
  angleMode:   'DEG' | 'RAD'
  shiftActive: boolean
  history:     HistoryEntry[]
}

interface HistoryEntry {
  id:         string
  expression: ASTNode[]
  result:     string
  timestamp:  number
}
```

---

## TDD Order

Tests are written before implementation for all logic files. Components get tests before rendering details are filled in.

```
1. ast/types.ts          — type definitions only, no tests
2. ast/builder.ts        — insertDigit, insertFraction, insertExponent,
                           insertFunction, deleteCurrent, moveCursor
3. ast/serializer.ts     — AST → math.js string for each node type
4. ast/cursor.ts         — all navigation cases (into/out of fraction, nested)
5. evaluator.ts          — known expressions → expected results,
                           angle mode switching, Ans substitution
6. history.ts            — add, retrieve, persist, restore from localStorage
7. ASTRenderer.tsx       — renders fraction as stacked, exponent as superscript
8. Display.tsx           — shows expression + result line
9. ButtonGrid.tsx        — SHIFT toggle changes labels, button fires correct id
10. useCalculator.ts     — integration: full button sequences → expected state
```

**Coverage target:** 100% on all `lib/` files. Components tested at behaviour level (no snapshots).

---

## PWA Config

- `vite-plugin-pwa` with `generateSW` strategy
- Pre-cache: all JS/CSS/assets, manifest, icons
- Web manifest:
  - `name`: "Scientific Calc"
  - `short_name`: "Calc"
  - `theme_color`: "#000000"
  - `background_color`: "#000000"
  - `display`: "standalone"
  - `orientation`: "portrait"
