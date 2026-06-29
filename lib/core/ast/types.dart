enum FunctionName {
  sin,
  cos,
  tan,
  asin,
  acos,
  atan,
  sinh,
  cosh,
  tanh,
  asinh,
  acosh,
  atanh,
  log,
  ln,
  pow10,
  exp,
  abs,
}

enum AngleMode { deg, rad }

sealed class ASTNode {}

final class NumberNode extends ASTNode {
  String value;
  NumberNode(this.value);
}

final class OperatorNode extends ASTNode {
  final String op;
  OperatorNode(this.op);
}

final class ConstantNode extends ASTNode {
  final String name;
  ConstantNode(this.name);
}

final class FractionNode extends ASTNode {
  List<ASTNode> numerator;
  List<ASTNode> denominator;
  FractionNode(this.numerator, this.denominator);
}

final class ExponentNode extends ASTNode {
  List<ASTNode> base;
  List<ASTNode> exponent;
  ExponentNode(this.base, this.exponent);
}

final class RadicalNode extends ASTNode {
  List<ASTNode> degree;
  List<ASTNode> radicand;
  RadicalNode(this.degree, this.radicand);
}

final class FunctionNode extends ASTNode {
  final FunctionName name;
  List<ASTNode> argument;
  FunctionNode(this.name, this.argument);
}

final class FactorialNode extends ASTNode {
  List<ASTNode> operand;
  FactorialNode(this.operand);
}

final class NcrNode extends ASTNode {
  List<ASTNode> n;
  List<ASTNode> r;
  NcrNode(this.n, this.r);
}

final class NprNode extends ASTNode {
  List<ASTNode> n;
  List<ASTNode> r;
  NprNode(this.n, this.r);
}

final class ParenNode extends ASTNode {
  List<ASTNode> children;
  ParenNode(this.children);
}

class CursorSegment {
  final int nodeIndex;
  final String slot;
  const CursorSegment(this.nodeIndex, this.slot);
}

class Cursor {
  final List<CursorSegment> path;
  final int insertAt;
  const Cursor(this.path, this.insertAt);
}

const initialCursor = Cursor([], 0);

class MemorySlots {
  final double? a;
  final double? b;
  final double? x;
  final double? y;

  const MemorySlots({this.a, this.b, this.x, this.y});

  factory MemorySlots.empty() => const MemorySlots();

  MemorySlots copyWith({
    Object? a = _sentinel,
    Object? b = _sentinel,
    Object? x = _sentinel,
    Object? y = _sentinel,
  }) {
    return MemorySlots(
      a: a == _sentinel ? this.a : a as double?,
      b: b == _sentinel ? this.b : b as double?,
      x: x == _sentinel ? this.x : x as double?,
      y: y == _sentinel ? this.y : y as double?,
    );
  }
}

const _sentinel = Object();

class HistoryEntry {
  final String id;
  final List<ASTNode> expression;
  final String result;
  final int timestamp;

  const HistoryEntry({
    required this.id,
    required this.expression,
    required this.result,
    required this.timestamp,
  });

  factory HistoryEntry.fromJson(Map<String, dynamic> json) {
    return HistoryEntry(
      id: json['id'] as String,
      expression: (json['expression'] as List<dynamic>)
          .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
          .toList(),
      result: json['result'] as String,
      timestamp: json['timestamp'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'expression': expression.map(_astNodeToJson).toList(),
      'result': result,
      'timestamp': timestamp,
    };
  }
}

Map<String, dynamic> _astNodeToJson(ASTNode node) {
  switch (node) {
    case NumberNode n:
      return {'type': 'number', 'value': n.value};
    case OperatorNode n:
      return {'type': 'operator', 'op': n.op};
    case ConstantNode n:
      return {'type': 'constant', 'name': n.name};
    case FractionNode n:
      return {
        'type': 'fraction',
        'numerator': n.numerator.map(_astNodeToJson).toList(),
        'denominator': n.denominator.map(_astNodeToJson).toList(),
      };
    case ExponentNode n:
      return {
        'type': 'exponent',
        'base': n.base.map(_astNodeToJson).toList(),
        'exponent': n.exponent.map(_astNodeToJson).toList(),
      };
    case RadicalNode n:
      return {
        'type': 'radical',
        'degree': n.degree.map(_astNodeToJson).toList(),
        'radicand': n.radicand.map(_astNodeToJson).toList(),
      };
    case FunctionNode n:
      return {
        'type': 'function',
        'name': n.name.name,
        'argument': n.argument.map(_astNodeToJson).toList(),
      };
    case FactorialNode n:
      return {
        'type': 'factorial',
        'operand': n.operand.map(_astNodeToJson).toList(),
      };
    case NcrNode n:
      return {
        'type': 'ncr',
        'n': n.n.map(_astNodeToJson).toList(),
        'r': n.r.map(_astNodeToJson).toList(),
      };
    case NprNode n:
      return {
        'type': 'npr',
        'n': n.n.map(_astNodeToJson).toList(),
        'r': n.r.map(_astNodeToJson).toList(),
      };
    case ParenNode n:
      return {
        'type': 'paren',
        'children': n.children.map(_astNodeToJson).toList(),
      };
  }
}

ASTNode _astNodeFromJson(Map<String, dynamic> json) {
  final type = json['type'] as String;
  switch (type) {
    case 'number':
      return NumberNode(json['value'] as String);
    case 'operator':
      return OperatorNode(json['op'] as String);
    case 'constant':
      return ConstantNode(json['name'] as String);
    case 'fraction':
      return FractionNode(
        (json['numerator'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
        (json['denominator'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
      );
    case 'exponent':
      return ExponentNode(
        (json['base'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
        (json['exponent'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
      );
    case 'radical':
      return RadicalNode(
        (json['degree'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
        (json['radicand'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
      );
    case 'function':
      return FunctionNode(
        FunctionName.values.byName(json['name'] as String),
        (json['argument'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
      );
    case 'factorial':
      return FactorialNode(
        (json['operand'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
      );
    case 'ncr':
      return NcrNode(
        (json['n'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
        (json['r'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
      );
    case 'npr':
      return NprNode(
        (json['n'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
        (json['r'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
      );
    case 'paren':
      return ParenNode(
        (json['children'] as List<dynamic>)
            .map((e) => _astNodeFromJson(e as Map<String, dynamic>))
            .toList(),
      );
    default:
      throw ArgumentError('Unknown AST node type: $type');
  }
}

class CalculatorState {
  final List<ASTNode> expression;
  final Cursor cursor;
  final String? result;
  final String? lastResult;
  final String resultMode;
  final AngleMode angleMode;
  final bool shiftActive;
  final bool hypActive;
  final List<HistoryEntry> history;
  final MemorySlots memory;
  final bool stoMode;

  const CalculatorState({
    required this.expression,
    required this.cursor,
    required this.result,
    required this.lastResult,
    required this.resultMode,
    required this.angleMode,
    required this.shiftActive,
    required this.hypActive,
    required this.history,
    required this.memory,
    required this.stoMode,
  });

  factory CalculatorState.initial() => CalculatorState(
        expression: const [],
        cursor: initialCursor,
        result: null,
        lastResult: null,
        resultMode: 'decimal',
        angleMode: AngleMode.deg,
        shiftActive: false,
        hypActive: false,
        history: const [],
        memory: MemorySlots.empty(),
        stoMode: false,
      );

  CalculatorState copyWith({
    List<ASTNode>? expression,
    Cursor? cursor,
    Object? result = _sentinel,
    Object? lastResult = _sentinel,
    String? resultMode,
    AngleMode? angleMode,
    bool? shiftActive,
    bool? hypActive,
    List<HistoryEntry>? history,
    MemorySlots? memory,
    bool? stoMode,
  }) {
    return CalculatorState(
      expression: expression ?? this.expression,
      cursor: cursor ?? this.cursor,
      result: result == _sentinel ? this.result : result as String?,
      lastResult: lastResult == _sentinel ? this.lastResult : lastResult as String?,
      resultMode: resultMode ?? this.resultMode,
      angleMode: angleMode ?? this.angleMode,
      shiftActive: shiftActive ?? this.shiftActive,
      hypActive: hypActive ?? this.hypActive,
      history: history ?? this.history,
      memory: memory ?? this.memory,
      stoMode: stoMode ?? this.stoMode,
    );
  }
}
