import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'controllers/settings_controller.dart';
import 'screens/calculator_screen.dart';
import 'theme/themes.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  final container = ProviderContainer();
  await container.read(settingsProvider.notifier).init();
  runApp(
    UncontrolledProviderScope(
      container: container,
      child: const CalcApp(),
    ),
  );
}

class CalcApp extends ConsumerWidget {
  const CalcApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeId = ref.watch(settingsProvider.select((s) => s.themeId));
    return MaterialApp(
      title: 'Scientific Calculator',
      debugShowCheckedModeBanner: false,
      theme: themeForId(themeId),
      home: const CalculatorScreen(),
    );
  }
}
