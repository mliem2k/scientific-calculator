import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';
import '../controllers/settings_controller.dart';
import '../core/ast/types.dart';
import '../services/updater.dart';
import '../theme/calc_theme.dart';
import '../theme/themes.dart';
import '../widgets/update_dialog.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  bool _checking = false;

  Future<void> _checkForUpdate() async {
    setState(() => _checking = true);
    final info = await checkForUpdate();
    if (!mounted) return;
    setState(() => _checking = false);
    if (info == null) {
      await showDialog<void>(
        context: context,
        builder: (_) => const UpToDateDialog(),
      );
    } else {
      await showDialog<void>(
        context: context,
        builder: (_) => UpdateDialog(info: info),
      );
    }
  }

  String _buildVersionString() {
    if (kBuildNumber == 0) return 'Dev build';
    final dt = DateTime.fromMillisecondsSinceEpoch(kBuildNumber * 1000);
    final y = dt.year.toString().padLeft(4, '0');
    final m = dt.month.toString().padLeft(2, '0');
    final d = dt.day.toString().padLeft(2, '0');
    return 'Build $y-$m-$d';
  }

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final settings = ref.watch(settingsProvider);
    final notifier = ref.read(settingsProvider.notifier);

    return Scaffold(
      backgroundColor: ct.background,
      appBar: AppBar(
        backgroundColor: ct.background,
        foregroundColor: ct.expressionText,
        elevation: 0,
        title: const Text('Settings'),
      ),
      body: ListView(
        children: [
          _SectionHeader(label: 'APPEARANCE', ct: ct),
          _ThemePickerGrid(
            settings: settings,
            ct: ct,
            onSetTheme: notifier.setTheme,
          ),
          _SectionHeader(label: 'CALCULATOR', ct: ct),
          ListTile(
            tileColor: ct.buttonBg,
            textColor: ct.expressionText,
            iconColor: ct.expressionText,
            title: const Text('Default angle mode'),
            trailing: SegmentedButton<AngleMode>(
              style: ButtonStyle(
                backgroundColor: WidgetStateProperty.resolveWith((states) =>
                    states.contains(WidgetState.selected)
                        ? ct.shiftActiveColor
                        : ct.buttonBg),
                foregroundColor: WidgetStateProperty.resolveWith((states) =>
                    states.contains(WidgetState.selected)
                        ? ct.eqText
                        : ct.expressionText),
                side: WidgetStateProperty.all(
                    BorderSide(color: ct.buttonBorder)),
              ),
              segments: const [
                ButtonSegment<AngleMode>(
                    value: AngleMode.deg, label: Text('DEG')),
                ButtonSegment<AngleMode>(
                    value: AngleMode.rad, label: Text('RAD')),
              ],
              selected: {settings.defaultAngleMode},
              onSelectionChanged: (s) =>
                  notifier.setDefaultAngleMode(s.first),
            ),
          ),
          ListTile(
            tileColor: ct.buttonBg,
            textColor: ct.expressionText,
            iconColor: ct.expressionText,
            title: const Text('Result precision'),
            trailing: DropdownButton<int>(
              value: settings.resultPrecision,
              dropdownColor: ct.buttonBg,
              style: TextStyle(color: ct.expressionText),
              underline: const SizedBox.shrink(),
              items: const [
                DropdownMenuItem(value: 0, child: Text('Auto')),
                DropdownMenuItem(value: 4, child: Text('4 dec')),
                DropdownMenuItem(value: 6, child: Text('6 dec')),
                DropdownMenuItem(value: 8, child: Text('8 dec')),
                DropdownMenuItem(value: 10, child: Text('10 dec')),
              ],
              onChanged: (v) {
                if (v != null) notifier.setResultPrecision(v);
              },
            ),
          ),
          _SectionHeader(label: 'DISPLAY', ct: ct),
          SwitchListTile(
            tileColor: ct.buttonBg,
            title: Text('Show shift labels',
                style: TextStyle(color: ct.expressionText)),
            subtitle: Text(
              'Display secondary function labels on buttons',
              style: TextStyle(color: ct.resultText),
            ),
            activeThumbColor: ct.shiftActiveColor,
            value: settings.showShiftLabels,
            onChanged: notifier.setShowShiftLabels,
          ),
          _SectionHeader(label: 'BEHAVIOR', ct: ct),
          SwitchListTile(
            tileColor: ct.buttonBg,
            title: Text('Haptic feedback',
                style: TextStyle(color: ct.expressionText)),
            subtitle: Text('Vibrate on button press',
                style: TextStyle(color: ct.resultText)),
            activeThumbColor: ct.shiftActiveColor,
            value: settings.hapticFeedback,
            onChanged: notifier.setHapticFeedback,
          ),
          _SectionHeader(label: 'ABOUT', ct: ct),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: Image.asset('assets/icon.png',
                      width: 56, height: 56, fit: BoxFit.cover),
                ),
                const SizedBox(width: 14),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Scientific Calculator',
                      style: TextStyle(
                          color: ct.expressionText,
                          fontSize: 16,
                          fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 2),
                    Text(_buildVersionString(),
                        style:
                            TextStyle(color: ct.secondaryLabel, fontSize: 13)),
                  ],
                ),
              ],
            ),
          ),
          ListTile(
            tileColor: ct.buttonBg,
            textColor: ct.expressionText,
            iconColor: ct.expressionText,
            leading: const Icon(Icons.system_update_outlined),
            title: const Text('Check for updates'),
            subtitle: Text('Compare with latest GitHub release',
                style: TextStyle(color: ct.resultText)),
            trailing: _checking
                ? SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                        strokeWidth: 2, color: ct.shiftActiveColor))
                : null,
            onTap: _checking ? null : _checkForUpdate,
          ),
          ListTile(
            tileColor: ct.buttonBg,
            textColor: ct.expressionText,
            iconColor: ct.expressionText,
            leading: const Icon(Icons.code_outlined),
            title: const Text('Source code'),
            subtitle: Text('github.com/mliem2k/scientific-calculator',
                style: TextStyle(color: ct.resultText)),
            trailing:
                Icon(Icons.open_in_new, size: 16, color: ct.secondaryLabel),
            onTap: () => launchUrl(
              Uri.parse('https://github.com/mliem2k/scientific-calculator'),
              mode: LaunchMode.externalApplication,
            ),
          ),
          ListTile(
            tileColor: ct.buttonBg,
            textColor: ct.expressionText,
            iconColor: ct.expressionText,
            leading: const Icon(Icons.article_outlined),
            title: const Text('Open source licenses'),
            onTap: () => showLicensePage(
              context: context,
              applicationName: 'Scientific Calculator',
              applicationVersion: _buildVersionString(),
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.label, required this.ct});

  final String label;
  final CalcTheme ct;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: ct.opText,
          letterSpacing: 1.2,
        ),
      ),
    );
  }
}

class _ThemePickerGrid extends StatelessWidget {
  const _ThemePickerGrid({
    required this.settings,
    required this.ct,
    required this.onSetTheme,
  });

  final SettingsState settings;
  final CalcTheme ct;
  final void Function(CalcThemeId) onSetTheme;

  CalcTheme _themeForPreview(CalcThemeId id) => switch (id) {
        CalcThemeId.amoled => amoledCalcTheme,
        CalcThemeId.dark => darkCalcTheme,
        CalcThemeId.light => lightCalcTheme,
        CalcThemeId.retro => retroCalcTheme,
      };

  String _nameForId(CalcThemeId id) => switch (id) {
        CalcThemeId.amoled => 'AMOLED',
        CalcThemeId.dark => 'Dark',
        CalcThemeId.light => 'Light',
        CalcThemeId.retro => 'Retro',
      };

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: GridView.count(
        crossAxisCount: 2,
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        mainAxisSpacing: 8,
        crossAxisSpacing: 8,
        childAspectRatio: 2.2,
        children: CalcThemeId.values.map((id) {
          final preview = _themeForPreview(id);
          return _ThemeCard(
            themeId: id,
            name: _nameForId(id),
            preview: preview,
            isActive: settings.themeId == id,
            onTap: () => onSetTheme(id),
          );
        }).toList(),
      ),
    );
  }
}

class _ThemeCard extends StatelessWidget {
  const _ThemeCard({
    required this.themeId,
    required this.name,
    required this.preview,
    required this.isActive,
    required this.onTap,
  });

  final CalcThemeId themeId;
  final String name;
  final CalcTheme preview;
  final bool isActive;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(8),
      child: Material(
        color: preview.background,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(8),
          child: Stack(
            children: [
              Padding(
                padding: const EdgeInsets.all(10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      name,
                      style: TextStyle(
                        color: preview.expressionText,
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        _SwatchBox(
                            color: preview.buttonBg,
                            border: preview.buttonBorder),
                        const SizedBox(width: 4),
                        _SwatchBox(color: preview.opText),
                        const SizedBox(width: 4),
                        _SwatchBox(color: preview.eqBg),
                      ],
                    ),
                  ],
                ),
              ),
              if (isActive)
                Positioned(
                  top: 6,
                  right: 6,
                  child: Icon(Icons.check_circle,
                      size: 18, color: preview.shiftActiveColor),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SwatchBox extends StatelessWidget {
  const _SwatchBox({required this.color, this.border});

  final Color color;
  final Color? border;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 16,
      height: 16,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(3),
        border:
            border != null ? Border.all(color: border!, width: 1) : null,
      ),
    );
  }
}
