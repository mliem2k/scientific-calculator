import 'package:flutter_test/flutter_test.dart';
import 'package:scientific_calculator/core/ast/types.dart';
import 'package:scientific_calculator/core/ast/cursor.dart';

Cursor c(int insertAt, [List<CursorSegment> path = const []]) =>
    Cursor(path, insertAt);

void main() {
  final fraction = [
    FractionNode(
      [NumberNode('1')],
      [NumberNode('2')],
    ),
  ];
  final exprAB = [NumberNode('A'), NumberNode('B')];

  group('moveCursorRight', () {
    test('moves right in flat list', () {
      final result = moveCursorRight(exprAB, c(0));
      expect(result.insertAt, 1);
      expect(result.path, isEmpty);
    });

    test('entering fraction from left → numerator start', () {
      final result = moveCursorRight(fraction, c(0));
      expect(result.path.length, 1);
      expect(result.path[0].nodeIndex, 0);
      expect(result.path[0].slot, 'numerator');
      expect(result.insertAt, 0);
    });

    test('numerator end → denominator start', () {
      final cur = c(1, [const CursorSegment(0, 'numerator')]);
      final result = moveCursorRight(fraction, cur);
      expect(result.path.length, 1);
      expect(result.path[0].nodeIndex, 0);
      expect(result.path[0].slot, 'denominator');
      expect(result.insertAt, 0);
    });

    test('denominator end → exits fraction to right', () {
      final cur = c(1, [const CursorSegment(0, 'denominator')]);
      final result = moveCursorRight(fraction, cur);
      expect(result.insertAt, 1);
      expect(result.path, isEmpty);
    });

    test('stays at root end', () {
      final result = moveCursorRight(exprAB, c(2));
      expect(result.insertAt, 2);
      expect(result.path, isEmpty);
    });
  });

  group('moveCursorLeft', () {
    test('moves left in flat list', () {
      final result = moveCursorLeft(exprAB, c(2));
      expect(result.insertAt, 1);
      expect(result.path, isEmpty);
    });

    test('denominator start → numerator end', () {
      final cur = c(0, [const CursorSegment(0, 'denominator')]);
      final result = moveCursorLeft(fraction, cur);
      expect(result.path.length, 1);
      expect(result.path[0].nodeIndex, 0);
      expect(result.path[0].slot, 'numerator');
      expect(result.insertAt, 1);
    });

    test('numerator start → exits to before fraction', () {
      final cur = c(0, [const CursorSegment(0, 'numerator')]);
      final result = moveCursorLeft(fraction, cur);
      expect(result.insertAt, 0);
      expect(result.path, isEmpty);
    });

    test('stays at root start', () {
      final result = moveCursorLeft(exprAB, c(0));
      expect(result.insertAt, 0);
      expect(result.path, isEmpty);
    });
  });

  group('moveCursorUp', () {
    test('denominator → numerator', () {
      final cur = c(0, [const CursorSegment(0, 'denominator')]);
      final result = moveCursorUp(fraction, cur);
      expect(result.path[result.path.length - 1].slot, 'numerator');
    });

    test('does nothing at root', () {
      final result = moveCursorUp(fraction, c(0));
      expect(result.insertAt, 0);
      expect(result.path, isEmpty);
    });

    test('does nothing in numerator already', () {
      final cur = c(0, [const CursorSegment(0, 'numerator')]);
      final result = moveCursorUp(fraction, cur);
      expect(result.path.length, 1);
      expect(result.path[0].slot, 'numerator');
      expect(result.insertAt, 0);
    });
  });

  group('moveCursorDown', () {
    test('numerator → denominator', () {
      final cur = c(0, [const CursorSegment(0, 'numerator')]);
      final result = moveCursorDown(fraction, cur);
      expect(result.path[result.path.length - 1].slot, 'denominator');
    });

    test('does nothing at root', () {
      final result = moveCursorDown(fraction, c(0));
      expect(result.insertAt, 0);
      expect(result.path, isEmpty);
    });
  });
}
