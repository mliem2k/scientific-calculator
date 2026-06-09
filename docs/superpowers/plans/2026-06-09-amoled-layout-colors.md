# AMOLED Layout and Color Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make buttons fill the full screen height on mobile and apply a pure AMOLED color scheme — true black, white text, thin grid-line cell borders, single `=` accent.

**Architecture:** Two isolated file changes. ButtonGrid.tsx switches from a fixed-height CSS grid with individual button styling to nested flex rows that fill remaining screen height, with cell-border styling replacing per-button borders/backgrounds. Display.tsx gets a one-line result font size bump.

**Tech Stack:** React, TypeScript, Tailwind CSS v3, Vite, Vitest

---

## File Map

| File | Change |
|---|---|
| `src/components/calculator/ButtonGrid.tsx` | New style constants, flex-fill row structure, updated renderBtn |
| `src/components/calculator/Display.tsx` | Result span: `text-base` → `text-2xl` |

No changes to Calculator.tsx, index.css, tailwind.config.js, or any logic/hook files.

---

## Task 1: AMOLED ButtonGrid — style constants and renderBtn

**Files:**
- Modify: `src/components/calculator/ButtonGrid.tsx`

### Background

The current grid uses CSS `grid grid-cols-5 gap-[3px] p-2` with fixed button heights (`h-11`/`h-12`) and per-button `bg-zinc-*` backgrounds + `border border-zinc-*` borders. The AMOLED reference uses:
- Pure black everywhere (no visible button backgrounds)
- Thin 1px grid lines between cells (border on each cell's right+bottom edge; border on container's left+top edge)
- No border-radius
- White text for all labels; red text for DEL and AC; white background/black text for `=`

- [ ] **Step 1: Update style constants at the top of ButtonGrid.tsx**

Replace the six `const` lines (lines 3–8):

```tsx
const DIGIT = 'bg-black text-white'
const FN    = 'bg-black text-white'
const OP    = 'bg-black text-white'
const DEL   = 'bg-black text-red-500 font-semibold'
const AC    = 'bg-black text-red-500 font-semibold'
const EQ    = 'bg-white text-black font-semibold'
```

- [ ] **Step 2: Update renderBtn to use flex-fill + cell-border styling**

Replace the entire `renderBtn` function (lines 116–139):

```tsx
function renderBtn(btn: BtnDef, key: string) {
  const secondary = !shiftActive && btn.shiftLabel ? btn.shiftLabel : null
  return (
    <button
      key={key}
      onClick={() => handleClick(btn)}
      className={cn(
        'flex-1 flex flex-col items-center justify-center select-none',
        'border-r border-b border-zinc-800',
        'active:bg-zinc-900 transition-colors duration-75',
        btn.cls,
      )}
    >
      {secondary && (
        <span className="text-[8px] text-zinc-500 leading-none mb-0.5 font-normal">
          {secondary}
        </span>
      )}
      <span className="text-sm font-medium leading-none">{displayLabel(btn)}</span>
    </button>
  )
}
```

Key changes from current:
- Remove `rounded`, fixed `h-11`/`h-12`, `active:scale-95 active:brightness-75`
- Add `flex-1` (button fills its row), `border-r border-b border-zinc-800` (cell border)
- `active:bg-zinc-900` replaces `active:brightness-75` (brightness on black is invisible)
- Secondary label: `text-zinc-500` replaces `text-orange-400` (quieter on pure black)
- Primary label extracted to its own `<span>` with `text-sm font-medium` (was inline, same result)

- [ ] **Step 3: Run existing tests to confirm no regressions**

```bash
cd /Users/mliem/Documents/GitHub/scientific-calculator && npm test -- ButtonGrid
```

Expected output: 6 tests pass. These tests check click behavior and label rendering — no snapshot/CSS assertions, so they are unaffected by styling changes.

---

## Task 2: AMOLED ButtonGrid — flex-fill grid structure

**Files:**
- Modify: `src/components/calculator/ButtonGrid.tsx`

### Background

Currently the return value is a single `<div className="grid grid-cols-5 ...">` that renders `ROWS.flat()` — all 35 buttons as a flat list in a CSS grid. The AMOLED design needs buttons to fill the remaining screen height proportionally, which requires switching to nested flex rows: one `<div className="flex-1 flex">` per row, each containing 5 `flex-1` buttons.

The parent container in Calculator.tsx is already `flex-1 flex flex-col`, so a `flex-1` ButtonGrid will expand to fill all space below the display.

- [ ] **Step 1: Replace the return statement in ButtonGrid**

Replace the entire `return` block (lines 141–145):

```tsx
return (
  <div className="flex-1 flex flex-col border-l border-t border-zinc-800 bg-black">
    {ROWS.map((row, rowIdx) => (
      <div key={rowIdx} className="flex-1 flex">
        {row.map((btn, i) => renderBtn(btn, btn.id + i))}
      </div>
    ))}
  </div>
)
```

Changes from current:
- `grid grid-cols-5 gap-[3px] p-2 bg-zinc-950 flex-shrink-0` → `flex-1 flex flex-col border-l border-t border-zinc-800 bg-black`
- `ROWS.flat().map(...)` → `ROWS.map(row => ...)` with inner `row.map(...)` — preserves row grouping for flex rows
- Container `border-l border-t` + button `border-r border-b` = complete grid-line table

- [ ] **Step 2: Run full test suite**

```bash
cd /Users/mliem/Documents/GitHub/scientific-calculator && npm test
```

Expected: all tests pass. If any ButtonGrid test fails, it indicates a structural issue with the row mapping — check that `renderBtn` still receives the correct `BtnDef` objects.

- [ ] **Step 3: Commit**

```bash
git add src/components/calculator/ButtonGrid.tsx
git commit -m "feat: AMOLED button grid — flex-fill rows, cell borders, black palette"
```

---

## Task 3: Larger result display

**Files:**
- Modify: `src/components/calculator/Display.tsx`

### Background

The result is currently rendered as `text-base` (16px). The AMOLED reference shows the result number in a much larger font, dominating the display area below the expression. Bumping to `text-2xl` (24px) matches this.

- [ ] **Step 1: Update result span font size in Display.tsx**

Find line 93 (the result `<span>`):

```tsx
// Current:
<span className={copied ? 'font-display text-base text-green-400' : 'font-display text-base text-zinc-400'}>
```

Replace with:

```tsx
<span className={copied ? 'font-display text-2xl text-green-400' : 'font-display text-2xl text-zinc-400'}>
```

- [ ] **Step 2: Run full test suite**

```bash
cd /Users/mliem/Documents/GitHub/scientific-calculator && npm test
```

Expected: all tests pass. The Display tests check text content and event handlers — not font size — so they are unaffected.

- [ ] **Step 3: Commit**

```bash
git add src/components/calculator/Display.tsx
git commit -m "feat: enlarge result display to text-2xl"
```

---

## Spec Coverage Self-Review

| Spec requirement | Task covering it |
|---|---|
| All buttons `bg-black` | Task 1, Step 1 (style constants) |
| Cell-border grid lines (no gap, no padding) | Task 2, Step 1 (return statement) |
| `flex-1` fill remaining height | Task 2, Step 1 |
| `rounded-none` (no border-radius) | Task 1, Step 2 (`rounded` removed from renderBtn) |
| Secondary labels `text-zinc-500` | Task 1, Step 2 |
| `active:bg-zinc-900` tap feedback | Task 1, Step 2 |
| DEL/AC `text-red-500` | Task 1, Step 1 (DEL/AC constants) |
| `=` is `bg-white text-black` | Task 1, Step 1 (EQ constant) |
| Result `text-2xl` | Task 3, Step 1 |
