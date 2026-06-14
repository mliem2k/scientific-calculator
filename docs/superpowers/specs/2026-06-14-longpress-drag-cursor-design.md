# Long-Press Drag Cursor — Design Spec

**Date:** 2026-06-14
**Scope:** Flutter calculator app expression display only
**Approach:** Position hit-test drag — long-press activates drag mode, finger position maps directly to nearest cursor boundary in the rendered expression

## Goal

Allow users to reposition the cursor in the expression display by holding a finger on the expression and dragging left or right, rather than requiring a precise tap on a specific node.

## Architecture

### Files Changed

- `lib/widgets/ast/ast_renderer.dart` — add optional `rowKey: GlobalKey?` parameter to `ASTRenderer`, attach it to the root `Row`
- `lib/widgets/calc_display.dart` — extract `_ExpressionArea` stateful widget from the existing expression `Selector` block; add `_cursorAtX` standalone function

### No Changes To

- `CalculatorController` — `handleCursorJump(path, insertAt)` is already the correct API
- Button grid, themes, settings, evaluator

## Components

### `ASTRenderer` — new `rowKey` parameter

```dart
class ASTRenderer extends StatelessWidget {
  final List<ASTNode> nodes;
  final Cursor cursor;
  final List<CursorSegment> path;
  final void Function(List<CursorSegment> path, int insertAt) onCursorJump;
  final GlobalKey? rowKey;   // NEW — optional, attached to root Row

  const ASTRenderer({
    super.key,
    required this.nodes,
    required this.cursor,
    required this.path,
    required this.onCursorJump,
    this.rowKey,             // NEW
  });
```

The root `Row` in `build()` gains `key: rowKey`:

```dart
return Row(
  key: rowKey,               // NEW
  mainAxisSize: MainAxisSize.min,
  crossAxisAlignment: CrossAxisAlignment.center,
  children: children,
);
```

All existing call sites pass no `rowKey` — default `null` — so they are unaffected.

### `_ExpressionArea` — new stateful widget in `calc_display.dart`

Extracted from the existing expression `Selector` block in `CalcDisplay.build()`. Manages:
- `final GlobalKey _rowKey = GlobalKey()` — passed to `ASTRenderer`
- `bool _dragging = false` — drag mode state (drives no UI change currently, reserved for future magnifier)

```dart
class _ExpressionArea extends StatefulWidget {
  const _ExpressionArea();

  @override
  State<_ExpressionArea> createState() => _ExpressionAreaState();
}

class _ExpressionAreaState extends State<_ExpressionArea> {
  final GlobalKey _rowKey = GlobalKey();
  bool _dragging = false;
  // ...
}
```

### Gesture handling

`_ExpressionArea` wraps the `SingleChildScrollView` in a `GestureDetector`:

```dart
GestureDetector(
  onLongPressStart: _onLongPressStart,
  onLongPressMoveUpdate: _onLongPressMoveUpdate,
  onLongPressEnd: (_) => setState(() => _dragging = false),
  onLongPressCancel: () => setState(() => _dragging = false),
  child: SingleChildScrollView(...),
)
```

**`_onLongPressStart`:**
1. `setState(() => _dragging = true)`
2. `HapticFeedback.selectionClick()`
3. Resolve cursor at `details.globalPosition.dx` and call `controller.handleCursorJump([], index)`

**`_onLongPressMoveUpdate`:**
1. Resolve cursor at `details.globalPosition.dx`
2. Call `controller.handleCursorJump([], index)` — fires on every pointer event for real-time tracking

**Scroll conflict:** Flutter's gesture arena gives the long-press gesture priority after the ~500ms threshold. Once claimed, the `SingleChildScrollView` inside does not activate scroll physics for that pointer event. No `ScrollController` locking required.

**Scope:** Only top-level cursor positions (path = `[]`) are set by drag. If the cursor is inside a fraction or exponent when the drag starts, `handleCursorJump([], index)` moves it to the top level. Entering sub-expressions still requires a direct tap.

### `_cursorAtX` — standalone hit-test function

`CursorWidget` renders as `Container(width: 2)` — not near-zero, so we cannot skip it by width threshold. Instead, we skip it by its known child index: when the cursor is at the top level (`cursor.path.isEmpty`), the `CursorWidget` sits at children index `cursor.insertAt` in the `Row`'s child list (per the build loop in `ASTRenderer.build()`). When the cursor is inside a sub-expression, no `CursorWidget` appears in the top-level Row.

```dart
int _cursorAtX(RenderBox rowBox, double globalX, int nodeCount, int? cursorChildIndex) {
  // cursorChildIndex: the Row child index of the CursorWidget, or null if no
  // CursorWidget is in this Row (cursor is inside a sub-expression).
  final localX = rowBox.globalToLocal(Offset(globalX, 0)).dx;
  final nodeMidpoints = <double>[];
  int childIndex = 0;

  rowBox.visitChildren((child) {
    if (childIndex == cursorChildIndex) {
      childIndex++;
      return; // skip CursorWidget
    }
    final childBox = child as RenderBox;
    final data = child.parentData as FlexParentData;
    nodeMidpoints.add(data.offset.dx + childBox.size.width / 2);
    childIndex++;
  });

  for (int i = 0; i < nodeMidpoints.length; i++) {
    if (localX <= nodeMidpoints[i]) return i;
  }
  return nodeCount;
}
```

Called from `_ExpressionAreaState` as:

```dart
void _moveCursorTo(double globalX) {
  final renderBox = _rowKey.currentContext?.findRenderObject() as RenderBox?;
  if (renderBox == null) return;
  final controller = context.read<CalculatorController>();
  final state = controller.state;
  final nodeCount = state.expression.length;
  // CursorWidget is in the top-level Row only when cursor.path is empty.
  // When present, its child index equals cursor.insertAt.
  final cursorChildIndex = state.cursor.path.isEmpty ? state.cursor.insertAt : null;
  final index = _cursorAtX(renderBox, globalX, nodeCount, cursorChildIndex);
  controller.handleCursorJump([], index);
}
```

## Data Flow

```
Long press detected
  → _onLongPressStart(details)
    → HapticFeedback.selectionClick()
    → _moveCursorTo(details.globalPosition.dx)
      → _rowKey → RenderBox
      → _cursorAtX(renderBox, x, nodeCount) → insertAt
      → controller.handleCursorJump([], insertAt)
        → state.cursor updated → Selector rebuilds → CursorWidget repositions

Finger moves
  → _onLongPressMoveUpdate(details)
    → _moveCursorTo(details.globalPosition.dx)
      → (same path as above)

Finger lifts
  → _dragging = false
```

## Constraints and Non-Changes

- No cursor movement into nested sub-expressions (fractions, exponents) via drag
- No magnifier / loupe UI (reserved for future enhancement)
- `_dragging` state stored for future use (e.g. showing a drag handle indicator)
- The `rowKey` is only attached to the **top-level** `ASTRenderer` call in `_ExpressionArea` — nested calls (fraction numerator/denominator, exponent, etc.) pass no `rowKey`
- Existing tap-to-position behavior on individual nodes is unchanged
