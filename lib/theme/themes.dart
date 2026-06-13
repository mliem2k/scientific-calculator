import 'package:flutter/material.dart';
import 'calc_theme.dart';
import '../controllers/settings_controller.dart';

const CalcTheme amoledCalcTheme = CalcTheme(
  background: Color(0xFF000000),
  displayBg: Color(0xFF000000),
  buttonBg: Color(0xFF000000),
  buttonBorder: Color(0xFF27272a),
  digitText: Color(0xFFffffff),
  fnText: Color(0xFFffffff),
  opText: Color(0xFFffffff),
  eqBg: Color(0xFFffffff),
  eqText: Color(0xFF000000),
  delText: Color(0xFFEF4444),
  secondaryLabel: Color(0xFF52525b),
  expressionText: Color(0xFFffffff),
  resultText: Color(0xFFa1a1aa),
  shiftActiveColor: Color(0xFFfb923c),
  statusBadgeColor: Color(0xFF818cf8),
);

const CalcTheme darkCalcTheme = CalcTheme(
  background: Color(0xFF121212),
  displayBg: Color(0xFF1a1a1a),
  buttonBg: Color(0xFF1E1E1E),
  buttonBorder: Color(0xFF2d2d2d),
  digitText: Color(0xFFffffff),
  fnText: Color(0xFFe0e0e0),
  opText: Color(0xFF81D4FA),
  eqBg: Color(0xFF448AFF),
  eqText: Color(0xFFffffff),
  delText: Color(0xFFFF5252),
  secondaryLabel: Color(0xFF9e9e9e),
  expressionText: Color(0xFFffffff),
  resultText: Color(0xFFbdbdbd),
  shiftActiveColor: Color(0xFFFFA726),
  statusBadgeColor: Color(0xFF7986CB),
);

const CalcTheme lightCalcTheme = CalcTheme(
  background: Color(0xFFF5F5F5),
  displayBg: Color(0xFFffffff),
  buttonBg: Color(0xFFFFFFFF),
  buttonBorder: Color(0xFFe0e0e0),
  digitText: Color(0xFF212121),
  fnText: Color(0xFF424242),
  opText: Color(0xFF1565C0),
  eqBg: Color(0xFF1565C0),
  eqText: Color(0xFFffffff),
  delText: Color(0xFFD32F2F),
  secondaryLabel: Color(0xFF757575),
  expressionText: Color(0xFF212121),
  resultText: Color(0xFF616161),
  shiftActiveColor: Color(0xFFE65100),
  statusBadgeColor: Color(0xFF5C6BC0),
);

const CalcTheme retroCalcTheme = CalcTheme(
  background: Color(0xFF0D0D0D),
  displayBg: Color(0xFF0D0D0D),
  buttonBg: Color(0xFF0D0D0D),
  buttonBorder: Color(0xFF003300),
  digitText: Color(0xFF00FF41),
  fnText: Color(0xFF00CC33),
  opText: Color(0xFF00FF41),
  eqBg: Color(0xFF00CC33),
  eqText: Color(0xFF000000),
  delText: Color(0xFFFF3300),
  secondaryLabel: Color(0xFF006600),
  expressionText: Color(0xFF00FF41),
  resultText: Color(0xFF00CC33),
  shiftActiveColor: Color(0xFFFFFF00),
  statusBadgeColor: Color(0xFFFF6600),
);

ThemeData themeDataFor(CalcTheme ct) {
  return ThemeData(
    scaffoldBackgroundColor: ct.background,
    appBarTheme: AppBarTheme(
      backgroundColor: ct.background,
      foregroundColor: ct.expressionText,
      elevation: 0,
    ),
    extensions: <ThemeExtension<dynamic>>[ct],
  );
}

final ThemeData amoledTheme = themeDataFor(amoledCalcTheme);
final ThemeData darkTheme = themeDataFor(darkCalcTheme);
final ThemeData lightTheme = themeDataFor(lightCalcTheme);
final ThemeData retroTheme = themeDataFor(retroCalcTheme);

ThemeData themeForId(CalcThemeId id) {
  switch (id) {
    case CalcThemeId.amoled:
      return amoledTheme;
    case CalcThemeId.dark:
      return darkTheme;
    case CalcThemeId.light:
      return lightTheme;
    case CalcThemeId.retro:
      return retroTheme;
  }
}
