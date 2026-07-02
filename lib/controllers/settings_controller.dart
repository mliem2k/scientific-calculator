import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../core/ast/types.dart';

enum CalcThemeId { amoled, dark, light, retro }

class SettingsState {
  const SettingsState({
    this.themeId = CalcThemeId.amoled,
    this.defaultAngleMode = AngleMode.deg,
    this.resultPrecision = 0,
    this.showShiftLabels = true,
    this.hapticFeedback = true,
  });

  final CalcThemeId themeId;
  final AngleMode defaultAngleMode;
  final int resultPrecision;
  final bool showShiftLabels;
  final bool hapticFeedback;

  SettingsState copyWith({
    CalcThemeId? themeId,
    AngleMode? defaultAngleMode,
    int? resultPrecision,
    bool? showShiftLabels,
    bool? hapticFeedback,
  }) =>
      SettingsState(
        themeId: themeId ?? this.themeId,
        defaultAngleMode: defaultAngleMode ?? this.defaultAngleMode,
        resultPrecision: resultPrecision ?? this.resultPrecision,
        showShiftLabels: showShiftLabels ?? this.showShiftLabels,
        hapticFeedback: hapticFeedback ?? this.hapticFeedback,
      );
}

class SettingsNotifier extends Notifier<SettingsState> {
  SharedPreferences? _prefs;

  @override
  SettingsState build() => const SettingsState();

  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _prefs = prefs;
    state = SettingsState(
      themeId:
          CalcThemeId.values.byName(prefs.getString('theme') ?? 'amoled'),
      defaultAngleMode:
          AngleMode.values.byName(prefs.getString('angleMode') ?? 'deg'),
      resultPrecision: prefs.getInt('resultPrecision') ?? 0,
      showShiftLabels: prefs.getBool('showShiftLabels') ?? true,
      hapticFeedback: prefs.getBool('hapticFeedback') ?? true,
    );
  }

  void setTheme(CalcThemeId id) {
    state = state.copyWith(themeId: id);
    _prefs?.setString('theme', id.name);
  }

  void setDefaultAngleMode(AngleMode mode) {
    state = state.copyWith(defaultAngleMode: mode);
    _prefs?.setString('angleMode', mode.name);
  }

  void setResultPrecision(int p) {
    state = state.copyWith(resultPrecision: p);
    _prefs?.setInt('resultPrecision', p);
  }

  void setShowShiftLabels(bool v) {
    state = state.copyWith(showShiftLabels: v);
    _prefs?.setBool('showShiftLabels', v);
  }

  void setHapticFeedback(bool v) {
    state = state.copyWith(hapticFeedback: v);
    _prefs?.setBool('hapticFeedback', v);
  }
}

final settingsProvider =
    NotifierProvider<SettingsNotifier, SettingsState>(SettingsNotifier.new);
