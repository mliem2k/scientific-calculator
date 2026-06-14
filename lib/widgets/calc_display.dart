import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../controllers/calculator_controller.dart';
import '../core/ast/types.dart';
import '../theme/calc_theme.dart';
import 'ast/ast_renderer.dart';

int _cursorAtX(
  RenderBox rowBox,
  double globalX,
  int nodeCount,
  int? cursorChildIndex,
) {
  final localX = rowBox.globalToLocal(Offset(globalX, 0)).dx;
  final nodeMidpoints = <double>[];
  int childIndex = 0;

  rowBox.visitChildren((child) {
    if (childIndex == cursorChildIndex) {
      childIndex++;
      return;
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

class CalcDisplay extends StatelessWidget {
  final VoidCallback? onSettings;

  const CalcDisplay({super.key, this.onSettings});

  void _copyResult(String? result) {
    if (result == null) return;
    Clipboard.setData(ClipboardData(text: result));
  }

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;

    return Container(
      color: ct.displayBg,
      padding: const EdgeInsets.all(12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Left strip: SHIFT, copy, angle toggle
          _DisplayStrip(ct: ct),
          const SizedBox(width: 8),
          // Main area
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                // Top row: status badges + settings button
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    Selector<CalculatorController, ({bool hyp, bool sto})>(
                      selector: (_, c) => (
                        hyp: c.state.hypActive,
                        sto: c.state.stoMode,
                      ),
                      builder: (_, badges, __) => Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          if (badges.hyp) _StatusBadge(label: 'HYP', ct: ct),
                          if (badges.sto) _StatusBadge(label: 'STO', ct: ct),
                        ],
                      ),
                    ),
                    if (onSettings != null)
                      GestureDetector(
                        onTap: onSettings,
                        child: Padding(
                          padding: const EdgeInsets.only(left: 8),
                          child: Icon(
                            Icons.settings,
                            size: 20,
                            color: ct.secondaryLabel,
                          ),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 4),
                // Expression area
                const _ExpressionArea(),
                const SizedBox(height: 4),
                // Result area
                Selector<CalculatorController, String?>(
                  selector: (_, c) => c.state.result,
                  builder: (_, result, __) {
                    if (result == null) return const SizedBox.shrink();
                    return GestureDetector(
                      onTap: () => _copyResult(result),
                      child: Text(
                        result,
                        style: TextStyle(
                          color: ct.resultText,
                          fontSize: 34,
                          fontWeight: FontWeight.w500,
                        ),
                        textAlign: TextAlign.right,
                      ),
                    );
                  },
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _DisplayStrip extends StatelessWidget {
  final CalcTheme ct;

  const _DisplayStrip({required this.ct});

  @override
  Widget build(BuildContext context) {
    final controller = context.read<CalculatorController>();

    return Column(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        // SHIFT toggle
        Selector<CalculatorController, bool>(
          selector: (_, c) => c.state.shiftActive,
          builder: (_, shiftActive, __) => GestureDetector(
            onTap: () => controller.handleButton('SHIFT'),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 2),
              child: Text(
                '≡',
                style: TextStyle(
                  fontSize: 28,
                  color: shiftActive ? ct.shiftActiveColor : ct.secondaryLabel,
                  fontWeight:
                      shiftActive ? FontWeight.bold : FontWeight.w500,
                ),
              ),
            ),
          ),
        ),
        // Copy result button
        Selector<CalculatorController, String?>(
          selector: (_, c) => c.state.result,
          builder: (_, result, __) => GestureDetector(
            onTap: () {
              if (result != null) {
                Clipboard.setData(ClipboardData(text: result));
              }
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 2),
              child: Text(
                '◁',
                style: TextStyle(
                  fontSize: 20,
                  color: result != null ? ct.secondaryLabel : ct.buttonBorder,
                ),
              ),
            ),
          ),
        ),
        // Angle mode toggle
        Selector<CalculatorController, AngleMode>(
          selector: (_, c) => c.state.angleMode,
          builder: (_, angleMode, __) => GestureDetector(
            onTap: () => controller.handleButton('DEG_RAD'),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 2),
              child: Text(
                angleMode == AngleMode.deg ? 'DEG' : 'RAD',
                style: TextStyle(
                  fontSize: 15,
                  color: angleMode == AngleMode.rad
                      ? const Color(0xFF4FC3F7)
                      : ct.secondaryLabel,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _StatusBadge extends StatelessWidget {
  final String label;
  final CalcTheme ct;

  const _StatusBadge({required this.label, required this.ct});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(left: 4),
      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
      decoration: BoxDecoration(
        color: ct.statusBadgeColor,
        borderRadius: BorderRadius.circular(3),
      ),
      child: Text(
        label,
        style: const TextStyle(fontSize: 14, color: Colors.white),
      ),
    );
  }
}

class _ExpressionArea extends StatefulWidget {
  const _ExpressionArea();

  @override
  State<_ExpressionArea> createState() => _ExpressionAreaState();
}

class _ExpressionAreaState extends State<_ExpressionArea> {
  final GlobalKey _rowKey = GlobalKey();
  // reserved for future drag-highlight affordance
  bool _dragging = false;

  void _moveCursorTo(double globalX) {
    if (!mounted) return;
    final renderBox =
        _rowKey.currentContext?.findRenderObject() as RenderBox?;
    if (renderBox == null) return;
    final controller = context.read<CalculatorController>();
    final state = controller.state;
    final nodeCount = state.expression.length;
    // cursorChildIndex reflects mutated state; render tree is one frame behind,
    // but the 2px cursor widget makes any single-event skip error imperceptible.
    final cursorChildIndex =
        state.cursor.path.isEmpty ? state.cursor.insertAt : null;
    final index =
        _cursorAtX(renderBox, globalX, nodeCount, cursorChildIndex);
    controller.handleCursorJump([], index);
  }

  void _onLongPressStart(LongPressStartDetails details) {
    _dragging = true;
    HapticFeedback.selectionClick();
    _moveCursorTo(details.globalPosition.dx);
  }

  void _onLongPressMoveUpdate(LongPressMoveUpdateDetails details) {
    _moveCursorTo(details.globalPosition.dx);
  }

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final controller = context.read<CalculatorController>();

    return GestureDetector(
      onLongPressStart: _onLongPressStart,
      onLongPressMoveUpdate: _onLongPressMoveUpdate,
      onLongPressEnd: (_) { _dragging = false; },
      onLongPressCancel: () { _dragging = false; },
      child: Selector<CalculatorController,
          ({List<ASTNode> expression, Cursor cursor, bool hasResult})>(
        selector: (_, c) => (
          expression: c.state.expression,
          cursor: c.state.cursor,
          hasResult: c.state.result != null,
        ),
        builder: (ctx, data, __) {
          // Scale expression down when result is visible so both fit in the
          // fixed-height display without pushing the button grid.
          final exprFontSize = data.hasResult ? 28.0 : 40.0;
          if (data.expression.isEmpty) {
            return Align(
              alignment: Alignment.centerRight,
              child: Text(
                '0',
                style: TextStyle(color: ct.buttonBorder, fontSize: exprFontSize),
              ),
            );
          }
          return SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            reverse: true,
            child: DefaultTextStyle(
              style: TextStyle(
                color: ct.expressionText,
                fontSize: exprFontSize,
                fontWeight: FontWeight.w700,
              ),
              child: ASTRenderer(
                rowKey: _rowKey,
                nodes: data.expression,
                cursor: data.cursor,
                path: const [],
                onCursorJump: controller.handleCursorJump,
              ),
            ),
          );
        },
      ),
    );
  }
}
