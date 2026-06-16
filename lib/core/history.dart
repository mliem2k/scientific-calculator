import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'ast/types.dart';

const _key = 'calc_history';
const _max = 50;

Future<List<HistoryEntry>> loadHistory() async {
  try {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_key);
    if (raw == null) return [];
    final list = jsonDecode(raw) as List<dynamic>;
    return list
        .map((e) => HistoryEntry.fromJson(e as Map<String, dynamic>))
        .toList();
  } catch (_) {
    return [];
  }
}

Future<List<HistoryEntry>> addEntry(
  List<HistoryEntry> entries,
  List<ASTNode> expression,
  String result,
) async {
  final entry = HistoryEntry(
    id: DateTime.now().millisecondsSinceEpoch.toString(),
    expression: expression,
    result: result,
    timestamp: DateTime.now().millisecondsSinceEpoch,
  );
  final next = [entry, ...entries].take(_max).toList();
  final prefs = await SharedPreferences.getInstance();
  await prefs.setString(_key, jsonEncode(next.map((e) => e.toJson()).toList()));
  return next;
}

Future<List<HistoryEntry>> clearHistory() async {
  final prefs = await SharedPreferences.getInstance();
  await prefs.remove(_key);
  return [];
}
