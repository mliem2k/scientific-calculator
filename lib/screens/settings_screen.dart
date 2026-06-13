import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../controllers/settings_controller.dart';
import '../core/ast/types.dart';
import '../services/updater.dart';
import '../theme/calc_theme.dart';
import '../theme/themes.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _checking = false;

  Future<void> _checkForUpdate() async {
    setState(() => _checking = true);
    final info = await checkForUpdate();
    if (!mounted) return;
    setState(() => _checking = false);
    if (info == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('App is up to date.')),
      );
    } else {
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
  }

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final settings = context.watch<SettingsController>();

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
          _ThemePickerGrid(settings: settings, ct: ct),
          _SectionHeader(label: 'CALCULATOR', ct: ct),
          ListTile(
            tileColor: ct.buttonBg,
            textColor: ct.expressionText,
            iconColor: ct.expressionText,
            title: const Text('Default angle mode'),
            trailing: SegmentedButton<AngleMode>(
              style: ButtonStyle(
                backgroundColor: WidgetStateProperty.resolveWith((states) {
                  if (states.contains(WidgetState.selected)) {
                    return ct.shiftActiveColor;
                  }
                  return ct.buttonBg;
                }),
                foregroundColor: WidgetStateProperty.resolveWith((states) {
                  if (states.contains(WidgetState.selected)) {
                    return ct.eqText;
                  }
                  return ct.expressionText;
                }),
                side: WidgetStateProperty.all(
                  BorderSide(color: ct.buttonBorder),
                ),
              ),
              segments: const [
                ButtonSegment<AngleMode>(
                  value: AngleMode.deg,
                  label: Text('DEG'),
                ),
                ButtonSegment<AngleMode>(
                  value: AngleMode.rad,
                  label: Text('RAD'),
                ),
              ],
              selected: {settings.defaultAngleMode},
              onSelectionChanged: (selection) {
                settings.setDefaultAngleMode(selection.first);
              },
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
              onChanged: (value) {
                if (value != null) settings.setResultPrecision(value);
              },
            ),
          ),
          _SectionHeader(label: 'DISPLAY', ct: ct),
          SwitchListTile(
            tileColor: ct.buttonBg,
            title: Text(
              'Show shift labels',
              style: TextStyle(color: ct.expressionText),
            ),
            subtitle: Text(
              'Display secondary function labels on buttons',
              style: TextStyle(color: ct.resultText),
            ),
            activeThumbColor: ct.shiftActiveColor,
            value: settings.showShiftLabels,
            onChanged: settings.setShowShiftLabels,
          ),
          _SectionHeader(label: 'BEHAVIOR', ct: ct),
          SwitchListTile(
            tileColor: ct.buttonBg,
            title: Text(
              'Haptic feedback',
              style: TextStyle(color: ct.expressionText),
            ),
            subtitle: Text(
              'Vibrate on button press',
              style: TextStyle(color: ct.resultText),
            ),
            activeThumbColor: ct.shiftActiveColor,
            value: settings.hapticFeedback,
            onChanged: settings.setHapticFeedback,
          ),
          _SectionHeader(label: 'ABOUT', ct: ct),
          ListTile(
            tileColor: ct.buttonBg,
            textColor: ct.expressionText,
            iconColor: ct.expressionText,
            title: const Text('Check for updates'),
            subtitle: Text(
              'Compare with latest GitHub release',
              style: TextStyle(color: ct.resultText),
            ),
            trailing: _checking
                ? SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: ct.shiftActiveColor,
                    ),
                  )
                : Icon(Icons.system_update_outlined, color: ct.expressionText),
            onTap: _checking ? null : _checkForUpdate,
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
          fontSize: 11,
          fontWeight: FontWeight.w600,
          color: ct.opText,
          letterSpacing: 1.2,
        ),
      ),
    );
  }
}

class _ThemePickerGrid extends StatelessWidget {
  const _ThemePickerGrid({required this.settings, required this.ct});

  final SettingsController settings;
  final CalcTheme ct;

  CalcTheme _themeForPreview(CalcThemeId id) {
    switch (id) {
      case CalcThemeId.amoled:
        return amoledCalcTheme;
      case CalcThemeId.dark:
        return darkCalcTheme;
      case CalcThemeId.light:
        return lightCalcTheme;
      case CalcThemeId.retro:
        return retroCalcTheme;
    }
  }

  String _nameForId(CalcThemeId id) {
    switch (id) {
      case CalcThemeId.amoled:
        return 'AMOLED';
      case CalcThemeId.dark:
        return 'Dark';
      case CalcThemeId.light:
        return 'Light';
      case CalcThemeId.retro:
        return 'Retro';
    }
  }

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
          final isActive = settings.themeId == id;
          return _ThemeCard(
            themeId: id,
            name: _nameForId(id),
            preview: preview,
            isActive: isActive,
            onTap: () => settings.setTheme(id),
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
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        _SwatchBox(color: preview.buttonBg, border: preview.buttonBorder),
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
                  child: Icon(
                    Icons.check_circle,
                    size: 18,
                    color: preview.shiftActiveColor,
                  ),
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
        border: border != null
            ? Border.all(color: border!, width: 1)
            : null,
      ),
    );
  }
}
