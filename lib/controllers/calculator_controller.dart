import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core/ast/types.dart';
import '../core/ast/builder.dart';
import '../core/ast/cursor.dart';
import '../core/evaluator.dart';
import '../core/history.dart';

class CalculatorNotifier extends Notifier<CalculatorState> {
  @override
  CalculatorState build() {
    _initHistory();
    return CalculatorState.initial();
  }

  Future<void> _initHistory() async {
    final h = await loadHistory();
    state = state.copyWith(history: h);
  }

  void handleButton(String id) {
    state = _reduce(state, id);
  }

  void handleCursorJump(List<CursorSegment> path, int insertAt) {
    state = state.copyWith(cursor: Cursor(path, insertAt));
  }

  Future<void> handlePaste() async {
    final data = await Clipboard.getData(Clipboard.kTextPlain);
    final text = data?.text?.trim() ?? '';
    if (!RegExp(r'^-?[\d]+(\.[\d]+)?([eE][+-]?[\d]+)?$').hasMatch(text)) {
      return;
    }
    final (expr, cur) =
        insertNumberLiteral(state.expression, state.cursor, text);
    final res =
        evaluate(expr, state.angleMode, state.lastResult, state.memory);
    state = state.copyWith(
      expression: expr,
      cursor: cur,
      result: res,
      resultMode: 'decimal',
      shiftActive: false,
      hypActive: false,
      stoMode: false,
    );
  }

  void handleRestore(HistoryEntry entry) {
    state = state.copyWith(
      expression: entry.expression,
      cursor: Cursor(const [], entry.expression.length),
      result: entry.result,
      resultMode: 'decimal',
      shiftActive: false,
      hypActive: false,
      stoMode: false,
    );
  }

  CalculatorState _reduce(CalculatorState prev, String id) {
    if (id == 'SHIFT') {
      return prev.copyWith(shiftActive: !prev.shiftActive, hypActive: false);
    }

    if (id == 'HYP') {
      return prev.copyWith(hypActive: !prev.hypActive, shiftActive: false);
    }

    if (id == 'DEG_RAD') {
      return prev.copyWith(
        angleMode:
            prev.angleMode == AngleMode.deg ? AngleMode.rad : AngleMode.deg,
        shiftActive: false,
      );
    }

    if (id == 'Sto') {
      return prev.copyWith(
          stoMode: true, shiftActive: false, hypActive: false);
    }

    if (id == 'A' || id == 'B' || id == 'X' || id == 'Y') {
      if (prev.stoMode) {
        final val = double.tryParse(prev.result ?? '');
        if (val == null) {
          return prev.copyWith(
              stoMode: false, shiftActive: false, hypActive: false);
        }
        final mem = switch (id) {
          'A' => prev.memory.copyWith(a: val),
          'B' => prev.memory.copyWith(b: val),
          'X' => prev.memory.copyWith(x: val),
          'Y' => prev.memory.copyWith(y: val),
          _ => prev.memory,
        };
        return prev.copyWith(
            memory: mem, stoMode: false, shiftActive: false, hypActive: false);
      } else {
        final (expr, cur) = insertConstant(prev.expression, prev.cursor, id);
        final res =
            evaluate(expr, prev.angleMode, prev.lastResult, prev.memory);
        return prev.copyWith(
          expression: expr,
          cursor: cur,
          result: res,
          resultMode: 'decimal',
          shiftActive: false,
          hypActive: false,
          stoMode: false,
        );
      }
    }

    if (id == 'S_TO_D' || id == 'DecFrac') {
      if (prev.resultMode == 'fraction') {
        return prev.copyWith(
          result: prev.lastResult,
          lastResult: null,
          resultMode: 'decimal',
          shiftActive: false,
          hypActive: false,
          stoMode: false,
        );
      }
      final fracStr = toFraction(prev.result ?? '');
      if (fracStr == null) {
        return prev.copyWith(
            shiftActive: false, hypActive: false, stoMode: false);
      }
      return prev.copyWith(
        lastResult: prev.result,
        result: fracStr,
        resultMode: 'fraction',
        shiftActive: false,
        hypActive: false,
        stoMode: false,
      );
    }

    if (id == 'MixedFrac') {
      final mixed = toMixed(prev.result ?? '');
      if (mixed != null) {
        return prev.copyWith(
          result: mixed,
          resultMode: 'fraction',
          shiftActive: false,
          hypActive: false,
          stoMode: false,
        );
      }
      final improper = fromMixed(prev.result ?? '');
      if (improper != null) {
        return prev.copyWith(
          result: improper,
          resultMode: 'fraction',
          shiftActive: false,
          hypActive: false,
          stoMode: false,
        );
      }
      return prev.copyWith(
          shiftActive: false, hypActive: false, stoMode: false);
    }

    if (id == 'LEFT') {
      return prev.copyWith(cursor: moveCursorLeft(prev.expression, prev.cursor));
    }
    if (id == 'RIGHT') {
      return prev.copyWith(
          cursor: moveCursorRight(prev.expression, prev.cursor));
    }
    if (id == 'UP') {
      return prev.copyWith(cursor: moveCursorUp(prev.expression, prev.cursor));
    }
    if (id == 'DOWN') {
      return prev.copyWith(
          cursor: moveCursorDown(prev.expression, prev.cursor));
    }

    if (id == 'DEL') {
      final (expr, cur) = deleteCurrent(prev.expression, prev.cursor);
      final res =
          evaluate(expr, prev.angleMode, prev.lastResult, prev.memory);
      return prev.copyWith(
        expression: expr,
        cursor: cur,
        result: res,
        resultMode: 'decimal',
        shiftActive: false,
        hypActive: false,
        stoMode: false,
      );
    }

    if (id == 'CLEAR' || id == 'AC') {
      final (expr, cur) = clearAll();
      return prev.copyWith(
        expression: expr,
        cursor: cur,
        result: null,
        resultMode: 'decimal',
        shiftActive: false,
        hypActive: false,
        stoMode: false,
      );
    }

    if (id == '=') {
      final res =
          evaluate(prev.expression, prev.angleMode, prev.lastResult, prev.memory);
      if (res == null) {
        return prev.copyWith(
            shiftActive: false, hypActive: false, stoMode: false);
      }
      _persistHistory(prev.expression, res, prev.history);
      return prev.copyWith(
        result: res,
        lastResult: res,
        resultMode: 'decimal',
        shiftActive: false,
        hypActive: false,
        stoMode: false,
      );
    }

    if (id == 'fraction' && prev.result != null) {
      if (prev.resultMode == 'fraction') {
        return prev.copyWith(
          result: prev.lastResult,
          lastResult: null,
          resultMode: 'decimal',
          shiftActive: false,
          hypActive: false,
          stoMode: false,
        );
      }
      final fracStr = toFraction(prev.result!);
      if (fracStr != null) {
        return prev.copyWith(
          lastResult: prev.result,
          result: fracStr,
          resultMode: 'fraction',
          shiftActive: false,
          hypActive: false,
          stoMode: false,
        );
      }
      return prev.copyWith(
          shiftActive: false, hypActive: false, stoMode: false);
    }

    final (expr, cur) = _applyInsert(prev, id);
    if (expr == null) {
      return prev.copyWith(
          shiftActive: false, hypActive: false, stoMode: false);
    }
    final res =
        evaluate(expr, prev.angleMode, prev.lastResult, prev.memory);
    return prev.copyWith(
      expression: expr,
      cursor: cur!,
      result: res,
      resultMode: 'decimal',
      shiftActive: false,
      hypActive: false,
      stoMode: false,
    );
  }

  (List<ASTNode>?, Cursor?) _applyInsert(CalculatorState prev, String id) {
    final e = prev.expression;
    final c = prev.cursor;

    switch (id) {
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        return insertDigit(e, c, id);
      case '.':
        return insertDecimalPoint(e, c);
      case 'plus':
        return insertOperator(e, c, '+');
      case 'minus':
        return insertOperator(e, c, '-');
      case 'multiply':
        return insertOperator(e, c, '×');
      case 'divide':
        return insertOperator(e, c, '÷');
      case 'fraction':
        return insertFraction(e, c);
      case 'exponent':
        return insertExponent(e, c);
      case 'sqrt':
        return insertRadical(e, c, false);
      case 'cbrt':
        return insertRadical(e, c, true);
      case 'square':
        return insertSquare(e, c);
      case 'cube':
        return insertCube(e, c);
      case 'nthRoot':
        return insertNthRadical(e, c);
      case 'mix':
        return insertMixed(e, c);
      case 'factorial':
        return insertFactorial(e, c);
      case 'ncr':
        return insertNcr(e, c);
      case 'npr':
        return insertNpr(e, c);
      case 'paren_open':
        return insertParen(e, c, 'open');
      case 'paren_close':
        return insertParen(e, c, 'close');
      case 'pi':
        return insertConstant(e, c, 'pi');
      case 'e_const':
        return insertConstant(e, c, 'e');
      case 'Ans':
        return insertConstant(e, c, 'Ans');
      case 'EXP':
        return insertExp(e, c);
      case 'abs':
        return insertFunction(e, c, FunctionName.abs);
      case 'sin':
        return insertFunction(e, c, FunctionName.sin);
      case 'cos':
        return insertFunction(e, c, FunctionName.cos);
      case 'tan':
        return insertFunction(e, c, FunctionName.tan);
      case 'asin':
        return insertFunction(e, c, FunctionName.asin);
      case 'acos':
        return insertFunction(e, c, FunctionName.acos);
      case 'atan':
        return insertFunction(e, c, FunctionName.atan);
      case 'sinh':
        return insertFunction(e, c, FunctionName.sinh);
      case 'cosh':
        return insertFunction(e, c, FunctionName.cosh);
      case 'tanh':
        return insertFunction(e, c, FunctionName.tanh);
      case 'asinh':
        return insertFunction(e, c, FunctionName.asinh);
      case 'acosh':
        return insertFunction(e, c, FunctionName.acosh);
      case 'atanh':
        return insertFunction(e, c, FunctionName.atanh);
      case 'log':
        return insertFunction(e, c, FunctionName.log);
      case 'ln':
        return insertFunction(e, c, FunctionName.ln);
      case 'exp':
        return insertFunction(e, c, FunctionName.exp);
      default:
        return (null, null);
    }
  }

  void _persistHistory(
      List<ASTNode> expression, String result, List<HistoryEntry> current) {
    addEntry(current, expression, result).then((updated) {
      state = state.copyWith(history: updated);
    });
  }
}

final calculatorProvider =
    NotifierProvider<CalculatorNotifier, CalculatorState>(CalculatorNotifier.new);
