import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../controllers/calculator_controller.dart';
import '../core/ast/serializer.dart';
import '../core/ast/types.dart';
import '../theme/calc_theme.dart';

class HistoryPanel extends StatelessWidget {
  const HistoryPanel({super.key});

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final controller = context.read<CalculatorController>();

    return Selector<CalculatorController, List<HistoryEntry>>(
      selector: (_, c) => c.state.history,
      builder: (_, history, __) {
        if (history.isEmpty) {
          return Center(
            child: Text(
              'No history yet',
              style: TextStyle(color: ct.secondaryLabel),
            ),
          );
        }
        return ListView.builder(
          itemCount: history.length,
          itemBuilder: (ctx, index) {
            final entry = history[history.length - 1 - index];
            final exprText = serialize(entry.expression);
            return ListTile(
              title: Text(
                entry.result,
                style: TextStyle(color: ct.resultText),
              ),
              subtitle: Text(
                exprText,
                style: TextStyle(color: ct.secondaryLabel, fontSize: 15),
                overflow: TextOverflow.ellipsis,
              ),
              onTap: () => controller.handleRestore(entry),
            );
          },
        );
      },
    );
  }
}
