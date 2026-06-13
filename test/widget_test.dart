import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:scientific_calculator/controllers/calculator_controller.dart';
import 'package:scientific_calculator/controllers/settings_controller.dart';
import 'package:scientific_calculator/theme/themes.dart';
import 'package:scientific_calculator/widgets/button_grid.dart';
import 'package:scientific_calculator/widgets/calc_display.dart';

Widget _wrapButtonGrid() => MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => CalculatorController()),
        ChangeNotifierProvider(create: (_) => SettingsController()),
      ],
      child: MaterialApp(
        theme: darkTheme,
        home: Scaffold(
          body: ButtonGrid(
            onButton: (_) {},
            onPaste: () async {},
          ),
        ),
      ),
    );

Widget _wrapCalcDisplay() => MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => CalculatorController()),
        ChangeNotifierProvider(create: (_) => SettingsController()),
      ],
      child: MaterialApp(
        theme: darkTheme,
        home: Scaffold(
          body: CalcDisplay(onSettings: () {}),
        ),
      ),
    );

void main() {
  group('button grid typography', () {
    testWidgets('primary label is 22px w700', (tester) async {
      await tester.pumpWidget(_wrapButtonGrid());
      await tester.pump();
      final text = tester.widget<Text>(find.text('7'));
      expect(text.style?.fontSize, 22.0);
      expect(text.style?.fontWeight, FontWeight.w700);
    });

    testWidgets('shift label is 12px w500', (tester) async {
      await tester.pumpWidget(_wrapButtonGrid());
      await tester.pump();
      // sin⁻¹( is the shift label of the sin button, visible when shiftActive=false
      final text = tester.widget<Text>(find.text('sin⁻¹('));
      expect(text.style?.fontSize, 12.0);
      expect(text.style?.fontWeight, FontWeight.w500);
    });
  });

  group('display typography', () {
    testWidgets('empty state expression placeholder is 40px', (tester) async {
      await tester.pumpWidget(_wrapCalcDisplay());
      await tester.pump();
      final text = tester.widget<Text>(find.text('0'));
      expect(text.style?.fontSize, 40.0);
    });

    testWidgets('settings icon is size 20', (tester) async {
      await tester.pumpWidget(_wrapCalcDisplay());
      await tester.pump();
      final icon = tester.widget<Icon>(find.byIcon(Icons.settings));
      expect(icon.size, 20.0);
    });

    testWidgets('SHIFT toggle is 28px w500 (inactive)', (tester) async {
      await tester.pumpWidget(_wrapCalcDisplay());
      await tester.pump();
      final text = tester.widget<Text>(find.text('≡'));
      expect(text.style?.fontSize, 28.0);
      expect(text.style?.fontWeight, FontWeight.w500);
    });

    // TODO: result text (34px w500) and copy button (20px) require non-null
    // calculator result to appear in the widget tree; tracked as follow-up.

    testWidgets('DEG label is 15px w700', (tester) async {
      await tester.pumpWidget(_wrapCalcDisplay());
      await tester.pump();
      final text = tester.widget<Text>(find.text('DEG'));
      expect(text.style?.fontSize, 15.0);
      expect(text.style?.fontWeight, FontWeight.w700);
    });
  });
}
