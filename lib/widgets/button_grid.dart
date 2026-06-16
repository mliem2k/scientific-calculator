import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../controllers/calculator_controller.dart';
import '../controllers/settings_controller.dart';
import '../theme/calc_theme.dart';

class _BtnDef {
  final String id;
  final String label;
  final String? shiftLabel;
  final _BtnRole role;
  final int flex;
  const _BtnDef(this.id, this.label, {this.shiftLabel, required this.role, this.flex = 1});
}

enum _BtnRole { digit, fn, op, del, eq }

const _rows = [
  // Row 1 — Control
  [
    _BtnDef('SHIFT',   'SHIFT',                       role: _BtnRole.fn),
    _BtnDef('HYP',     'HYP',                         role: _BtnRole.fn),
    _BtnDef('pi',      'π',                           role: _BtnRole.fn),
    _BtnDef('e_const', 'e',   shiftLabel: 'eˣ',      role: _BtnRole.fn),
    _BtnDef('Ans',     'Ans',                         role: _BtnRole.fn),
  ],
  // Row 2
  [
    _BtnDef('Sto',         'Sto',    shiftLabel: 'Paste Code', role: _BtnRole.fn),
    _BtnDef('exponent',    'xʸ',     shiftLabel: 'ⁿ√',     role: _BtnRole.fn),
    _BtnDef('sin',         'sin(',   shiftLabel: 'sin⁻¹(', role: _BtnRole.fn),
    _BtnDef('cos',         'cos(',   shiftLabel: 'cos⁻¹(', role: _BtnRole.fn),
    _BtnDef('tan',         'tan(',   shiftLabel: 'tan⁻¹(', role: _BtnRole.fn),
  ],
  // Row 3
  [
    _BtnDef('abs',         'Abs',                          role: _BtnRole.fn),
    _BtnDef('fraction',    'a/b',    shiftLabel: 'mix',    role: _BtnRole.fn),
    _BtnDef('sqrt',        '√',      shiftLabel: '³√',     role: _BtnRole.fn),
    _BtnDef('log',         'log(',   shiftLabel: 'a↔cd/e', role: _BtnRole.fn),
    _BtnDef('ln',          'ln(',    shiftLabel: 'Dec↔a/b',role: _BtnRole.fn),
  ],
  // Row 4
  [
    _BtnDef('factorial',   'x!',     shiftLabel: 'A',      role: _BtnRole.fn),
    _BtnDef('cube',        'x³',     shiftLabel: 'B',      role: _BtnRole.fn),
    _BtnDef('square',      'x²',     shiftLabel: 'X',      role: _BtnRole.fn),
    _BtnDef('paren_open',  '(',                            role: _BtnRole.fn),
    _BtnDef('paren_close', ')',       shiftLabel: 'Y',      role: _BtnRole.fn),
  ],
  // Row 5
  [
    _BtnDef('7',    '7',   role: _BtnRole.digit),
    _BtnDef('8',    '8',   role: _BtnRole.digit),
    _BtnDef('9',    '9',   role: _BtnRole.digit),
    _BtnDef('DEL',  'DEL', role: _BtnRole.del),
    _BtnDef('AC',   'AC',  role: _BtnRole.del),
  ],
  // Row 6
  [
    _BtnDef('4',        '4', role: _BtnRole.digit),
    _BtnDef('5',        '5', role: _BtnRole.digit),
    _BtnDef('6',        '6', role: _BtnRole.digit),
    _BtnDef('multiply', '×', shiftLabel: 'nPr', role: _BtnRole.op),
    _BtnDef('divide',   '÷', shiftLabel: 'nCr', role: _BtnRole.op),
  ],
  // Row 7
  [
    _BtnDef('1',     '1', role: _BtnRole.digit),
    _BtnDef('2',     '2', role: _BtnRole.digit),
    _BtnDef('3',     '3', role: _BtnRole.digit),
    _BtnDef('plus',  '+', role: _BtnRole.op),
    _BtnDef('minus', '−', role: _BtnRole.op),
  ],
  // Row 8 — Bottom (double-wide 0, = last)
  [
    _BtnDef('0',      '0',   role: _BtnRole.digit, flex: 2),
    _BtnDef('.',      '.',   shiftLabel: 'EXP',    role: _BtnRole.digit),
    _BtnDef('=',      '=',   shiftLabel: 'S⇔D',   role: _BtnRole.eq),
  ],
];

String _actionId(String id, bool shiftActive) {
  if (!shiftActive) return id;
  switch (id) {
    case 'sin':         return 'asin';
    case 'cos':         return 'acos';
    case 'tan':         return 'atan';
    case 'exponent':    return 'nthRoot';
    case 'sqrt':        return 'cbrt';
    case 'log':         return 'MixedFrac';
    case 'ln':          return 'DecFrac';
    case 'fraction':    return 'mix';
    case 'multiply':    return 'npr';
    case 'divide':      return 'ncr';
    case '=':           return 'S_TO_D';
    case 'factorial':   return 'A';
    case 'cube':        return 'B';
    case 'square':      return 'X';
    case 'paren_close': return 'Y';
    case 'Sto':         return 'PasteCode';
    case 'e_const':     return 'exp';  // SHIFT+e → eˣ
    case '.':           return 'EXP';  // SHIFT+. → EXP notation
    default:            return id;
  }
}

String _displayLabel(_BtnDef btn, bool shiftActive) {
  if (shiftActive && btn.shiftLabel != null) return btn.shiftLabel!;
  return btn.label;
}

class ButtonGrid extends StatelessWidget {
  final void Function(String id) onButton;
  final Future<void> Function() onPaste;

  const ButtonGrid({
    super.key,
    required this.onButton,
    required this.onPaste,
  });

  void _handleTap(BuildContext context, _BtnDef btn, bool shiftActive) {
    final haptic = context.read<SettingsController>().hapticFeedback;
    if (haptic) HapticFeedback.lightImpact();
    final actionId = _actionId(btn.id, shiftActive);
    if (actionId == 'PasteCode') {
      onPaste();
    } else {
      onButton(actionId);
    }
  }

  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      child: Selector<CalculatorController, bool>(
        selector: (_, c) => c.state.shiftActive,
        builder: (context, shiftActive, _) {
          return LayoutBuilder(
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
                                onTap: _handleTap,
                              ),
                            ),
                        ],
                      ),
                    ),
                ],
              );
            },
          );
        },
      ),
    );
  }
}

class _CalcButton extends StatefulWidget {
  final _BtnDef btn;
  final bool shiftActive;
  final void Function(BuildContext, _BtnDef, bool) onTap;

  const _CalcButton({
    required this.btn,
    required this.shiftActive,
    required this.onTap,
  });

  @override
  State<_CalcButton> createState() => _CalcButtonState();
}

class _CalcButtonState extends State<_CalcButton> {
  bool _pressed = false;

  void _onTapDown(TapDownDetails _) {
    setState(() => _pressed = true);
  }

  void _onTapUp(TapUpDetails _) {
    setState(() => _pressed = false);
  }

  void _onTapCancel() {
    setState(() => _pressed = false);
  }

  void _onTap() {
    widget.onTap(context, widget.btn, widget.shiftActive);
  }

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final btn = widget.btn;
    final shiftActive = widget.shiftActive;
    final label = _displayLabel(btn, shiftActive);

    final Color bgColor;
    final Color labelColor;
    if (btn.role == _BtnRole.eq) {
      bgColor = _pressed ? ct.eqBg.withAlpha(200) : ct.eqBg;
      labelColor = ct.eqText;
    } else {
      bgColor = _pressed
          ? Color.alphaBlend(Colors.white.withAlpha(30), ct.buttonBg)
          : ct.buttonBg;
      labelColor = switch (btn.role) {
        _BtnRole.digit  => ct.digitText,
        _BtnRole.fn     => ct.fnText,
        _BtnRole.op     => ct.opText,
        _BtnRole.del    => ct.delText,
        _BtnRole.eq     => ct.eqText,
      };
    }

    return GestureDetector(
      onTapDown: _onTapDown,
      onTapUp: _onTapUp,
      onTapCancel: _onTapCancel,
      onTap: _onTap,
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
                    ? _FractionIcon(size: 13, color: ct.secondaryLabel, mixed: true)
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
                      size: 22,
                      color: labelColor,
                      mixed: shiftActive,
                    )
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
  final double size;
  final Color color;
  final bool mixed;

  const _FractionIcon({
    required this.size,
    required this.color,
    required this.mixed,
  });

  Widget _stackedFraction() {
    final subSize = size * 0.42;
    return IntrinsicWidth(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Center(
            child: Text(
              'a',
              style: TextStyle(
                fontSize: subSize,
                fontWeight: FontWeight.w700,
                color: color,
                height: 1.1,
              ),
            ),
          ),
          Container(height: 1.5, color: color),
          Center(
            child: Text(
              'b',
              style: TextStyle(
                fontSize: subSize,
                fontWeight: FontWeight.w700,
                color: color,
                height: 1.1,
              ),
            ),
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
        Text(
          '1',
          style: TextStyle(
            fontSize: size * 0.55,
            fontWeight: FontWeight.w700,
            color: color,
          ),
        ),
        const SizedBox(width: 2),
        _stackedFraction(),
      ],
    );
  }
}
