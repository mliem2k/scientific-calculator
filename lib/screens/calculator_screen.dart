import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../controllers/calculator_controller.dart';
import '../services/updater.dart';
import '../theme/calc_theme.dart';
import '../widgets/button_grid.dart';
import '../widgets/calc_display.dart';
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
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Update available: ${info.tagName}'),
        duration: const Duration(seconds: 10),
        action: SnackBarAction(
          label: 'Download',
          onPressed: () => launchUrl(
            Uri.parse(info.releaseUrl),
            mode: LaunchMode.externalApplication,
          ),
        ),
      ),
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
                (constraints.maxHeight * 0.20).clamp(160.0, 210.0);
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                SizedBox(
                  height: displayH,
                  child: CalcDisplay(
                    onSettings: () => _openSettings(context),
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
}
