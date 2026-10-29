# Typography Redesign — Calculator App

**Date:** 2026-06-13
**Scope:** Flutter calculator app main screen only (not landing page)
**Approach:** Bold Hierarchy — size up all elements, add weight to establish a 3-tier typographic scale

## Goals

- Make all text and icons legible at a glance on Android phones
- Establish clear hierarchy: display > button primary > button secondary
- Bold, high-contrast feel — premium calculator aesthetic

## Type Scale

| Element | File | Before | After | Weight change |
|---|---|---|---|---|
| Button primary label | `button_grid.dart` | 17 | 22 | → w700 |
| Button shift label | `button_grid.dart` | 10 | 12 | → w500 |
| Expression text | `calc_display.dart` | 32 | 40 | → w600 |
| Result text | `calc_display.dart` | 28 | 34 | → w500 |
| Settings icon | `calc_display.dart` | 16 | 20 | — |
| Status badges (HYP/STO) | `calc_display.dart` | 11 | 14 | — |
| SHIFT toggle '≡' active | `calc_display.dart` | 22 | 28 | bold (unchanged) |
| SHIFT toggle '≡' inactive | `calc_display.dart` | 22 | 28 | → w500 |
| Copy button '◁' | `calc_display.dart` | 17 | 20 | — |
| DEG/RAD label | `calc_display.dart` | 12 | 15 | w700 (unchanged) |

## Section Details

### Button Grid (`lib/widgets/button_grid.dart`)

**Primary label** — the centered `Text` in `_CalcButtonState.build`:
- `fontSize: 17` → `fontSize: 22`
- Add `fontWeight: FontWeight.w700`

**Shift label** — the `Positioned` top-left corner `Text`:
- `fontSize: 10` → `fontSize: 12`
- Add `fontWeight: FontWeight.w500`

No layout changes. The 5-column `Expanded` + `LayoutBuilder` grid absorbs the size increase correctly. Long shift labels (`Paste Code`, `sin⁻¹(`, `a↔cd/e`) fit at 12px — they already fit at 10px.

### Display Area (`lib/widgets/calc_display.dart`)

**Expression text** — `DefaultTextStyle` wrapping `ASTRenderer`:
- `fontSize: 32` → `fontSize: 40`
- Add `fontWeight: FontWeight.w600`
- Empty-state placeholder `'0'` text also updates to 40px

**Result text** — `Text` in the result `Selector`:
- `fontSize: 28` → `fontSize: 34`
- Add `fontWeight: FontWeight.w500`
- Slightly lighter than expression to maintain hierarchy

**Settings icon** — `Icon(Icons.settings)`:
- `size: 16` → `size: 20`

**Status badges** — `Text` inside `_StatusBadge`:
- `fontSize: 11` → `fontSize: 14`

The `minHeight: 120` constraint on `CalcDisplay` is unchanged. `IntrinsicHeight` in `CalculatorScreen` handles the natural height expansion from larger text.

### Left Strip (`_DisplayStrip` in `lib/widgets/calc_display.dart`)

**SHIFT toggle '≡'**:
- `fontSize: 22` → `fontSize: 28`
- Active state: keep `FontWeight.bold`
- Inactive state: change from `FontWeight.normal` → `FontWeight.w500`

**Copy button '◁'**:
- `fontSize: 17` → `fontSize: 20`
- No weight change

**DEG/RAD label**:
- `fontSize: 12` → `fontSize: 15`
- Weight stays `FontWeight.bold`

## Constraints and Non-Changes

- No layout changes anywhere — all sizing is content-driven
- `_rows` button definitions unchanged
- `CalcTheme` color tokens unchanged
- `minHeight: 120` on display unchanged
- `IntrinsicHeight` wrapper unchanged
- No font family changes — uses Flutter default (Roboto on Android)
