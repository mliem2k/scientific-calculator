import 'package:flutter_test/flutter_test.dart';
import 'package:scientific_calculator/core/ast/types.dart';
import 'package:scientific_calculator/core/ast/serializer.dart';

void main() {
  group('serialize', () {
    test('number', () {
      expect(serialize([NumberNode('42')]), '42');
    });

    test('+ operator', () {
      expect(serialize([OperatorNode('+')]), '+');
    });

    test('× operator maps to *', () {
      expect(serialize([OperatorNode('×')]), '*');
    });

    test('÷ operator maps to /', () {
      expect(serialize([OperatorNode('÷')]), '/');
    });

    test('pi constant', () {
      expect(serialize([ConstantNode('pi')]), 'pi');
    });

    test('e constant', () {
      expect(serialize([ConstantNode('e')]), 'e');
    });

    test('Ans constant', () {
      expect(serialize([ConstantNode('Ans')]), 'ans');
    });

    test('fraction', () {
      final nodes = [
        FractionNode([NumberNode('1')], [NumberNode('2')]),
      ];
      expect(serialize(nodes), '(1)/(2)');
    });

    test('exponent', () {
      final nodes = [
        ExponentNode([NumberNode('2')], [NumberNode('3')]),
      ];
      expect(serialize(nodes), '(2)^(3)');
    });

    test('sqrt radical (empty degree)', () {
      final nodes = [
        RadicalNode([], [NumberNode('4')]),
      ];
      expect(serialize(nodes), 'sqrt(4)');
    });

    test('nth root radical', () {
      final nodes = [
        RadicalNode([NumberNode('3')], [NumberNode('8')]),
      ];
      expect(serialize(nodes), 'nthRoot(8, 3)');
    });

    test('sin function', () {
      final nodes = [
        FunctionNode(FunctionName.sin, [ConstantNode('pi')]),
      ];
      expect(serialize(nodes), 'sin(pi)');
    });

    test('log maps to log base 10', () {
      final nodes = [
        FunctionNode(FunctionName.log, [NumberNode('100')]),
      ];
      expect(serialize(nodes), 'log(100, 10)');
    });

    test('ln maps to natural log', () {
      final nodes = [
        FunctionNode(FunctionName.ln, [NumberNode('1')]),
      ];
      expect(serialize(nodes), 'log(1)');
    });

    test('pow10', () {
      final nodes = [
        FunctionNode(FunctionName.pow10, [NumberNode('2')]),
      ];
      expect(serialize(nodes), '10^(2)');
    });

    test('exp (e^x)', () {
      final nodes = [
        FunctionNode(FunctionName.exp, [NumberNode('1')]),
      ];
      expect(serialize(nodes), 'e^(1)');
    });

    test('factorial', () {
      final nodes = [
        FactorialNode([NumberNode('5')]),
      ];
      expect(serialize(nodes), 'factorial(5)');
    });

    test('nCr', () {
      final nodes = [
        NcrNode([NumberNode('5')], [NumberNode('2')]),
      ];
      expect(serialize(nodes), 'combinations(5, 2)');
    });

    test('nPr', () {
      final nodes = [
        NprNode([NumberNode('5')], [NumberNode('2')]),
      ];
      expect(serialize(nodes), 'permutations(5, 2)');
    });

    test('paren', () {
      final nodes = [
        ParenNode([NumberNode('3')]),
      ];
      expect(serialize(nodes), '(3)');
    });

    test('complex: 1/2 + sin(pi)', () {
      final nodes = [
        FractionNode([NumberNode('1')], [NumberNode('2')]),
        OperatorNode('+'),
        FunctionNode(FunctionName.sin, [ConstantNode('pi')]),
      ];
      expect(serialize(nodes), '(1)/(2)+sin(pi)');
    });

    test('empty array returns empty string', () {
      expect(serialize([]), '');
    });
  });
}
