# Flutter Port Design

**Date:** 2026-06-13
**Scope:** Port scientific calculator web app to Flutter (Android), add settings and themes, add GitHub Release script

---

## 1. Repository Restructure

The existing web app moves to `legacy/` at the repo root. The Flutter project is initialized at the repo root (not in a subdirectory). No files in `legacy/` are modified after the move.

```
scientific-calculator/
├── lib/                            ← Flutter source
├── android/
├── test/
├── scripts/
│   └── release.sh
├── pubspec.yaml
├── legacy/                         ← old web app (read-only after move)
│   ├── src/
│   ├── public/
│   ├── dist/
│   ├── package.json
│   ├── pnpm-lock.yaml
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── postcss.config.js
│   ├── tsconfig.json
│   └── index.html
└── docs/
```

The `reference-photos/` and `docs/` directories stay at the root unchanged.

---

## 2. Flutter App Structure

```
lib/
├── main.dart                         ← MultiProvider root, await SettingsController.init()
├── core/
│   ├── ast/
│   │   ├── types.dart                ← sealed ASTNode classes
│   │   ├── builder.dart              ← insert* functions
│   │   ├── cursor.dart               ← moveCursor* functions
│   │   └── serializer.dart           ← serialize(List<ASTNode>) → String
│   ├── evaluator.dart                ← MathEngine, evaluate(), toFraction(), toMixed(), fromMixed()
│   └── history.dart                  ← HistoryEntry, loadHistory(), addEntry()
├── controllers/
│   ├── calculator_controller.dart    ← CalculatorController extends ChangeNotifier
│   └── settings_controller.dart     ← SettingsController extends ChangeNotifier
├── screens/
│   ├── calculator_screen.dart
│   └── settings_screen.dart
├── widgets/
│   ├── button_grid.dart
│   ├── calc_display.dart
│   ├── history_panel.dart
│   └── ast/
│       ├── ast_renderer.dart
│       └── nodes/
│           ├── number_node_widget.dart
│           ├── operator_node_widget.dart
│           ├── constant_node_widget.dart
│           ├── fraction_node_widget.dart
│           ├── exponent_node_widget.dart
│           ├── radical_node_widget.dart
│           ├── function_node_widget.dart
│           ├── factorial_node_widget.dart
│           ├── ncr_node_widget.dart
│           ├── npr_node_widget.dart
│           └── paren_node_widget.dart
└── theme/
    ├── calc_theme.dart               ← ThemeExtension<CalcTheme>
    └── themes.dart                   ← amoledTheme, darkTheme, lightTheme, retroTheme
```

---

## 3. Dependencies (`pubspec.yaml`)

```yaml
dependencies:
  flutter:
    sdk: flutter
  provider: ^6.1.2
  shared_preferences: ^2.3.3

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^4.0.0
```

No external math library. No JS bridge.

---

## 4. Core Dart Port

### 4.1 AST Types (`core/ast/types.dart`)

TypeScript discriminated unions map to Dart 3 sealed classes, enabling exhaustive `switch` expressions with no default branch required.

```dart
sealed class ASTNode {}

class NumberNode    extends ASTNode { final String value; NumberNode(this.value); }
class OperatorNode  extends ASTNode { final String op;    OperatorNode(this.op); }
class ConstantNode  extends ASTNode { final String name;  ConstantNode(this.name); }
class FractionNode  extends ASTNode {
  final List<ASTNode> numerator, denominator;
  FractionNode(this.numerator, this.denominator);
}
class ExponentNode  extends ASTNode {
  final List<ASTNode> base, exponent;
  ExponentNode(this.base, this.exponent);
}
class RadicalNode   extends ASTNode {
  final List<ASTNode> degree, radicand;
  RadicalNode(this.degree, this.radicand);
}
class FunctionNode  extends ASTNode {
  final String name;
  final List<ASTNode> argument;
  FunctionNode(this.name, this.argument);
}
class FactorialNode extends ASTNode { final List<ASTNode> operand; FactorialNode(this.operand); }
class NcrNode       extends ASTNode { final List<ASTNode> n, r;    NcrNode(this.n, this.r); }
class NprNode       extends ASTNode { final List<ASTNode> n, r;    NprNode(this.n, this.r); }
class ParenNode     extends ASTNode { final List<ASTNode> children; ParenNode(this.children); }

class CursorSegment { final int nodeIndex; final String slot; CursorSegment(this.nodeIndex, this.slot); }
class Cursor { final List<CursorSegment> path; final int insertAt; Cursor(this.path, this.insertAt); }

final initialCursor = Cursor([], 0);
```

All node classes are immutable (final fields). Lists passed in are stored as-is; callers pass new lists for immutable updates.

### 4.2 Evaluator (`core/evaluator.dart`)

The evaluator walks the AST directly (no serialize → re-parse round-trip). This avoids a second parser and is exact by construction. The serializer still exists for clipboard/paste operations but is not used in evaluation.

`evaluate(List<ASTNode> nodes, AngleMode mode, String? lastResult, MemorySlots memory)` recursively evaluates each node via a `switch` on the sealed class:

- `NumberNode` → `double.parse(value)`
- `OperatorNode` → applied between left and right operands at the call site
- `ConstantNode` → `pi`, `e`, `Ans` (→ lastResult ?? 0), `A`/`B`/`X`/`Y` (→ memory slot)
- `FractionNode` → `evalNodes(numerator) / evalNodes(denominator)`
- `ExponentNode` → `pow(evalNodes(base), evalNodes(exponent))`
- `RadicalNode` → `pow(evalNodes(radicand), 1.0 / evalNodes(degree))`
- `FunctionNode` → dispatches to a `_evalFunction(String name, double arg, AngleMode)` helper
- `FactorialNode`, `NcrNode`, `NprNode` → iterative integer algorithms
- `ParenNode` → `evalNodes(children)`

`_evalFunction` wraps `dart:math` for all trig/log/exp. Angle conversion for DEG mode:
- `sin(x)` → `math.sin(x * pi / 180)`, and so on for cos/tan/sinh/cosh/tanh
- `asin(x)` → `math.asin(x) * 180 / pi`, and so on

`evalNodes(List<ASTNode>)` evaluates a flat node list respecting operator precedence: two-pass (multiplication/division first, then addition/subtraction), matching the web app's serializer + mathjs behavior.

`toFraction(String decimal)` uses Euclidean GCD to find the rational approximation within denominator <= 999. Returns null for integers and denominators > 999.

`toFraction(String decimal)` uses Euclidean GCD to find the rational approximation within denominator <= 999. Returns null for integers and denominators > 999.

`toMixed`, `fromMixed` are direct ports of the TypeScript implementations.

`formatNumber(double n)` mirrors TypeScript: integers with |n| < 1e15 return as integer string; others use 10 significant figures, stripped of trailing zeros.

### 4.3 Builder, Cursor, Serializer, History

Direct Dart ports of the corresponding TypeScript modules. Logic is identical — only syntax changes (arrow functions → Dart closures, array spread → list spread, TypeScript interfaces → Dart records/classes).

History persists to `shared_preferences` as a JSON-encoded string (same format as `localStorage` in the web app).

---

## 5. State Management

### 5.1 CalculatorController

```dart
class CalculatorController extends ChangeNotifier {
  CalculatorState _state = CalculatorState.initial();

  CalculatorState get state => _state;

  void handleButton(String id) {
    _state = _reduce(_state, id);
    notifyListeners();
  }

  void handleCursorJump(List<CursorSegment> path, int insertAt) {
    _state = _state.copyWith(cursor: Cursor(path, insertAt));
    notifyListeners();
  }

  Future<void> handlePaste() async { ... notifyListeners(); }
  void handleRestore(HistoryEntry entry) { ... notifyListeners(); }

  CalculatorState _reduce(CalculatorState prev, String id) { ... }
}
```

`_reduce` is a direct port of the `setState` callback body in `useCalculator.ts`. The logic is identical: same action IDs, same guard conditions, same memory/stoMode behavior.

### 5.2 SettingsController

```dart
enum CalcThemeId { amoled, dark, light, retro }

class SettingsController extends ChangeNotifier {
  CalcThemeId themeId = CalcThemeId.amoled;
  AngleMode defaultAngleMode = AngleMode.deg;
  int resultPrecision = 0;       // 0 = auto
  bool showShiftLabels = true;
  bool hapticFeedback = true;

  late SharedPreferences _prefs;

  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
    // load each field from _prefs with fallback to defaults
  }

  void setTheme(CalcThemeId id) {
    themeId = id;
    _prefs.setString('theme', id.name);
    notifyListeners();
  }

  // setDefaultAngleMode, setResultPrecision, setShowShiftLabels, setHapticFeedback — same pattern
}
```

### 5.3 App Root

```dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final settings = SettingsController();
  await settings.init();
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: settings),
        ChangeNotifierProvider(create: (_) => CalculatorController()),
      ],
      child: const CalcApp(),
    ),
  );
}
```

`CalcApp` is a `Consumer<SettingsController>` that rebuilds `MaterialApp` with the correct `theme` when the theme setting changes.

---

## 6. Theme System

### 6.1 CalcTheme Extension

```dart
class CalcTheme extends ThemeExtension<CalcTheme> {
  final Color background;
  final Color displayBg;
  final Color buttonBg;
  final Color buttonBorder;
  final Color digitText;
  final Color fnText;
  final Color opText;
  final Color eqBg;
  final Color eqText;
  final Color delText;
  final Color secondaryLabel;
  final Color expressionText;
  final Color resultText;
  final Color shiftActiveColor;
  final Color statusBadgeColor;

  @override
  CalcTheme copyWith({ ... }) { ... }

  @override
  CalcTheme lerp(ThemeExtension<CalcTheme>? other, double t) { ... }
}
```

Access in any widget: `Theme.of(context).extension<CalcTheme>()!`

### 6.2 Four Themes

| ID | Background | Button bg | `=` button | Operator text | Character |
|---|---|---|---|---|---|
| `amoled` | `#000000` | `#000000` | white bg / black text | white | Current AMOLED design |
| `dark` | `#121212` | `#1E1E1E` | `#448AFF` bg / white text | `#81D4FA` | Material dark + blue accent |
| `light` | `#F5F5F5` | `#FFFFFF` | `#1565C0` bg / white text | `#1565C0` | Clean light / professional |
| `retro` | `#0D0D0D` | `#0D0D0D` | `#00CC33` bg / black text | `#00FF41` | Matrix green / terminal |

Each theme also specifies `delText` (AMOLED: `#EF4444`, dark: `#FF5252`, light: `#D32F2F`, retro: `#FF3300`), `secondaryLabel`, `shiftActiveColor`, `statusBadgeColor`.

`ThemeData` for each theme sets `scaffoldBackgroundColor` = `CalcTheme.background`, `appBarTheme` matching the same, and embeds the `CalcTheme` extension.

---

## 7. Calculator Screen

`CalculatorScreen` is a `Scaffold` with no AppBar. Body is a `Column`:

```
Column
  CalcDisplay              ← flex: 0 (intrinsic height, ~30% screen)
  ButtonGrid               ← Expanded (fills rest)
```

The display header's right side gains a `⚙` icon button that calls `Navigator.push(context, MaterialPageRoute(builder: (_) => SettingsScreen()))`.

`CalcDisplay` and `ButtonGrid` are `Consumer<CalculatorController>` widgets. They read `CalcTheme` for colors.

Haptic feedback: every button press checks `context.read<SettingsController>().hapticFeedback` and calls `HapticFeedback.lightImpact()` if true.

---

## 8. Settings Screen

`SettingsScreen` is a `Scaffold` with an `AppBar` titled "Settings". Background and AppBar colors follow `CalcTheme`. Body is a `ListView`:

```
Section: APPEARANCE
  ThemePickerGrid          ← 2×2 GridView
    ThemeCard (each)       ← InkWell, rounded, shows name + 3-color swatch + checkmark

Section: CALCULATOR
  ListTile: Default angle mode  → SegmentedButton<AngleMode>([DEG, RAD])
  ListTile: Result precision    → DropdownButton([Auto, 4, 6, 8, 10])

Section: DISPLAY
  SwitchListTile: Show shift labels

Section: BEHAVIOR
  SwitchListTile: Haptic feedback
```

Each control reads from `context.watch<SettingsController>()` and writes via the setter methods.

`ThemeCard` preview swatch: a `Row` of 3 small colored `Container` boxes showing `buttonBg`, `opText` (as a colored border or fill), and `eqBg`. The active theme card shows a checkmark overlay.

---

## 9. Natural Display (AST Widgets)

### 9.1 Layout

`ASTRenderer` maps each `ASTNode` to its widget. For inline flat nodes (numbers, operators, constants), it uses a `Wrap` with no spacing, same line. Structural nodes embed child `ASTRenderer` calls.

| Node | Widget |
|---|---|
| `NumberNode` | `Text` |
| `OperatorNode` | `Text` (themed `opText` color) |
| `ConstantNode` | `Text` (italic for `pi`/`e`, styled for `A`/`B`/`X`/`Y`) |
| `FractionNode` | `IntrinsicWidth(Column([numeratorRow, Divider, denominatorRow]))` |
| `ExponentNode` | `Row([base, Transform.translate(offset: Offset(0, -fontSize*0.4), child: smallerExponent)])` |
| `RadicalNode` | `CustomPaint` draws radical sign; radicand is a child widget |
| `FunctionNode` | `Row([Text(name+'('), argument, Text(')')])` |
| `FactorialNode` | `Row([operand, Text('!')])` |
| `NcrNode` / `NprNode` | `Row([n, Text('C'/'P'), r])` |
| `ParenNode` | `Row([Text('('), children, Text(')')])` |

### 9.2 Cursor

`CursorWidget` is a blinking vertical bar driven by an `AnimationController` (500ms on/off). It inserts into the flat node list at `cursor.insertAt` when `cursor.path` is empty, or recursively into the matching child slot otherwise.

### 9.3 Tap-to-position

Each leaf node widget wraps in `GestureDetector(onTapDown: ...)`. The tap handler calls `context.read<CalculatorController>().handleCursorJump(path, insertAt)` with the path encoded at widget construction time.

---

## 10. Release Script (`scripts/release.sh`)

```bash
#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?Usage: release.sh <version> [notes]}"
NOTES="${2:-}"
TAG="v${VERSION}"
APK="build/app/outputs/flutter-apk/app-release.apk"
APK_ASSET="${APK}#scientific-calculator-${TAG}.apk"

# Guard: require gh and flutter
command -v gh    >/dev/null 2>&1 || { echo "gh CLI not found"; exit 1; }
command -v flutter >/dev/null 2>&1 || { echo "flutter not found"; exit 1; }

# Bump pubspec version
BUILD=$(date +%s)
sed -i.bak "s/^version: .*/version: ${VERSION}+${BUILD}/" pubspec.yaml && rm pubspec.yaml.bak

# Build
echo "Building release APK..."
flutter build apk --release

# Commit + tag
git add pubspec.yaml
git commit -m "chore: bump version to ${VERSION}"
git tag "${TAG}"
git push origin main
git push origin "${TAG}"

# GitHub Release
echo "Creating GitHub Release ${TAG}..."
gh release create "${TAG}" \
  --title "${TAG}" \
  --notes "${NOTES}" \
  "${APK_ASSET}"

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
echo "Done: https://github.com/${REPO}/releases/tag/${TAG}"
```

The script is `chmod +x`. No GitHub Actions CI is used. The build, tag, and release all happen locally on the developer's machine.

---

## 11. Testing

Unit tests in `test/` mirror the existing Vitest suites:

| Test file | Covers |
|---|---|
| `test/core/ast/builder_test.dart` | All `insert*` functions |
| `test/core/ast/cursor_test.dart` | `moveCursor*` functions |
| `test/core/ast/serializer_test.dart` | `serialize()` round-trips |
| `test/core/evaluator_test.dart` | `evaluate()`, `toFraction()`, `toMixed()`, `fromMixed()` |
| `test/controllers/calculator_controller_test.dart` | Button dispatch, memory, stoMode |

Widget tests are out of scope for the initial port.

---

## 12. Out of Scope

- iOS support
- Web/desktop targets
- PWA / service worker
- Keyboard input (no physical keyboard on Android calculator)
- History panel appearance changes beyond what the theme system covers
- Any new calculation functions beyond what the web app has (settings and themes are in scope)
