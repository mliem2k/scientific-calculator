import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../controllers/calculator_controller.dart';
import '../core/ast/serializer.dart';
import '../theme/calc_theme.dart';

class HistoryPanel extends ConsumerWidget {
  const HistoryPanel({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final history =
        ref.watch(calculatorProvider.select((s) => s.history));
    final notifier = ref.read(calculatorProvider.notifier);

    if (history.isEmpty) {
      return Center(
        child: Text('No history yet',
            style: TextStyle(color: ct.secondaryLabel)),
      );
    }
    return ListView.builder(
      itemCount: history.length,
      itemBuilder: (ctx, index) {
        final entry = history[history.length - 1 - index];
        final exprText = serialize(entry.expression);
        return ListTile(
          title: Text(entry.result,
              style: TextStyle(color: ct.resultText)),
          subtitle: Text(
            exprText,
            style: TextStyle(color: ct.secondaryLabel, fontSize: 15),
            overflow: TextOverflow.ellipsis,
          ),
          onTap: () => notifier.handleRestore(entry),
        );
      },
    );
  }
}
