import 'package:flutter_test/flutter_test.dart';
import 'package:scientific_calculator/core/ast/types.dart';
import 'package:scientific_calculator/core/ast/builder.dart';

Cursor c(int insertAt, [List<CursorSegment> path = const []]) =>
    Cursor(path, insertAt);

void main() {
  group('insertDigit', () {
    test('inserts NumberNode at empty expression', () {
      final (nodes, cur) = insertDigit([], initialCursor, '3');
      expect(nodes.length, 1);
      expect(nodes[0], isA<NumberNode>().having((n) => n.value, 'value', '3'));
      expect(cur.insertAt, 1);
      expect(cur.path, isEmpty);
    });

    test('appends to adjacent NumberNode', () {
      final init = [NumberNode('3')];
      final (nodes, cur) = insertDigit(init, c(1), '5');
      expect(nodes.length, 1);
      expect(nodes[0], isA<NumberNode>().having((n) => n.value, 'value', '35'));
      expect(cur.insertAt, 1);
      expect(cur.path, isEmpty);
    });

    test('inserts new NumberNode after operator', () {
      final init = [OperatorNode('+')];
      final (nodes, _) = insertDigit(init, c(1), '5');
      expect(nodes[1], isA<NumberNode>().having((n) => n.value, 'value', '5'));
    });
  });

  group('insertDecimalPoint', () {
    test('appends dot to existing number', () {
      final init = [NumberNode('3')];
      final (nodes, _) = insertDecimalPoint(init, c(1));
      expect((nodes[0] as NumberNode).value, '3.');
    });

    test('creates 0. node when no prior number', () {
      final (nodes, _) = insertDecimalPoint([], initialCursor);
      expect(nodes[0], isA<NumberNode>().having((n) => n.value, 'value', '0.'));
    });

    test('does not add second dot', () {
      final init = [NumberNode('3.')];
      final (nodes, _) = insertDecimalPoint(init, c(1));
      expect((nodes[0] as NumberNode).value, '3.');
    });
  });

  group('insertOperator', () {
    test('inserts operator node', () {
      final (nodes, _) = insertOperator([], initialCursor, '+');
      expect(nodes[0], isA<OperatorNode>().having((n) => n.op, 'op', '+'));
    });
  });

  group('insertFraction', () {
    test('inserts fraction template and positions cursor in numerator', () {
      final (nodes, cur) = insertFraction([], initialCursor);
      expect(nodes[0], isA<FractionNode>());
      final frac = nodes[0] as FractionNode;
      expect(frac.numerator, isEmpty);
      expect(frac.denominator, isEmpty);
      expect(cur.path.length, 1);
      expect(cur.path[0].nodeIndex, 0);
      expect(cur.path[0].slot, 'numerator');
      expect(cur.insertAt, 0);
    });
  });

  group('insertExponent', () {
    test('wraps previous node as base, cursor in exponent slot', () {
      final init = [NumberNode('2')];
      final (nodes, cur) = insertExponent(init, c(1));
      expect(nodes[0], isA<ExponentNode>());
      final exp = nodes[0] as ExponentNode;
      expect(exp.base.length, 1);
      expect(exp.base[0], isA<NumberNode>().having((n) => n.value, 'value', '2'));
      expect(exp.exponent, isEmpty);
      expect(cur.path[0].slot, 'exponent');
      expect(cur.insertAt, 0);
    });

    test('creates empty base when nothing before cursor', () {
      final (nodes, cur) = insertExponent([], initialCursor);
      expect((nodes[0] as ExponentNode).base, isEmpty);
      expect(cur.path[0].slot, 'exponent');
    });
  });

  group('insertRadical', () {
    test('inserts sqrt with empty degree, cursor in radicand', () {
      final (nodes, cur) = insertRadical([], initialCursor, false);
      expect(nodes[0], isA<RadicalNode>());
      final rad = nodes[0] as RadicalNode;
      expect(rad.degree, isEmpty);
      expect(rad.radicand, isEmpty);
      expect(cur.path[0].slot, 'radicand');
    });

    test('inserts cbrt with degree=[3]', () {
      final (nodes, _) = insertRadical([], initialCursor, true);
      final rad = nodes[0] as RadicalNode;
      expect(rad.degree.length, 1);
      expect(rad.degree[0], isA<NumberNode>().having((n) => n.value, 'value', '3'));
    });
  });

  group('insertSquare', () {
    test('wraps previous node in exponent with exponent=[2]', () {
      final init = [NumberNode('4')];
      final (nodes, cur) = insertSquare(init, c(1));
      expect(nodes[0], isA<ExponentNode>());
      final exp = nodes[0] as ExponentNode;
      expect(exp.base.length, 1);
      expect(exp.base[0], isA<NumberNode>().having((n) => n.value, 'value', '4'));
      expect(exp.exponent.length, 1);
      expect(exp.exponent[0], isA<NumberNode>().having((n) => n.value, 'value', '2'));
      expect(cur.insertAt, 1);
      expect(cur.path, isEmpty);
    });
  });

  group('insertFunction', () {
    test('inserts function node, cursor in argument', () {
      final (nodes, cur) = insertFunction([], initialCursor, FunctionName.sin);
      expect(nodes[0], isA<FunctionNode>());
      final fn = nodes[0] as FunctionNode;
      expect(fn.name, FunctionName.sin);
      expect(fn.argument, isEmpty);
      expect(cur.path[0].slot, 'argument');
    });
  });

  group('insertConstant', () {
    test('inserts pi constant', () {
      final (nodes, _) = insertConstant([], initialCursor, 'pi');
      expect(nodes[0], isA<ConstantNode>().having((n) => n.name, 'name', 'pi'));
    });
  });

  group('insertFactorial', () {
    test('wraps previous node in factorial', () {
      final init = [NumberNode('5')];
      final (nodes, cur) = insertFactorial(init, c(1));
      expect(nodes[0], isA<FactorialNode>());
      final fact = nodes[0] as FactorialNode;
      expect(fact.operand.length, 1);
      expect(fact.operand[0], isA<NumberNode>().having((n) => n.value, 'value', '5'));
      expect(cur.insertAt, 1);
      expect(cur.path, isEmpty);
    });
  });

  group('insertNcr', () {
    test('wraps previous node as n, cursor in r slot', () {
      final init = [NumberNode('5')];
      final (nodes, cur) = insertNcr(init, c(1));
      expect(nodes[0], isA<NcrNode>());
      final ncr = nodes[0] as NcrNode;
      expect(ncr.n.length, 1);
      expect(ncr.n[0], isA<NumberNode>().having((n) => n.value, 'value', '5'));
      expect(ncr.r, isEmpty);
      expect(cur.path[0].slot, 'r');
    });
  });

  group('insertNpr', () {
    test('wraps previous node as n, cursor in r slot', () {
      final init = [NumberNode('5')];
      final (nodes, cur) = insertNpr(init, c(1));
      expect(nodes[0], isA<NprNode>());
      final npr = nodes[0] as NprNode;
      expect(npr.n.length, 1);
      expect(npr.n[0], isA<NumberNode>().having((n) => n.value, 'value', '5'));
      expect(npr.r, isEmpty);
      expect(cur.path[0].slot, 'r');
    });
  });

  group('insertParen', () {
    test('open: inserts paren node, cursor in children', () {
      final (nodes, cur) = insertParen([], initialCursor, 'open');
      expect(nodes[0], isA<ParenNode>());
      expect((nodes[0] as ParenNode).children, isEmpty);
      expect(cur.path[0].slot, 'children');
    });

    test('close: exits to parent level after paren node', () {
      final init = [ParenNode([])];
      final inner = Cursor([CursorSegment(0, 'children')], 0);
      final (_, cur) = insertParen(init, inner, 'close');
      expect(cur.insertAt, 1);
      expect(cur.path, isEmpty);
    });
  });

  group('deleteCurrent', () {
    test('does nothing when expression empty at root', () {
      final (nodes, cur) = deleteCurrent([], initialCursor);
      expect(nodes, isEmpty);
      expect(cur.insertAt, 0);
      expect(cur.path, isEmpty);
    });

    test('removes last node', () {
      final init = [NumberNode('3'), OperatorNode('+')];
      final (nodes, cur) = deleteCurrent(init, c(2));
      expect(nodes.length, 1);
      expect(nodes[0], isA<NumberNode>().having((n) => n.value, 'value', '3'));
      expect(cur.insertAt, 1);
      expect(cur.path, isEmpty);
    });

    test('removes last digit from multi-digit number', () {
      final init = [NumberNode('35')];
      final (nodes, cur) = deleteCurrent(init, c(1));
      expect((nodes[0] as NumberNode).value, '3');
      expect(cur.insertAt, 1);
      expect(cur.path, isEmpty);
    });

    test('exits to parent when at position 0 in a slot', () {
      final init = [FractionNode([], [])];
      final inner = Cursor([CursorSegment(0, 'numerator')], 0);
      final (_, cur) = deleteCurrent(init, inner);
      expect(cur.insertAt, 0);
      expect(cur.path, isEmpty);
    });
  });

  group('clearAll', () {
    test('returns empty expression and reset cursor', () {
      final (nodes, cur) = clearAll();
      expect(nodes, isEmpty);
      expect(cur.insertAt, 0);
      expect(cur.path, isEmpty);
    });
  });

  group('insertCube', () {
    test('wraps previous node as base with exponent 3', () {
      final init = [NumberNode('5')];
      final (nodes, _) = insertCube(init, c(1));
      expect(nodes.length, 1);
      expect(nodes[0], isA<ExponentNode>());
      final exp = nodes[0] as ExponentNode;
      expect(exp.base.length, 1);
      expect(exp.base[0], isA<NumberNode>().having((n) => n.value, 'value', '5'));
      expect(exp.exponent.length, 1);
      expect(exp.exponent[0], isA<NumberNode>().having((n) => n.value, 'value', '3'));
    });

    test('inserts empty-base exponent^3 when no previous node', () {
      final (nodes, _) = insertCube([], initialCursor);
      expect(nodes[0], isA<ExponentNode>());
      final exp = nodes[0] as ExponentNode;
      expect(exp.base, isEmpty);
      expect(exp.exponent.length, 1);
      expect(exp.exponent[0], isA<NumberNode>().having((n) => n.value, 'value', '3'));
    });
  });

  group('insertNthRadical', () {
    test('inserts radical with empty degree and cursor in degree slot', () {
      final (nodes, cur) = insertNthRadical([], initialCursor);
      expect(nodes[0], isA<RadicalNode>());
      final rad = nodes[0] as RadicalNode;
      expect(rad.degree, isEmpty);
      expect(rad.radicand, isEmpty);
      expect(cur.path.length, 1);
      expect(cur.path[0].nodeIndex, 0);
      expect(cur.path[0].slot, 'degree');
      expect(cur.insertAt, 0);
    });
  });

  group('insertMixed', () {
    test('inserts whole-number 0 then empty fraction, cursor starts in numerator', () {
      final (nodes, cur) = insertMixed([], initialCursor);
      expect(nodes.length, 2);
      expect(nodes[0], isA<NumberNode>().having((n) => n.value, 'value', '0'));
      expect(nodes[1], isA<FractionNode>());
      final frac = nodes[1] as FractionNode;
      expect(frac.numerator, isEmpty);
      expect(frac.denominator, isEmpty);
      expect(cur.path.length, 1);
      expect(cur.path[0].nodeIndex, 1);
      expect(cur.path[0].slot, 'numerator');
      expect(cur.insertAt, 0);
    });
  });

  group('insertNumberLiteral', () {
    test('inserts a NumberNode with the given string value', () {
      final (nodes, cur) = insertNumberLiteral([], initialCursor, '3.14');
      expect(nodes[0], isA<NumberNode>().having((n) => n.value, 'value', '3.14'));
      expect(cur.insertAt, 1);
      expect(cur.path, isEmpty);
    });
  });
}
