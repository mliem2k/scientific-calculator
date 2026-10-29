# Best-Practices Layout Redesign

**Date:** 2026-06-16
**Scope:** Flutter calculator app — button grid, display area, screen layout
**Approach:** Casio-exact with double-wide 0 (Approach C)

---

## Goals

- Bring button layout in line with scientific calculator conventions (Casio fx-991, iOS, Google Calculator)
- Surface all controller-wired buttons that currently have no UI entry point (SHIFT in grid, HYP, π, e, Ans)
- Fix `=` to the bottom-right corner
- Give expression and result more horizontal room by removing the left display strip
- Add a history entry point

---

## 1. Button Grid

### Structure

8 rows × 5 columns (was 7 × 5). New control row at top; bottom row uses a double-wide `0`.

```
Row 1 (Control):  SHIFT   | HYP    | π      | e      | Ans
Row 2 (Trig):     Sto     | xʸ     | sin(   | cos(   | tan(
Row 3 (Fns):      Abs     | a/b    | √      | log(   | ln(
Row 4 (Algebra):  x!      | x³     | x²     | (      | )
Row 5 (Numpad):   7       | 8      | 9      | DEL    | AC
Row 6 (Numpad):   4       | 5      | 6      | ×      | ÷
Row 7 (Numpad):   1       | 2      | 3      | +      | −
Row 8 (Bottom):   0 (2×)  | .      | (−)    | =
```

### `_BtnDef` change

Add `flex` field (default 1). Used in the `Expanded` widget to give `0` double width:

```dart
class _BtnDef {
  final String id;
  final String label;
  final String? shiftLabel;
  final _BtnRole role;
  final int flex;
  const _BtnDef(this.id, this.label, {this.shiftLabel, required this.role, this.flex = 1});
}
```

Row builder becomes:
```dart
Expanded(flex: btn.flex, child: _CalcButton(...))
```

### Row 1 button definitions

```dart
_BtnDef('SHIFT', 'SHIFT',                          role: _BtnRole.fn),
_BtnDef('HYP',   'HYP',                            role: _BtnRole.fn),
_BtnDef('pi',    'π',                              role: _BtnRole.fn),
_BtnDef('e_const','e',    shiftLabel: 'eˣ',        role: _BtnRole.fn),
_BtnDef('Ans',   'Ans',                            role: _BtnRole.fn),
```

### Row 8 button definitions

```dart
_BtnDef('0',      '0',   role: _BtnRole.digit, flex: 2),
_BtnDef('.',      '.',   shiftLabel: 'EXP',    role: _BtnRole.digit),
_BtnDef('negate', '(−)',                        role: _BtnRole.digit),
_BtnDef('=',      '=',   shiftLabel: 'S⇔D',   role: _BtnRole.eq),
```

`EXP` is removed from its previous row 7 position (it no longer exists as a standalone button).

### Shift label additions

| Button id | Primary | Shift label | Shift action id |
|-----------|---------|-------------|-----------------|
| `e_const` | `e`     | `eˣ`       | `exp`           |
| `.`       | `.`     | `EXP`       | `EXP`           |

All other shift pairings (rows 2–7) are unchanged.

### `_actionId` additions

```dart
case 'e_const': return 'exp';   // SHIFT+e → e^x
case '.':       return 'EXP';   // SHIFT+. → EXP
```

### Roles

SHIFT and HYP use `_BtnRole.fn` (same color as function buttons). No new role needed. SHIFT and HYP are already handled in `CalculatorController._reduce` — no controller changes required.

### Button height

With 8 rows instead of 7, each row is ~10px shorter on a typical 800px phone (~75px vs ~91px). This is within comfortable tap target range (Google HIG minimum is 48dp).

---

## 2. Display Area

### Remove left strip

`_DisplayStrip` is deleted entirely. Its three responsibilities are redistributed:
- SHIFT toggle → now in the button grid (Row 1)
- Copy result (`◁`) → redundant; result text is already tap-to-copy
- DEG/RAD toggle → moves to display header as a tappable badge

### New layout

`CalcDisplay.build` changes from a `Row` (strip + content) to a `Column` (header + spacer + expression + result):

```
┌─────────────────────────────────────────────────────┐
│ [DEG]  [SHIFT]  [HYP]  [STO]   ...  [🕐]  [⚙]    │  ← header row
│                                                     │
│                  3sin(45) + √(2)                    │  ← expression (full width)
│                           1.914...                  │  ← result (full width)
└─────────────────────────────────────────────────────┘
```

### Header row (left to right)

1. **DEG/RAD badge** — always visible. Tapping fires `controller.handleButton('DEG_RAD')`. Blue (`Color(0xFF4FC3F7)`) when RAD, muted (`ct.secondaryLabel`) when DEG. Implemented as `_DegRadBadge` inline widget using same `_StatusBadge`-style container.
2. **SHIFT badge** — appears only when `shiftActive`. Reuses `_StatusBadge` with label `'SHIFT'`. Same orange as `ct.shiftActiveColor`.
3. **HYP / STO badges** — unchanged, appear when those modes are active.
4. `Spacer()`
5. **History icon** (`Icons.history`, size 20, color `ct.secondaryLabel`) — tapping pushes a `MaterialPageRoute` wrapping `HistoryPanel` in a `Scaffold`.
6. **Settings icon** — unchanged, same `Icons.settings`, size 20.

### Expression and result

`_ExpressionArea` and the result `Selector` are unchanged. They now span full display width (no `Expanded` needed since they're in a `Column`).

### Padding

`CalcDisplay` padding stays `EdgeInsets.all(12)`. The `SizedBox(width: 8)` spacer between old strip and content is removed.

---

## 3. Screen Layout

### Display height

In `calculator_screen.dart`, one line changes:

```dart
// Before
final displayH = (constraints.maxHeight * 0.20).clamp(160.0, 210.0);

// After
final displayH = (constraints.maxHeight * 0.25).clamp(180.0, 240.0);
```

This gives the expression and result more room, especially on small phones where 160px with 40px expression + 34px result was tight.

---

## 4. Files Changed

| File | Change |
|------|--------|
| `lib/widgets/button_grid.dart` | Add `flex` to `_BtnDef`; add Row 1; update Row 8; update `_actionId`; update `Expanded` builder |
| `lib/widgets/calc_display.dart` | Delete `_DisplayStrip`; rewrite `CalcDisplay.build` to `Column`; add `_DegRadBadge`; add history icon |
| `lib/screens/calculator_screen.dart` | Change display height formula (1 line) |

---

## 5. Out of Scope

- Themes — all 4 themes unchanged (no new color tokens needed)
- Controller — no changes; all button IDs already handled
- AST engine, evaluator, history persistence — unchanged
- Settings screen — unchanged
- `history_panel.dart` — unchanged (used as-is)
- Keyboard shortcuts or accessibility labels
- Animation on button press (existing press-state color change retained)
- Long-press drag cursor (unchanged)
