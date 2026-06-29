import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../controllers/calculator_controller.dart';
import '../controllers/settings_controller.dart';
import '../theme/calc_theme.dart';

class _BtnDef {
  final String id;
  final String label;
  final String? shiftLabel;
  final _BtnRole role;
  final int flex;
  const _BtnDef(this.id, this.label,
      {this.shiftLabel, required this.role, this.flex = 1});
}

enum _BtnRole { digit, fn, op, del, eq }

const _rows = [
  [
    _BtnDef('SHIFT', 'SHIFT', role: _BtnRole.fn),
    _BtnDef('HYP', 'HYP', role: _BtnRole.fn),
    _BtnDef('pi', 'π', role: _BtnRole.fn),
    _BtnDef('e_const', 'e', shiftLabel: 'eˣ', role: _BtnRole.fn),
    _BtnDef('Ans', 'Ans', role: _BtnRole.fn),
  ],
  [
    _BtnDef('Sto', 'Sto', shiftLabel: 'Paste Code', role: _BtnRole.fn),
    _BtnDef('exponent', 'xʸ', shiftLabel: 'ⁿ√', role: _BtnRole.fn),
    _BtnDef('sin', 'sin(', shiftLabel: 'sin⁻¹(', role: _BtnRole.fn),
    _BtnDef('cos', 'cos(', shiftLabel: 'cos⁻¹(', role: _BtnRole.fn),
    _BtnDef('tan', 'tan(', shiftLabel: 'tan⁻¹(', role: _BtnRole.fn),
  ],
  [
    _BtnDef('abs', 'Abs', role: _BtnRole.fn),
    _BtnDef('fraction', 'a/b', shiftLabel: 'mix', role: _BtnRole.fn),
    _BtnDef('sqrt', '√', shiftLabel: '³√', role: _BtnRole.fn),
    _BtnDef('log', 'log(', shiftLabel: 'a↔cd/e', role: _BtnRole.fn),
    _BtnDef('ln', 'ln(', shiftLabel: 'Dec↔a/b', role: _BtnRole.fn),
  ],
  [
    _BtnDef('factorial', 'x!', shiftLabel: 'A', role: _BtnRole.fn),
    _BtnDef('cube', 'x³', shiftLabel: 'B', role: _BtnRole.fn),
    _BtnDef('square', 'x²', shiftLabel: 'X', role: _BtnRole.fn),
    _BtnDef('paren_open', '(', role: _BtnRole.fn),
    _BtnDef('paren_close', ')', shiftLabel: 'Y', role: _BtnRole.fn),
  ],
  [
    _BtnDef('7', '7', role: _BtnRole.digit),
    _BtnDef('8', '8', role: _BtnRole.digit),
    _BtnDef('9', '9', role: _BtnRole.digit),
    _BtnDef('DEL', 'DEL', role: _BtnRole.del),
    _BtnDef('AC', 'AC', role: _BtnRole.del),
  ],
  [
    _BtnDef('4', '4', role: _BtnRole.digit),
    _BtnDef('5', '5', role: _BtnRole.digit),
    _BtnDef('6', '6', role: _BtnRole.digit),
    _BtnDef('multiply', '×', shiftLabel: 'nPr', role: _BtnRole.op),
    _BtnDef('divide', '÷', shiftLabel: 'nCr', role: _BtnRole.op),
  ],
  [
    _BtnDef('1', '1', role: _BtnRole.digit),
    _BtnDef('2', '2', role: _BtnRole.digit),
    _BtnDef('3', '3', role: _BtnRole.digit),
    _BtnDef('plus', '+', role: _BtnRole.op),
    _BtnDef('minus', '−', role: _BtnRole.op),
  ],
  [
    _BtnDef('0', '0', role: _BtnRole.digit, flex: 2),
    _BtnDef('.', '.', shiftLabel: 'EXP', role: _BtnRole.digit),
    _BtnDef('=', '=', shiftLabel: 'S⇔D', role: _BtnRole.eq),
  ],
];

String _actionId(String id, bool shiftActive) {
  if (!shiftActive) return id;
  return switch (id) {
    'sin' => 'asin',
    'cos' => 'acos',
    'tan' => 'atan',
    'exponent' => 'nthRoot',
    'sqrt' => 'cbrt',
    'log' => 'MixedFrac',
    'ln' => 'DecFrac',
    'fraction' => 'mix',
    'multiply' => 'npr',
    'divide' => 'ncr',
    '=' => 'S_TO_D',
    'factorial' => 'A',
    'cube' => 'B',
    'square' => 'X',
    'paren_close' => 'Y',
    'Sto' => 'PasteCode',
    'e_const' => 'exp',
    '.' => 'EXP',
    _ => id,
  };
}

String _displayLabel(_BtnDef btn, bool shiftActive) {
  if (shiftActive && btn.shiftLabel != null) return btn.shiftLabel!;
  return btn.label;
}

class ButtonGrid extends ConsumerWidget {
  const ButtonGrid({
    super.key,
    required this.onButton,
    required this.onPaste,
  });

  final void Function(String id) onButton;
  final Future<void> Function() onPaste;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final shiftActive =
        ref.watch(calculatorProvider.select((s) => s.shiftActive));

    void handleTap(BuildContext ctx, _BtnDef btn, bool shift) {
      if (ref.read(settingsProvider).hapticFeedback) {
        HapticFeedback.lightImpact();
      }
      final actionId = _actionId(btn.id, shift);
      if (actionId == 'PasteCode') {
        onPaste();
      } else {
        onButton(actionId);
      }
    }

    return RepaintBoundary(
      child: LayoutBuilder(
        builder: (context, constraints) {
          final rowH = constraints.maxHeight.isFinite
              ? constraints.maxHeight / _rows.length
              : 60.0;
          return Column(
            children: [
              for (final row in _rows)
                SizedBox(
                  height: rowH,
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      for (final btn in row)
                        Expanded(
                          flex: btn.flex,
                          child: _CalcButton(
                            btn: btn,
                            shiftActive: shiftActive,
                            onTap: handleTap,
                          ),
                        ),
                    ],
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}

class _CalcButton extends StatefulWidget {
  const _CalcButton({
    required this.btn,
    required this.shiftActive,
    required this.onTap,
  });

  final _BtnDef btn;
  final bool shiftActive;
  final void Function(BuildContext, _BtnDef, bool) onTap;

  @override
  State<_CalcButton> createState() => _CalcButtonState();
}

class _CalcButtonState extends State<_CalcButton> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final btn = widget.btn;
    final shiftActive = widget.shiftActive;
    final label = _displayLabel(btn, shiftActive);

    final Color bgColor;
    final Color labelColor;
    if (btn.role == _BtnRole.eq) {
      bgColor = _pressed
          ? ct.eqBg.withValues(alpha: 200 / 255)
          : ct.eqBg;
      labelColor = ct.eqText;
    } else {
      bgColor = _pressed
          ? Color.alphaBlend(
              Colors.white.withValues(alpha: 30 / 255), ct.buttonBg)
          : ct.buttonBg;
      labelColor = switch (btn.role) {
        _BtnRole.digit => ct.digitText,
        _BtnRole.fn => ct.fnText,
        _BtnRole.op => ct.opText,
        _BtnRole.del => ct.delText,
        _BtnRole.eq => ct.eqText,
      };
    }

    return GestureDetector(
      onTapDown: (_) => setState(() => _pressed = true),
      onTapUp: (_) => setState(() => _pressed = false),
      onTapCancel: () => setState(() => _pressed = false),
      onTap: () => widget.onTap(context, btn, shiftActive),
      child: Container(
        decoration: BoxDecoration(
          color: bgColor,
          border: Border(
            right: BorderSide(color: ct.buttonBorder),
            bottom: BorderSide(color: ct.buttonBorder),
          ),
        ),
        child: Stack(
          children: [
            if (!shiftActive && btn.shiftLabel != null)
              Positioned(
                top: 2,
                left: 4,
                child: btn.id == 'fraction'
                    ? _FractionIcon(
                        size: 13, color: ct.secondaryLabel, mixed: true)
                    : Text(
                        btn.shiftLabel!,
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                          color: ct.secondaryLabel,
                        ),
                      ),
              ),
            Center(
              child: btn.id == 'fraction'
                  ? _FractionIcon(
                      size: 22, color: labelColor, mixed: shiftActive)
                  : Text(
                      label,
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w700,
                        color: labelColor,
                      ),
                      textAlign: TextAlign.center,
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FractionIcon extends StatelessWidget {
  const _FractionIcon({
    required this.size,
    required this.color,
    required this.mixed,
  });

  final double size;
  final Color color;
  final bool mixed;

  Widget _stackedFraction() {
    final subSize = size * 0.42;
    return IntrinsicWidth(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Center(
            child: Text('a',
                style: TextStyle(
                    fontSize: subSize,
                    fontWeight: FontWeight.w700,
                    color: color,
                    height: 1.1)),
          ),
          Container(height: 1.5, color: color),
          Center(
            child: Text('b',
                style: TextStyle(
                    fontSize: subSize,
                    fontWeight: FontWeight.w700,
                    color: color,
                    height: 1.1)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (!mixed) return _stackedFraction();
    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Text('1',
            style: TextStyle(
                fontSize: size * 0.55,
                fontWeight: FontWeight.w700,
                color: color)),
        const SizedBox(width: 2),
        _stackedFraction(),
      ],
    );
  }
}
