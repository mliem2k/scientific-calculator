# Reference Layout Redesign

**Date:** 2026-06-09
**Reference:** Natural Scientific Calculator by Stultus Studios (Google Play)

---

## Goal

Redesign the calculator UI to match the reference app's 5-column button layout, remove the DPad, and add tap-on-display cursor positioning plus new memory/conversion features.

---

## 1. Button Grid Layout

### Structure

Switch from a 4-column to a **5-column** grid. Remove the DPad component entirely. No SHIFT button in the grid.

### Row definitions (secondary label above primary in each cell)

| Col 1 | Col 2 | Col 3 | Col 4 | Col 5 |
|---|---|---|---|---|
| `{PasteCode}` / **Sto** | `{ⁿ√}` / **xʸ** | `{sin⁻¹(}` / **sin(** | `{cos⁻¹(}` / **cos(** | `{tan⁻¹(}` / **tan(** |
| **Abs** | `{mix}` / **a/b** | `{³√}` / **√** | `{a/b↔cd/e}` / **log(** | `{Dec↔a/b}` / **ln(** |
| `{A}` / **x!** | `{B}` / **x³** | `{X}` / **x²** | **(** | `{Y}` / **)** |
| **7** | **8** | **9** | **DEL** (red) | **AC** (red) |
| **4** | **5** | **6** | `{nPr}` / **×** | `{nCr}` / **÷** |
| **1** | **2** | **3** | **+** | **−** |
| **0** | **.** | **EXP** | `{S⇔D}` / **=** | **(−)** |

### SHIFT mechanism

SHIFT moves out of the grid into the display header (the `≡` icon). Tapping it toggles SHIFT mode. Secondary labels (orange, tiny, above primary) are always shown — pressing SHIFT makes those labels fire instead.

### Styling

- DEL and AC: separate cells, red text, same background as digit buttons
- Secondary labels: 7px orange text above primary (unchanged from current)
- Function buttons: slightly darker background than digit buttons
- `+`, `−`, `×`, `÷`: operator color (sky blue currently)

---

## 2. Display

### Header area

Left side of display header (vertical stack):
- `≡` — SHIFT toggle (replaces the dedicated SHIFT grid button)
- `◁` — share icon (tapping copies result to clipboard; matches reference visual)
- `RAD`/`DEG` — angle mode indicator (dim when DEG, bright blue when RAD)

Right side:
- `sci calc` micro-label (existing)
- `HYP` / `STO` status indicators when active

### Expression + result

- Expression: right-aligned, natural display, same font/size
- Result: displayed large below expression, no `= ` prefix, tappable to copy
- Empty state: dim `0` placeholder (unchanged)

### Tap-to-position cursor

Each rendered AST node is wrapped in a click-target `<span>` that carries two data attributes:
- `data-cursor-path` — JSON-serialized `number[]` path into the AST
- `data-cursor-insert` — `insertAt` offset within that node

A single `onClick` handler on the display container reads these attributes from `e.target` (with `.closest('[data-cursor-path]')` fallback) and dispatches a `CURSOR_JUMP` action with the decoded path + insertAt.

Leaf nodes (NumberNode, ConstantNode, OperatorNode) emit one clickable span per character. Structural nodes (FractionNode, ExponentNode, RadicalNode) make their whole region clickable to a sensible entry point (numerator start for fractions, etc.).

---

## 3. New Features

### Memory slots (Sto / A B X Y)

**State additions to `useCalculator`:**
```ts
memory: { A: number | null; B: number | null; X: number | null; Y: number | null }
stoMode: boolean
// Action additions:
// { type: 'CURSOR_JUMP'; path: number[]; insertAt: number }
// dispatched by the display tap handler; reducer sets cursor = { path, insertAt }
```

**Handler logic:**
- `'Sto'` → set `stoMode = true` (shows `STO` in status strip)
- `'A'` / `'B'` / `'X'` / `'Y'`:
  - if `stoMode && result !== null`: write `parseFloat(result)` to `memory[letter]`, clear `stoMode`
  - else: insert a `ConstantNode` with that letter's stored value (or 0 if unset)
- Any other button press while `stoMode` is true: cancel stoMode

### PasteCode (SHIFT + Sto)

- `'PasteCode'` → `navigator.clipboard.readText()` → parse as serialized expression string → replace current expression. No-op on parse failure (no error shown to user).

### Fraction conversions

- `'DecFrac'` (SHIFT + ln, labeled `Dec↔a/b`): new action ID, same behaviour as existing `S_TO_D` (toggle result between decimal and fraction form). `S_TO_D` remains as its own button in row 7 (SHIFT+=). `DecFrac` is an alias wired to the same handler.
- `'MixedFrac'` (SHIFT + log, labeled `a/b↔cd/e`): converts the current top-level FractionNode between improper and mixed representation. New logic in `useCalculator`.

### Mix (SHIFT + a/b)

- `'mix'` → inserts a mixed-number structure: a whole-number NumberNode followed by a FractionNode. Maps to a new AST composition in `builder.ts`.

### x³ and ⁿ√

- `'cube'` → inserts `ExponentNode` with exponent 3. New action, trivially implemented alongside `'square'`.
- Existing `'cbrt'` renamed to display as `ⁿ√` in the SHIFT layer; the handler is unchanged.

---

## 4. Files Changed

| File | Change |
|---|---|
| `src/components/calculator/ButtonGrid.tsx` | 5-column layout, new row definitions, remove DPad import |
| `src/components/calculator/DPad.tsx` | Delete |
| `src/components/calculator/Display.tsx` | Header with ≡/◁/RAD icons; tap-to-position onClick; result without `= ` prefix |
| `src/components/calculator/Calculator.tsx` | Pass SHIFT toggle + memory state; remove DPad wiring |
| `src/hooks/useCalculator.ts` | Add memory state + stoMode; new handlers for Sto, A/B/X/Y, PasteCode, MixedFrac, mix, cube |
| `src/components/ast/ASTRenderer.tsx` | Thread `onCursorJump` callback; add data attributes |
| `src/components/ast/nodes/*.tsx` | Add click-target spans with data-cursor-path/insert per leaf |

---

## 5. Out of Scope

- Themes (AMOLED, red, etc.) — keep existing dark theme
- Favourites / sharing equations
- History panel appearance changes
- Keyboard shortcuts (useKeyboard.ts unchanged)
