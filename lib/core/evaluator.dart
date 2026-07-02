import 'dart:math' as math;
import 'ast/types.dart';

String? evaluate(
  List<ASTNode> nodes,
  AngleMode mode,
  String? lastResult,
  MemorySlots memory,
) {
  if (nodes.isEmpty) return null;
  try {
    final result = _evalNodes(nodes, mode, lastResult, memory);
    if (!result.isFinite) return 'Math Error';
    return formatNumber(result);
  } catch (_) {
    return null;
  }
}

double _evalNodes(
  List<ASTNode> nodes,
  AngleMode mode,
  String? lastResult,
  MemorySlots memory,
) {
  final List<double> operands = [];
  final List<String> operators = [];
  double acc = _evalNode(nodes[0], mode, lastResult, memory);
  for (int i = 1; i < nodes.length; i += 2) {
    final op = (nodes[i] as OperatorNode).op;
    final right = _evalNode(nodes[i + 1], mode, lastResult, memory);
    if (op == '×') {
      acc *= right;
    } else if (op == '÷') {
      acc /= right;
    } else {
      operands.add(acc);
      operators.add(op);
      acc = right;
    }
  }
  operands.add(acc);
  double result = operands[0];
  for (int j = 0; j < operators.length; j++) {
    if (operators[j] == '+') {
      result += operands[j + 1];
    } else {
      result -= operands[j + 1];
    }
  }
  return result;
}

double _evalNode(
  ASTNode node,
  AngleMode mode,
  String? lastResult,
  MemorySlots memory,
) {
  switch (node) {
    case NumberNode n:
      return double.parse(n.value);
    case OperatorNode _:
      throw StateError('OperatorNode should not be evaluated directly');
    case ConstantNode n:
      switch (n.name) {
        case 'pi':
          return math.pi;
        case 'e':
          return math.e;
        case 'Ans':
          return double.parse(lastResult ?? '0');
        case 'A':
          return memory.a ?? 0.0;
        case 'B':
          return memory.b ?? 0.0;
        case 'X':
          return memory.x ?? 0.0;
        case 'Y':
          return memory.y ?? 0.0;
        default:
          throw ArgumentError('Unknown constant: ${n.name}');
      }
    case FractionNode n:
      return _evalNodes(n.numerator, mode, lastResult, memory) /
          _evalNodes(n.denominator, mode, lastResult, memory);
    case ExponentNode n:
      return math
          .pow(
            _evalNodes(n.base, mode, lastResult, memory),
            _evalNodes(n.exponent, mode, lastResult, memory),
          )
          .toDouble();
    case RadicalNode n:
      final radicand = _evalNodes(n.radicand, mode, lastResult, memory);
      if (n.degree.isEmpty) {
        return math.sqrt(radicand);
      }
      final degree = _evalNodes(n.degree, mode, lastResult, memory);
      return math.pow(radicand, 1.0 / degree).toDouble();
    case FunctionNode n:
      final arg = _evalNodes(n.argument, mode, lastResult, memory);
      return _evalFunction(n.name, arg, mode);
    case FactorialNode n:
      return _factorial(_evalNodes(n.operand, mode, lastResult, memory).round());
    case NcrNode n:
      final nVal = _evalNodes(n.n, mode, lastResult, memory).round();
      final rVal = _evalNodes(n.r, mode, lastResult, memory).round();
      return _ncr(nVal, rVal);
    case NprNode n:
      final nVal = _evalNodes(n.n, mode, lastResult, memory).round();
      final rVal = _evalNodes(n.r, mode, lastResult, memory).round();
      return _npr(nVal, rVal);
    case ParenNode n:
      return _evalNodes(n.children, mode, lastResult, memory);
  }
}

double _evalFunction(FunctionName name, double arg, AngleMode mode) {
  final toRad = mode == AngleMode.deg ? arg * math.pi / 180.0 : arg;
  final fromRad = mode == AngleMode.deg ? 180.0 / math.pi : 1.0;
  switch (name) {
    case FunctionName.sin:
      return math.sin(toRad);
    case FunctionName.cos:
      return math.cos(toRad);
    case FunctionName.tan:
      return math.tan(toRad);
    case FunctionName.asin:
      return math.asin(arg) * fromRad;
    case FunctionName.acos:
      return math.acos(arg) * fromRad;
    case FunctionName.atan:
      return math.atan(arg) * fromRad;
    case FunctionName.sinh:
      final x = mode == AngleMode.deg ? arg * math.pi / 180.0 : arg;
      return (math.exp(x) - math.exp(-x)) / 2.0;
    case FunctionName.cosh:
      final x = mode == AngleMode.deg ? arg * math.pi / 180.0 : arg;
      return (math.exp(x) + math.exp(-x)) / 2.0;
    case FunctionName.tanh:
      final x = mode == AngleMode.deg ? arg * math.pi / 180.0 : arg;
      final ex = math.exp(x);
      final enx = math.exp(-x);
      return (ex - enx) / (ex + enx);
    case FunctionName.asinh:
      final result = math.log(arg + math.sqrt(arg * arg + 1));
      return result * fromRad;
    case FunctionName.acosh:
      final result = math.log(arg + math.sqrt(arg * arg - 1));
      return result * fromRad;
    case FunctionName.atanh:
      final result = 0.5 * math.log((1 + arg) / (1 - arg));
      return result * fromRad;
    case FunctionName.log:
      return math.log(arg) / math.log(10);
    case FunctionName.ln:
      return math.log(arg);
    case FunctionName.pow10:
      return math.pow(10, arg).toDouble();
    case FunctionName.exp:
      return math.exp(arg);
    case FunctionName.abs:
      return arg.abs();
  }
}

double _factorial(int n) {
  if (n < 0 || n > 170) throw ArgumentError('Factorial out of range: $n');
  double r = 1.0;
  for (int i = 2; i <= n; i++) {
    r *= i;
  }
  return r;
}

double _ncr(int n, int r) {
  if (r < 0 || r > n) return 0.0;
  return _factorial(n) / (_factorial(r) * _factorial(n - r));
}

double _npr(int n, int r) {
  if (r < 0 || r > n) return 0.0;
  return _factorial(n) / _factorial(n - r);
}

String? toFraction(String decimalStr) {
  final value = double.tryParse(decimalStr);
  if (value == null || !value.isFinite) return null;
  if (value == value.truncateToDouble()) return null;
  const scale = 1000000;
  int num = (value * scale).round();
  int den = scale;
  final g = _gcd(num.abs(), den);
  num ~/= g;
  den ~/= g;
  if (den > 999 || den <= 1) return null;
  return '$num/$den';
}

int _gcd(int a, int b) {
  while (b != 0) {
    final t = b;
    b = a % b;
    a = t;
  }
  return a;
}

String? toMixed(String fracStr) {
  final match = RegExp(r'^(-?\d+)\/(\d+)$').firstMatch(fracStr);
  if (match == null) return null;
  final num = int.parse(match.group(1)!);
  final den = int.parse(match.group(2)!);
  if (den == 0 || num.abs() < den) return null;
  final whole = num ~/ den;
  final remainder = num.abs() % den;
  if (remainder == 0) return whole.toString();
  return '$whole $remainder/$den';
}

String? fromMixed(String mixedStr) {
  final match = RegExp(r'^(-?\d+) (\d+)\/(\d+)$').firstMatch(mixedStr);
  if (match == null) return null;
  final whole = int.parse(match.group(1)!);
  final num = int.parse(match.group(2)!);
  final den = int.parse(match.group(3)!);
  final improperNum = whole < 0 ? whole * den - num : whole * den + num;
  return '$improperNum/$den';
}

String formatNumber(double n) {
  if (n == n.truncateToDouble() && n.abs() < 1e15) {
    return n.toInt().toString();
  }
  return double.parse(n.toStringAsPrecision(10)).toString();
}
