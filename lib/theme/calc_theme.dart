import 'package:flutter/material.dart';

class CalcTheme extends ThemeExtension<CalcTheme> {
  final Color background;
  final Color displayBg;
  final Color buttonBg;
  final Color buttonBorder;
  final Color digitText;
  final Color fnText;
  final Color opText;
  final Color eqBg;
  final Color eqText;
  final Color delText;
  final Color secondaryLabel;
  final Color expressionText;
  final Color resultText;
  final Color shiftActiveColor;
  final Color statusBadgeColor;

  const CalcTheme({
    required this.background,
    required this.displayBg,
    required this.buttonBg,
    required this.buttonBorder,
    required this.digitText,
    required this.fnText,
    required this.opText,
    required this.eqBg,
    required this.eqText,
    required this.delText,
    required this.secondaryLabel,
    required this.expressionText,
    required this.resultText,
    required this.shiftActiveColor,
    required this.statusBadgeColor,
  });

  @override
  CalcTheme copyWith({
    Color? background,
    Color? displayBg,
    Color? buttonBg,
    Color? buttonBorder,
    Color? digitText,
    Color? fnText,
    Color? opText,
    Color? eqBg,
    Color? eqText,
    Color? delText,
    Color? secondaryLabel,
    Color? expressionText,
    Color? resultText,
    Color? shiftActiveColor,
    Color? statusBadgeColor,
  }) {
    return CalcTheme(
      background: background ?? this.background,
      displayBg: displayBg ?? this.displayBg,
      buttonBg: buttonBg ?? this.buttonBg,
      buttonBorder: buttonBorder ?? this.buttonBorder,
      digitText: digitText ?? this.digitText,
      fnText: fnText ?? this.fnText,
      opText: opText ?? this.opText,
      eqBg: eqBg ?? this.eqBg,
      eqText: eqText ?? this.eqText,
      delText: delText ?? this.delText,
      secondaryLabel: secondaryLabel ?? this.secondaryLabel,
      expressionText: expressionText ?? this.expressionText,
      resultText: resultText ?? this.resultText,
      shiftActiveColor: shiftActiveColor ?? this.shiftActiveColor,
      statusBadgeColor: statusBadgeColor ?? this.statusBadgeColor,
    );
  }

  @override
  CalcTheme lerp(ThemeExtension<CalcTheme>? other, double t) {
    if (other is! CalcTheme) return this;
    return CalcTheme(
      background: Color.lerp(background, other.background, t)!,
      displayBg: Color.lerp(displayBg, other.displayBg, t)!,
      buttonBg: Color.lerp(buttonBg, other.buttonBg, t)!,
      buttonBorder: Color.lerp(buttonBorder, other.buttonBorder, t)!,
      digitText: Color.lerp(digitText, other.digitText, t)!,
      fnText: Color.lerp(fnText, other.fnText, t)!,
      opText: Color.lerp(opText, other.opText, t)!,
      eqBg: Color.lerp(eqBg, other.eqBg, t)!,
      eqText: Color.lerp(eqText, other.eqText, t)!,
      delText: Color.lerp(delText, other.delText, t)!,
      secondaryLabel: Color.lerp(secondaryLabel, other.secondaryLabel, t)!,
      expressionText: Color.lerp(expressionText, other.expressionText, t)!,
      resultText: Color.lerp(resultText, other.resultText, t)!,
      shiftActiveColor:
          Color.lerp(shiftActiveColor, other.shiftActiveColor, t)!,
      statusBadgeColor:
          Color.lerp(statusBadgeColor, other.statusBadgeColor, t)!,
    );
  }
}
