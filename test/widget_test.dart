import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:scientific_calculator/controllers/calculator_controller.dart';
import 'package:scientific_calculator/controllers/settings_controller.dart';
import 'package:scientific_calculator/core/ast/types.dart';
import 'package:scientific_calculator/theme/themes.dart';
import 'package:scientific_calculator/widgets/ast/ast_renderer.dart';
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
          body: CalcDisplay(
            onSettings: () {},
            onHistory: () {},
          ),
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

  group('display header', () {
    testWidgets('DEG badge is visible by default', (tester) async {
      await tester.pumpWidget(_wrapCalcDisplay());
      await tester.pump();
      expect(find.text('DEG'), findsOneWidget);
    });

    testWidgets('DEG badge tap toggles angle mode to RAD', (tester) async {
      final controller = CalculatorController();
      await tester.pumpWidget(MultiProvider(
        providers: [
          ChangeNotifierProvider.value(value: controller),
          ChangeNotifierProvider(create: (_) => SettingsController()),
        ],
        child: MaterialApp(
          theme: darkTheme,
          home: Scaffold(
            body: CalcDisplay(onSettings: () {}, onHistory: () {}),
          ),
        ),
      ));
      await tester.pump();
      expect(controller.state.angleMode, AngleMode.deg);
      await tester.tap(find.text('DEG'));
      await tester.pump();
      expect(controller.state.angleMode, AngleMode.rad);
    });

    testWidgets('history icon is present', (tester) async {
      await tester.pumpWidget(_wrapCalcDisplay());
      await tester.pump();
      expect(find.byIcon(Icons.history), findsOneWidget);
    });

    testWidgets('SHIFT toggle symbol ⇧ is not in the display', (tester) async {
      await tester.pumpWidget(_wrapCalcDisplay());
      await tester.pump();
      expect(find.text('⇧'), findsNothing);
    });
  });

  group('ast renderer', () {
    testWidgets('rowKey is attached to root Row after pump', (tester) async {
      final key = GlobalKey();
      await tester.pumpWidget(
        MaterialApp(
          theme: darkTheme,
          home: Scaffold(
            body: ASTRenderer(
              rowKey: key,
              nodes: [NumberNode('1'), NumberNode('2')],
              cursor: const Cursor([], 2),
              path: const [],
              onCursorJump: (_, __) {},
            ),
          ),
        ),
      );
      expect(key.currentContext, isNotNull);
      expect(key.currentContext!.findRenderObject(), isA<RenderBox>());
    });
  });

  group('long-press drag cursor', () {
    testWidgets('long-press on expression moves cursor from end', (tester) async {
      final controller = CalculatorController();
      await tester.pumpWidget(
        MultiProvider(
          providers: [
            ChangeNotifierProvider.value(value: controller),
            ChangeNotifierProvider(create: (_) => SettingsController()),
          ],
          child: MaterialApp(
            theme: darkTheme,
            home: Scaffold(
              body: SizedBox(
                width: 400,
                height: 150,
                child: CalcDisplay(onSettings: () {}, onHistory: () {}),
              ),
            ),
          ),
        ),
      );

      // Build a 3-node expression: "1 + 2"
      controller.handleButton('1');
      controller.handleButton('plus');
      controller.handleButton('2');
      await tester.pump();

      // Cursor starts at end of expression
      expect(controller.state.cursor.path, isEmpty);
      expect(controller.state.cursor.insertAt, 3);

      // Long-press the far-left edge of the expression scroll view
      final scrollView = find.byType(SingleChildScrollView).first;
      final rect = tester.getRect(scrollView);
      final target = Offset(rect.left + rect.width * 0.05, rect.center.dy);

      final gesture = await tester.startGesture(target);
      await tester.pump(const Duration(milliseconds: 600));
      await tester.pump();
      await gesture.up();
      await tester.pump();

      // Cursor moved earlier in the expression
      expect(controller.state.cursor.path, isEmpty);
      expect(controller.state.cursor.insertAt, lessThan(3));
    });
  });

  group('control row', () {
    testWidgets('SHIFT, HYP, π, e, Ans buttons are all present', (tester) async {
      await tester.pumpWidget(_wrapButtonGrid());
      await tester.pump();
      expect(find.text('SHIFT'), findsOneWidget);
      expect(find.text('HYP'), findsOneWidget);
      expect(find.text('π'), findsOneWidget);
      expect(find.text('e'), findsOneWidget);
      expect(find.text('Ans'), findsOneWidget);
    });

    testWidgets('tapping SHIFT fires SHIFT action', (tester) async {
      final fired = <String>[];
      await tester.pumpWidget(MultiProvider(
        providers: [
          ChangeNotifierProvider(create: (_) => CalculatorController()),
          ChangeNotifierProvider(create: (_) => SettingsController()),
        ],
        child: MaterialApp(
          theme: darkTheme,
          home: Scaffold(
            body: ButtonGrid(onButton: fired.add, onPaste: () async {}),
          ),
        ),
      ));
      await tester.pump();
      await tester.tap(find.text('SHIFT'));
      await tester.pump();
      expect(fired, contains('SHIFT'));
    });

    testWidgets('tapping π fires pi action', (tester) async {
      final fired = <String>[];
      await tester.pumpWidget(MultiProvider(
        providers: [
          ChangeNotifierProvider(create: (_) => CalculatorController()),
          ChangeNotifierProvider(create: (_) => SettingsController()),
        ],
        child: MaterialApp(
          theme: darkTheme,
          home: Scaffold(
            body: ButtonGrid(onButton: fired.add, onPaste: () async {}),
          ),
        ),
      ));
      await tester.pump();
      await tester.tap(find.text('π'));
      await tester.pump();
      expect(fired, contains('pi'));
    });
  });

  group('bottom row', () {
    testWidgets('= is to the right of (−)', (tester) async {
      await tester.pumpWidget(_wrapButtonGrid());
      await tester.pump();
      final eqPos = tester.getCenter(find.text('='));
      final negatePos = tester.getCenter(find.text('(−)'));
      expect(eqPos.dx, greaterThan(negatePos.dx));
    });

    testWidgets('EXP is not a 22px primary button label', (tester) async {
      await tester.pumpWidget(_wrapButtonGrid());
      await tester.pump();
      // When shift is inactive, EXP only appears as a 12px corner shift label.
      // If no Text('EXP') is present at all, the test also passes.
      final expWidgets = tester.widgetList<Text>(find.text('EXP')).toList();
      for (final t in expWidgets) {
        expect(t.style?.fontSize, isNot(22.0));
      }
    });
  });

  group('shift action mappings', () {
    Widget buildTrackedGrid(CalculatorController controller, List<String> fired) {
      return MultiProvider(
        providers: [
          ChangeNotifierProvider.value(value: controller),
          ChangeNotifierProvider(create: (_) => SettingsController()),
        ],
        child: MaterialApp(
          theme: darkTheme,
          home: Scaffold(
            body: ButtonGrid(
              onButton: (id) {
                fired.add(id);
                controller.handleButton(id);
              },
              onPaste: () async {},
            ),
          ),
        ),
      );
    }

    testWidgets('SHIFT + e fires exp (eˣ)', (tester) async {
      final controller = CalculatorController();
      final fired = <String>[];
      await tester.pumpWidget(buildTrackedGrid(controller, fired));
      await tester.pump();
      await tester.tap(find.text('SHIFT'));
      await tester.pump();
      expect(controller.state.shiftActive, isTrue);
      // When shiftActive, 'e' button shows 'eˣ' as its primary label
      await tester.tap(find.text('eˣ'));
      await tester.pump();
      expect(fired, contains('exp'));
    });

    testWidgets('SHIFT + . fires EXP', (tester) async {
      final controller = CalculatorController();
      final fired = <String>[];
      await tester.pumpWidget(buildTrackedGrid(controller, fired));
      await tester.pump();
      await tester.tap(find.text('SHIFT'));
      await tester.pump();
      expect(controller.state.shiftActive, isTrue);
      // When shiftActive, '.' button shows 'EXP' as its primary label
      await tester.tap(find.text('EXP'));
      await tester.pump();
      expect(fired, contains('EXP'));
    });
  });
}

