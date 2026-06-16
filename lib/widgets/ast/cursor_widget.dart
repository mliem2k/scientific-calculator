import 'package:flutter/material.dart';
import 'package:scientific_calculator/theme/calc_theme.dart';

class CursorWidget extends StatefulWidget {
  const CursorWidget({super.key});

  @override
  State<CursorWidget> createState() => _CursorWidgetState();
}

class _CursorWidgetState extends State<CursorWidget>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _opacity;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    _opacity = Tween(begin: 1.0, end: 0.0).animate(_controller);
    _controller.repeat(reverse: true);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    return FadeTransition(
      opacity: _opacity,
      child: Container(width: 2, height: 20, color: ct.expressionText),
    );
  }
}
