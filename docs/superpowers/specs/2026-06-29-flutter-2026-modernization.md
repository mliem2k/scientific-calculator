---
name: flutter-2026-modernization
description: Migrate provider → Riverpod 2.x, remove dead code, fix deprecated Color APIs, convert switch expressions
metadata:
  type: project
---

# Flutter 2026 Modernization

**Date:** 2026-06-29  
**Scope:** All files under `lib/` (20 Dart files, 4333 lines)

## Goals

1. Replace `provider ^6.1.2` with `flutter_riverpod ^2.6.1`
2. Remove three confirmed dead-code sites
3. Fix five `Color.withAlpha()` deprecations (→ `Color.withValues(alpha:)`)
4. Convert three switch-statement functions to Dart 3 switch expressions

---

## 1. State Management Migration

### Approach

Use manual Riverpod `Notifier` (no `@riverpod` code-gen). Code-gen adds build complexity for minimal gain in a 2-controller app.

Pre-init pattern: `ProviderContainer` is created before `runApp`, settings loaded via `container.read(settingsProvider.notifier).init()`, then app wrapped in `UncontrolledProviderScope(container: container, ...)`.

### New `SettingsState` value class (`settings_controller.dart`)

```dart
class SettingsState {
  const SettingsState({
    this.themeId = CalcThemeId.amoled,
    this.defaultAngleMode = AngleMode.deg,
    this.resultPrecision = 0,
    this.showShiftLabels = true,
    this.hapticFeedback = true,
  });
  final CalcThemeId themeId;
  final AngleMode defaultAngleMode;
  final int resultPrecision;
  final bool showShiftLabels;
  final bool hapticFeedback;
  SettingsState copyWith({...}) => SettingsState(...);
}
```

### `SettingsNotifier extends Notifier<SettingsState>`

- `build()` returns `const SettingsState()` (defaults; `init()` overrides from prefs)
- `init()` loads SharedPreferences and sets `state`
- Setters update `state` via `copyWith` and persist to prefs
- Provider: `final settingsProvider = NotifierProvider<SettingsNotifier, SettingsState>(SettingsNotifier.new)`

### `CalculatorNotifier extends Notifier<CalculatorState>`

- `CalculatorState` already exists as a value class with `copyWith` — no structural change needed
- `build()` calls `_initHistory()` and returns `CalculatorState.initial()`
- Private `_state` field removed; all reads become `state`, writes become `state = ...`
- `notifyListeners()` calls removed
- Provider: `final calculatorProvider = NotifierProvider<CalculatorNotifier, CalculatorState>(CalculatorNotifier.new)`

### Widget migration

All widgets that previously consumed `provider` are updated to use `flutter_riverpod`. `Selector<T, V>` → `ref.watch(provider.select(...))`. `context.read<T>()` → `ref.read(provider.notifier)`.

| File | Change |
|---|---|
| `main.dart` | `CalcApp` → `ConsumerWidget`; wrap with `UncontrolledProviderScope` |
| `screens/calculator_screen.dart` | `StatefulWidget` → `ConsumerStatefulWidget` |
| `screens/settings_screen.dart` | `StatefulWidget` → `ConsumerStatefulWidget` |
| `widgets/calc_display.dart` → `_DegRadBadge` | `StatelessWidget` → `ConsumerWidget` |
| `widgets/calc_display.dart` → `_ExpressionArea` | `StatefulWidget` → `ConsumerStatefulWidget` |
| `widgets/button_grid.dart` → `ButtonGrid` | `StatelessWidget` → `ConsumerWidget`; `ref` captured in tap closure |
| `widgets/history_panel.dart` | `StatelessWidget` → `ConsumerWidget` |

`import 'package:provider/provider.dart'` removed from all 5 files that had it.

---

## 2. Dead Code Removal

| Site | Reason |
|---|---|
| `case 'reciprocal'` in `_applyInsert` (calculator_controller.dart:417) | No button dispatches this ID; button was removed in commit `ef64e98` |
| `case 'pow10'` in `_applyInsert` (calculator_controller.dart:485) | Shift+log dispatches `MixedFrac`, not `pow10`; no path reaches this case |
| `bool _dragging` field in `_ExpressionAreaState` (calc_display.dart) | Assigned in gesture callbacks but never read |

---

## 3. Deprecated Color API Fixes

`Color.withAlpha(int n)` is deprecated in Flutter 3.27+. Replacement: `Color.withValues(alpha: n / 255)`.

| File | Call | Fix |
|---|---|---|
| `widgets/button_grid.dart:220` | `ct.eqBg.withAlpha(200)` | `.withValues(alpha: 200/255)` |
| `widgets/button_grid.dart:224` | `Colors.white.withAlpha(30)` | `.withValues(alpha: 30/255)` |
| `widgets/update_dialog.dart:162` | `ct.buttonBorder.withAlpha(60)` | `.withValues(alpha: 60/255)` |
| `widgets/update_dialog.dart:248` | `color.withAlpha(25)` | `.withValues(alpha: 25/255)` |
| `widgets/ast/ast_renderer.dart:206` | `ct.expressionText.withAlpha(80)` | `.withValues(alpha: 80/255)` |

---

## 4. Switch Expression Conversions

Three functions using imperative `switch` statements converted to Dart 3 switch expressions (exhaustive, returns a value directly):

- `_actionId(String id, bool shiftActive)` in `widgets/button_grid.dart`
- `_themeForPreview(CalcThemeId id)` in `screens/settings_screen.dart`
- `_nameForId(CalcThemeId id)` in `screens/settings_screen.dart`

---

## Files Changed

```
pubspec.yaml
lib/main.dart
lib/controllers/settings_controller.dart   ← SettingsState + SettingsNotifier
lib/controllers/calculator_controller.dart  ← CalculatorNotifier + dead code removed
lib/screens/calculator_screen.dart
lib/screens/settings_screen.dart            ← switch expressions
lib/widgets/calc_display.dart
lib/widgets/button_grid.dart                ← switch expression + withValues()
lib/widgets/history_panel.dart
lib/widgets/update_dialog.dart              ← withValues()
lib/widgets/ast/ast_renderer.dart           ← withValues()
```

## Out of Scope

- No changes to `core/` (AST types, evaluator, history) — pure logic, no UI/state deps
- No changes to `theme/` — no deprecated APIs found there
- No changes to `services/updater.dart` — standalone, no provider usage
- No widget structure or visual changes

## Success Criteria

- `flutter analyze` reports zero issues
- App launches, calculator operates, settings persist, update check works
- No `provider` imports remain in `lib/`
