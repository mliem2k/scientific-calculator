import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../controllers/calculator_controller.dart';
import '../services/updater.dart';
import '../theme/calc_theme.dart';
import '../widgets/button_grid.dart';
import '../widgets/calc_display.dart';
import '../widgets/history_panel.dart';
import '../widgets/update_dialog.dart';
import 'settings_screen.dart';

class CalculatorScreen extends ConsumerStatefulWidget {
  const CalculatorScreen({super.key});

  @override
  ConsumerState<CalculatorScreen> createState() => _CalculatorScreenState();
}

class _CalculatorScreenState extends ConsumerState<CalculatorScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _checkForUpdate());
  }

  Future<void> _checkForUpdate() async {
    final info = await checkForUpdate();
    if (info == null || !mounted) return;
    await showDialog<void>(
      context: context,
      barrierDismissible: true,
      builder: (ctx) => UpdateDialog(info: info),
    );
  }

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final notifier = ref.read(calculatorProvider.notifier);

    return Scaffold(
      backgroundColor: ct.background,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            final displayH =
                (constraints.maxHeight * 0.25).clamp(180.0, 240.0);
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                SizedBox(
                  height: displayH,
                  child: CalcDisplay(
                    onSettings: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => const SettingsScreen(),
                      ),
                    ),
                    onHistory: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => Scaffold(
                          appBar: AppBar(title: const Text('History')),
                          body: const HistoryPanel(),
                        ),
                      ),
                    ),
                  ),
                ),
                Expanded(
                  child: ButtonGrid(
                    onButton: notifier.handleButton,
                    onPaste: notifier.handlePaste,
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
