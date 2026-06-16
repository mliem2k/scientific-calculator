import 'package:flutter_test/flutter_test.dart';
import 'package:scientific_calculator/core/ast/types.dart';
import 'package:scientific_calculator/core/evaluator.dart';

void main() {
  group('evaluate', () {
    test('returns null for empty expression', () {
      expect(evaluate([], AngleMode.rad, null, MemorySlots.empty()), isNull);
    });

    test('returns null for invalid expression (lone operator)', () {
      expect(
        evaluate([OperatorNode('+')], AngleMode.rad, null, MemorySlots.empty()),
        isNull,
      );
    });

    test('evaluates simple addition', () {
      final result = evaluate(
        [NumberNode('2'), OperatorNode('+'), NumberNode('3')],
        AngleMode.rad,
        null,
        MemorySlots.empty(),
      );
      expect(result, '5');
    });

    test('evaluates fraction 1/4 = 0.25', () {
      final nodes = [
        FractionNode([NumberNode('1')], [NumberNode('4')]),
      ];
      expect(
        evaluate(nodes, AngleMode.rad, null, MemorySlots.empty()),
        '0.25',
      );
    });

    test('evaluates sin(pi) ≈ 0 in RAD', () {
      final result = evaluate(
        [FunctionNode(FunctionName.sin, [ConstantNode('pi')])],
        AngleMode.rad,
        null,
        MemorySlots.empty(),
      );
      expect(double.parse(result!).abs(), lessThan(1e-10));
    });

    test('evaluates sin(90) = 1 in DEG', () {
      final result = evaluate(
        [FunctionNode(FunctionName.sin, [NumberNode('90')])],
        AngleMode.deg,
        null,
        MemorySlots.empty(),
      );
      expect(double.parse(result!), closeTo(1.0, 1e-10));
    });

    test('evaluates cos(0) = 1 in DEG', () {
      final result = evaluate(
        [FunctionNode(FunctionName.cos, [NumberNode('0')])],
        AngleMode.deg,
        null,
        MemorySlots.empty(),
      );
      expect(double.parse(result!), closeTo(1.0, 1e-10));
    });

    test('substitutes Ans', () {
      final result = evaluate(
        [ConstantNode('Ans'), OperatorNode('+'), NumberNode('1')],
        AngleMode.rad,
        '5',
        MemorySlots.empty(),
      );
      expect(result, '6');
    });

    test('uses 0 for Ans when lastResult is null', () {
      final result = evaluate(
        [ConstantNode('Ans'), OperatorNode('+'), NumberNode('3')],
        AngleMode.rad,
        null,
        MemorySlots.empty(),
      );
      expect(result, '3');
    });

    test('evaluates sqrt(4) = 2', () {
      final nodes = [
        RadicalNode([], [NumberNode('4')]),
      ];
      expect(evaluate(nodes, AngleMode.rad, null, MemorySlots.empty()), '2');
    });

    test('evaluates 2^10 = 1024', () {
      final nodes = [
        ExponentNode([NumberNode('2')], [NumberNode('10')]),
      ];
      expect(evaluate(nodes, AngleMode.rad, null, MemorySlots.empty()), '1024');
    });

    test('evaluates 5! = 120', () {
      expect(
        evaluate(
          [FactorialNode([NumberNode('5')])],
          AngleMode.rad,
          null,
          MemorySlots.empty(),
        ),
        '120',
      );
    });

    test('evaluates 5C2 = 10', () {
      final nodes = [
        NcrNode([NumberNode('5')], [NumberNode('2')]),
      ];
      expect(evaluate(nodes, AngleMode.rad, null, MemorySlots.empty()), '10');
    });

    test('evaluates log(100) = 2', () {
      expect(
        evaluate(
          [FunctionNode(FunctionName.log, [NumberNode('100')])],
          AngleMode.rad,
          null,
          MemorySlots.empty(),
        ),
        '2',
      );
    });

    test('evaluates ln(e) ≈ 1', () {
      final result = evaluate(
        [FunctionNode(FunctionName.ln, [ConstantNode('e')])],
        AngleMode.rad,
        null,
        MemorySlots.empty(),
      );
      expect(double.parse(result!), closeTo(1.0, 1e-10));
    });
  });

  group('evaluate with memory scope', () {
    test('substitutes memory variable A', () {
      final nodes = [ConstantNode('A')];
      final result = evaluate(
        nodes,
        AngleMode.deg,
        null,
        const MemorySlots(a: 7),
      );
      expect(result, '7');
    });

    test('defaults unset memory variable to 0', () {
      final nodes = [ConstantNode('B')];
      final result = evaluate(
        nodes,
        AngleMode.deg,
        null,
        MemorySlots.empty(),
      );
      expect(result, '0');
    });
  });

  group('toMixed', () {
    test('converts improper fraction to mixed', () {
      expect(toMixed('7/3'), '2 1/3');
    });

    test('returns null for a proper fraction', () {
      expect(toMixed('1/3'), isNull);
    });

    test('returns whole number string when fraction divides evenly', () {
      expect(toMixed('6/3'), '2');
    });

    test('returns null for non-fraction input', () {
      expect(toMixed('3.14'), isNull);
    });
  });

  group('fromMixed', () {
    test('converts mixed number to improper fraction', () {
      expect(fromMixed('2 1/3'), '7/3');
    });

    test('returns null for an improper fraction string', () {
      expect(fromMixed('7/3'), isNull);
    });
  });
}
