import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../controllers/calculator_controller.dart';
import '../theme/calc_theme.dart';
import '../widgets/button_grid.dart';
import '../widgets/calc_display.dart';
import 'settings_screen.dart';

class CalculatorScreen extends StatelessWidget {
  const CalculatorScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final controller = context.read<CalculatorController>();

    return Scaffold(
      backgroundColor: ct.background,
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            CalcDisplay(
              onSettings: () => _openSettings(context),
            ),
            Expanded(
              child: RepaintBoundary(
                child: ButtonGrid(
                  onButton: controller.handleButton,
                  onPaste: controller.handlePaste,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _openSettings(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => const SettingsScreen(),
      ),
    );
  }
}
