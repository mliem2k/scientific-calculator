# AMOLED Layout and Color Redesign

**Date:** 2026-06-09
**Reference:** Natural Scientific Calculator by Stultus Studios — AMOLED theme (screenshot-3.webp)

---

## Goal

Make buttons larger on mobile and apply a pure AMOLED color scheme: true black backgrounds, white text, thin grid-line separators instead of individually styled buttons, with `=` as the single accent.

---

## 1. Button Grid

### Layout

Replace the current `grid grid-cols-5 gap-[3px] p-2` approach with a flex-fill structure:

- Grid wrapper: `flex-1 flex flex-col border-l border-t border-zinc-800 bg-black` (no gap, no padding)
- Each row: `flex-1 flex` (7 rows, each filling equal vertical space)
- Each button: `flex-1 border-r border-b border-zinc-800` (cell border on right and bottom completes the grid lines)

Buttons fill all vertical space remaining after the display. On any phone height they grow proportionally — no fixed `h-*` values.

### Colors

Remove all per-category background classes (`bg-zinc-900`, `bg-zinc-950`). All buttons are `bg-black`.

| Category | Background | Text |
|---|---|---|
| Digits | `bg-black` | `text-white` |
| Functions | `bg-black` | `text-white` |
| Operators (+, −, ×, ÷) | `bg-black` | `text-white` |
| DEL | `bg-black` | `text-red-500` |
| AC | `bg-black` | `text-red-500` |
| = | `bg-white` | `text-black` |

No border-radius on buttons (`rounded-none`).

### Secondary shift labels

Keep existing secondary label layout (tiny text above primary). Change color from `text-orange-400` to `text-zinc-500` — quieter on pure black, still legible.

### Active state

Change `active:brightness-75` (invisible on black) to `active:bg-zinc-900` — provides visible tap feedback without breaking the AMOLED look.

---

## 2. Display

### Result size

Bump result text from `text-base` to `text-2xl` to match the reference's large number presentation.

### No other display changes

Expression size, left strip icons (≡, <, RAD/DEG), status badges (HYP, STO), and layout are unchanged.

---

## 3. Files Changed

| File | Change |
|---|---|
| `src/components/calculator/ButtonGrid.tsx` | Replace style constants; change grid to flex-fill rows; update button rendering |
| `src/components/calculator/Display.tsx` | Result text size `text-base` → `text-2xl` |

No changes to Calculator.tsx, index.css, tailwind.config, or any logic files.

---

## 4. Out of Scope

- Display background color (stays `#080c08`)
- History panel styling
- Font changes
- Any logic or calculator behavior
