import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../core/ast/types.dart';

enum CalcThemeId { amoled, dark, light, retro }

class SettingsController extends ChangeNotifier {
  CalcThemeId themeId = CalcThemeId.amoled;
  AngleMode defaultAngleMode = AngleMode.deg;
  int resultPrecision = 0;
  bool showShiftLabels = true;
  bool hapticFeedback = true;

  late SharedPreferences _prefs;

  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
    themeId = CalcThemeId.values.byName(_prefs.getString('theme') ?? 'amoled');
    defaultAngleMode =
        AngleMode.values.byName(_prefs.getString('angleMode') ?? 'deg');
    resultPrecision = _prefs.getInt('resultPrecision') ?? 0;
    showShiftLabels = _prefs.getBool('showShiftLabels') ?? true;
    hapticFeedback = _prefs.getBool('hapticFeedback') ?? true;
  }

  void setTheme(CalcThemeId id) {
    themeId = id;
    _prefs.setString('theme', id.name);
    notifyListeners();
  }

  void setDefaultAngleMode(AngleMode mode) {
    defaultAngleMode = mode;
    _prefs.setString('angleMode', mode.name);
    notifyListeners();
  }

  void setResultPrecision(int p) {
    resultPrecision = p;
    _prefs.setInt('resultPrecision', p);
    notifyListeners();
  }

  void setShowShiftLabels(bool v) {
    showShiftLabels = v;
    _prefs.setBool('showShiftLabels', v);
    notifyListeners();
  }

  void setHapticFeedback(bool v) {
    hapticFeedback = v;
    _prefs.setBool('hapticFeedback', v);
    notifyListeners();
  }
}
