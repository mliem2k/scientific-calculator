import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'controllers/calculator_controller.dart';
import 'controllers/settings_controller.dart';
import 'screens/calculator_screen.dart';
import 'theme/themes.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  final settings = SettingsController();
  await settings.init();
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: settings),
        ChangeNotifierProvider(create: (_) => CalculatorController()),
      ],
      child: const CalcApp(),
    ),
  );
}

class CalcApp extends StatelessWidget {
  const CalcApp({super.key});

  @override
  Widget build(BuildContext context) {
    final themeId = context.watch<SettingsController>().themeId;
    return MaterialApp(
      title: 'Scientific Calculator',
      debugShowCheckedModeBanner: false,
      theme: themeForId(themeId),
      home: const CalculatorScreen(),
    );
  }
}
