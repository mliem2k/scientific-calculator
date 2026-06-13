import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../controllers/calculator_controller.dart';
import '../core/ast/types.dart';
import '../theme/calc_theme.dart';
import 'ast/ast_renderer.dart';

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
    final controller = context.read<CalculatorController>();

    return Container(
      color: ct.displayBg,
      padding: const EdgeInsets.all(12),
      constraints: const BoxConstraints(minHeight: 120),
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
              mainAxisSize: MainAxisSize.min,
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
                Selector<CalculatorController,
                    ({List<ASTNode> expression, Cursor cursor})>(
                  selector: (_, c) => (
                    expression: c.state.expression,
                    cursor: c.state.cursor,
                  ),
                  builder: (ctx, data, __) {
                    if (data.expression.isEmpty) {
                      return Align(
                        alignment: Alignment.centerRight,
                        child: Text(
                          '0',
                          style: TextStyle(color: ct.buttonBorder, fontSize: 40),
                        ),
                      );
                    }
                    return SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      reverse: true,
                      child: DefaultTextStyle(
                        style: TextStyle(
                          color: ct.expressionText,
                          fontSize: 40,
                          fontWeight: FontWeight.w600,
                        ),
                        child: ASTRenderer(
                          nodes: data.expression,
                          cursor: data.cursor,
                          path: const [],
                          onCursorJump: controller.handleCursorJump,
                        ),
                      ),
                    );
                  },
                ),
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
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      mainAxisSize: MainAxisSize.min,
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
