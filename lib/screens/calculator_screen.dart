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
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // IntrinsicHeight caps the display at its natural content height.
            // Without it, Row(crossAxisAlignment: stretch) inside CalcDisplay
            // uses constraints.maxHeight (full screen) as the tight child height,
            // which makes CalcDisplay consume the entire screen and leave zero
            // height for ButtonGrid.
            IntrinsicHeight(
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
