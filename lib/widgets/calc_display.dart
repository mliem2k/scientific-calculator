import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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

class CalcDisplay extends ConsumerWidget {
  const CalcDisplay({super.key, this.onSettings, this.onHistory});

  final VoidCallback? onSettings;
  final VoidCallback? onHistory;

  void _copyResult(String? result) {
    if (result == null) return;
    Clipboard.setData(ClipboardData(text: result));
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final badges = ref.watch(calculatorProvider.select((s) => (
          shift: s.shiftActive,
          hyp: s.hypActive,
          sto: s.stoMode,
        )));
    final result = ref.watch(calculatorProvider.select((s) => s.result));

    return Container(
      color: ct.displayBg,
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const _DegRadBadge(),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (badges.shift)
                    _StatusBadge(
                        label: 'SHIFT',
                        ct: ct,
                        bgColor: ct.shiftActiveColor),
                  if (badges.hyp) _StatusBadge(label: 'HYP', ct: ct),
                  if (badges.sto) _StatusBadge(label: 'STO', ct: ct),
                ],
              ),
              const Spacer(),
              if (onHistory != null)
                GestureDetector(
                  onTap: onHistory,
                  child: Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: Icon(Icons.history,
                        size: 20, color: ct.secondaryLabel),
                  ),
                ),
              if (onSettings != null)
                GestureDetector(
                  onTap: onSettings,
                  child: Icon(Icons.settings,
                      size: 20, color: ct.secondaryLabel),
                ),
            ],
          ),
          const Spacer(),
          const _ExpressionArea(),
          const SizedBox(height: 4),
          if (result != null)
            GestureDetector(
              onTap: () => _copyResult(result),
              child: Text(
                result,
                style: TextStyle(
                    color: ct.resultText,
                    fontSize: 34,
                    fontWeight: FontWeight.w500),
                textAlign: TextAlign.right,
              ),
            ),
        ],
      ),
    );
  }
}

class _DegRadBadge extends ConsumerWidget {
  const _DegRadBadge();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final angleMode =
        ref.watch(calculatorProvider.select((s) => s.angleMode));
    return GestureDetector(
      onTap: () =>
          ref.read(calculatorProvider.notifier).handleButton('DEG_RAD'),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 2),
        child: Text(
          angleMode == AngleMode.deg ? 'DEG' : 'RAD',
          style: TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w700,
            color: angleMode == AngleMode.rad
                ? const Color(0xFF4FC3F7)
                : ct.secondaryLabel,
          ),
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.label, required this.ct, this.bgColor});

  final String label;
  final CalcTheme ct;
  final Color? bgColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(left: 4),
      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
      decoration: BoxDecoration(
        color: bgColor ?? ct.statusBadgeColor,
        borderRadius: BorderRadius.circular(3),
      ),
      child: Text(label,
          style: const TextStyle(fontSize: 14, color: Colors.white)),
    );
  }
}

class _ExpressionArea extends ConsumerStatefulWidget {
  const _ExpressionArea();

  @override
  ConsumerState<_ExpressionArea> createState() => _ExpressionAreaState();
}

class _ExpressionAreaState extends ConsumerState<_ExpressionArea> {
  final GlobalKey _rowKey = GlobalKey();

  void _moveCursorTo(double globalX) {
    if (!mounted) return;
    final renderBox =
        _rowKey.currentContext?.findRenderObject() as RenderBox?;
    if (renderBox == null) return;
    final state = ref.read(calculatorProvider);
    final nodeCount = state.expression.length;
    // Render tree is one frame behind mutated state, but the 2px cursor
    // widget makes any single-event skip error imperceptible.
    final cursorChildIndex =
        state.cursor.path.isEmpty ? state.cursor.insertAt : null;
    final index =
        _cursorAtX(renderBox, globalX, nodeCount, cursorChildIndex);
    ref.read(calculatorProvider.notifier).handleCursorJump([], index);
  }

  void _onLongPressStart(LongPressStartDetails details) {
    HapticFeedback.selectionClick();
    _moveCursorTo(details.globalPosition.dx);
  }

  void _onLongPressMoveUpdate(LongPressMoveUpdateDetails details) {
    _moveCursorTo(details.globalPosition.dx);
  }

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final notifier = ref.read(calculatorProvider.notifier);
    final data = ref.watch(calculatorProvider.select((s) => (
          expression: s.expression,
          cursor: s.cursor,
          hasResult: s.result != null,
        )));
    final exprFontSize = data.hasResult ? 28.0 : 40.0;

    final Widget content = data.expression.isEmpty
        ? Align(
            alignment: Alignment.centerRight,
            child: Text('0',
                style: TextStyle(
                    color: ct.buttonBorder, fontSize: exprFontSize)),
          )
        : SingleChildScrollView(
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
                onCursorJump: notifier.handleCursorJump,
              ),
            ),
          );

    return GestureDetector(
      onLongPressStart: _onLongPressStart,
      onLongPressMoveUpdate: _onLongPressMoveUpdate,
      child: content,
    );
  }
}
