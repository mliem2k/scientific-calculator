import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../controllers/calculator_controller.dart';
import '../services/updater.dart';
import '../theme/calc_theme.dart';
import '../widgets/button_grid.dart';
import '../widgets/calc_display.dart';
import '../widgets/history_panel.dart';
import '../widgets/update_dialog.dart';
import 'settings_screen.dart';

class CalculatorScreen extends StatefulWidget {
  const CalculatorScreen({super.key});

  @override
  State<CalculatorScreen> createState() => _CalculatorScreenState();
}

class _CalculatorScreenState extends State<CalculatorScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _checkForUpdate());
  }

  Future<void> _checkForUpdate() async {
    final info = await checkForUpdate();
    if (info == null || !mounted) return;
    _showUpdateDialog(info);
  }

  Future<void> _showUpdateDialog(UpdateInfo info) async {
    await showDialog<void>(
      context: context,
      barrierDismissible: true,
      builder: (ctx) => UpdateDialog(info: info),
    );
  }

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final controller = context.read<CalculatorController>();

    return Scaffold(
      backgroundColor: ct.background,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            // Fix display height so ButtonGrid's Expanded always gets the same
            // remaining height regardless of whether a result is shown.
            final displayH =
                (constraints.maxHeight * 0.25).clamp(180.0, 240.0);
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                SizedBox(
                  height: displayH,
                  child: CalcDisplay(
                    onSettings: () => _openSettings(context),
                    onHistory: () => _openHistory(context),
                  ),
                ),
                Expanded(
                  child: ButtonGrid(
                    onButton: controller.handleButton,
                    onPaste: controller.handlePaste,
                  ),
                ),
              ],
            );
          },
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

  void _openHistory(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => Scaffold(
          appBar: AppBar(title: const Text('History')),
          body: const HistoryPanel(),
        ),
      ),
    );
  }
}
