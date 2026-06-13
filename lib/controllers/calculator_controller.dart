import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import '../core/ast/types.dart';
import '../core/ast/builder.dart';
import '../core/ast/cursor.dart';
import '../core/evaluator.dart';
import '../core/history.dart';

class CalculatorController extends ChangeNotifier {
  CalculatorState _state;

  CalculatorController() : _state = CalculatorState.initial() {
    _initHistory();
  }

  CalculatorState get state => _state;

  Future<void> _initHistory() async {
    final h = await loadHistory();
    _state = _state.copyWith(history: h);
    notifyListeners();
  }

  void handleButton(String id) {
    _state = _reduce(_state, id);
    notifyListeners();
  }

  void handleCursorJump(List<CursorSegment> path, int insertAt) {
    _state = _state.copyWith(cursor: Cursor(path, insertAt));
    notifyListeners();
  }

  Future<void> handlePaste() async {
    final data = await Clipboard.getData(Clipboard.kTextPlain);
    final text = data?.text?.trim() ?? '';
    if (!RegExp(r'^-?[\d]+(\.[\d]+)?([eE][+-]?[\d]+)?$').hasMatch(text)) return;
    final (expr, cur) =
        insertNumberLiteral(_state.expression, _state.cursor, text);
    final res = evaluate(expr, _state.angleMode, _state.lastResult, _state.memory);
    _state = _state.copyWith(
      expression: expr,
      cursor: cur,
      result: res,
      resultMode: 'decimal',
      shiftActive: false,
      hypActive: false,
      stoMode: false,
    );
    notifyListeners();
  }

  void handleRestore(HistoryEntry entry) {
    _state = _state.copyWith(
      expression: entry.expression,
      cursor: Cursor(const [], entry.expression.length),
      result: entry.result,
      resultMode: 'decimal',
      shiftActive: false,
      hypActive: false,
      stoMode: false,
    );
    notifyListeners();
  }

  CalculatorState _reduce(CalculatorState prev, String id) {
    if (id == 'SHIFT') {
      return prev.copyWith(
        shiftActive: !prev.shiftActive,
        hypActive: false,
      );
    }

    if (id == 'HYP') {
      return prev.copyWith(
        hypActive: !prev.hypActive,
        shiftActive: false,
      );
    }

    if (id == 'DEG_RAD') {
      return prev.copyWith(
        angleMode: prev.angleMode == AngleMode.deg ? AngleMode.rad : AngleMode.deg,
        shiftActive: false,
      );
    }

    if (id == 'Sto') {
      return prev.copyWith(
        stoMode: true,
        shiftActive: false,
        hypActive: false,
      );
    }

    if (id == 'A' || id == 'B' || id == 'X' || id == 'Y') {
      if (prev.stoMode) {
        final val = double.tryParse(prev.result ?? '');
        if (val == null) {
          return prev.copyWith(
            stoMode: false,
            shiftActive: false,
            hypActive: false,
          );
        }
        final mem = switch (id) {
          'A' => prev.memory.copyWith(a: val),
          'B' => prev.memory.copyWith(b: val),
          'X' => prev.memory.copyWith(x: val),
          'Y' => prev.memory.copyWith(y: val),
          _ => prev.memory,
        };
        return prev.copyWith(
          memory: mem,
          stoMode: false,
          shiftActive: false,
          hypActive: false,
        );
      } else {
        final (expr, cur) =
            insertConstant(prev.expression, prev.cursor, id);
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
      } else {
        final fracStr = toFraction(prev.result ?? '');
        if (fracStr == null) {
          return prev.copyWith(
            shiftActive: false,
            hypActive: false,
            stoMode: false,
          );
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
        shiftActive: false,
        hypActive: false,
        stoMode: false,
      );
    }

    if (id == 'LEFT') {
      final cur = moveCursorLeft(prev.expression, prev.cursor);
      return prev.copyWith(cursor: cur);
    }

    if (id == 'RIGHT') {
      final cur = moveCursorRight(prev.expression, prev.cursor);
      return prev.copyWith(cursor: cur);
    }

    if (id == 'UP') {
      final cur = moveCursorUp(prev.expression, prev.cursor);
      return prev.copyWith(cursor: cur);
    }

    if (id == 'DOWN') {
      final cur = moveCursorDown(prev.expression, prev.cursor);
      return prev.copyWith(cursor: cur);
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
          shiftActive: false,
          hypActive: false,
          stoMode: false,
        );
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

    final (expr, cur) = _applyInsert(prev, id);
    if (expr == null) {
      return prev.copyWith(
        shiftActive: false,
        hypActive: false,
        stoMode: false,
      );
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
        final (expr, cur) = insertDigit(e, c, id);
        return (expr, cur);

      case '.':
        final (expr, cur) = insertDecimalPoint(e, c);
        return (expr, cur);

      case 'plus':
        final (expr, cur) = insertOperator(e, c, '+');
        return (expr, cur);

      case 'minus':
        final (expr, cur) = insertOperator(e, c, '-');
        return (expr, cur);

      case 'multiply':
        final (expr, cur) = insertOperator(e, c, '×');
        return (expr, cur);

      case 'divide':
        final (expr, cur) = insertOperator(e, c, '÷');
        return (expr, cur);

      case 'fraction':
        final (expr, cur) = insertFraction(e, c);
        return (expr, cur);

      case 'exponent':
        final (expr, cur) = insertExponent(e, c);
        return (expr, cur);

      case 'sqrt':
        final (expr, cur) = insertRadical(e, c, false);
        return (expr, cur);

      case 'cbrt':
        final (expr, cur) = insertRadical(e, c, true);
        return (expr, cur);

      case 'square':
        final (expr, cur) = insertSquare(e, c);
        return (expr, cur);

      case 'cube':
        final (expr, cur) = insertCube(e, c);
        return (expr, cur);

      case 'nthRoot':
        final (expr, cur) = insertNthRadical(e, c);
        return (expr, cur);

      case 'mix':
        final (expr, cur) = insertMixed(e, c);
        return (expr, cur);

      case 'factorial':
        final (expr, cur) = insertFactorial(e, c);
        return (expr, cur);

      case 'ncr':
        final (expr, cur) = insertNcr(e, c);
        return (expr, cur);

      case 'npr':
        final (expr, cur) = insertNpr(e, c);
        return (expr, cur);

      case 'paren_open':
        final (expr, cur) = insertParen(e, c, 'open');
        return (expr, cur);

      case 'paren_close':
        final (expr, cur) = insertParen(e, c, 'close');
        return (expr, cur);

      case 'pi':
        final (expr, cur) = insertConstant(e, c, 'pi');
        return (expr, cur);

      case 'e_const':
        final (expr, cur) = insertConstant(e, c, 'e');
        return (expr, cur);

      case 'Ans':
        final (expr, cur) = insertConstant(e, c, 'Ans');
        return (expr, cur);

      case 'reciprocal':
        final (expr, cur) = insertReciprocal(e, c);
        return (expr, cur);

      case 'EXP':
        final (expr, cur) = insertExp(e, c);
        return (expr, cur);

      case 'negate':
        final (expr, cur) = insertNegate(e, c);
        return (expr, cur);

      case 'abs':
        final (expr, cur) = insertFunction(e, c, FunctionName.abs);
        return (expr, cur);

      case 'sin':
        final (expr, cur) = insertFunction(e, c, FunctionName.sin);
        return (expr, cur);

      case 'cos':
        final (expr, cur) = insertFunction(e, c, FunctionName.cos);
        return (expr, cur);

      case 'tan':
        final (expr, cur) = insertFunction(e, c, FunctionName.tan);
        return (expr, cur);

      case 'asin':
        final (expr, cur) = insertFunction(e, c, FunctionName.asin);
        return (expr, cur);

      case 'acos':
        final (expr, cur) = insertFunction(e, c, FunctionName.acos);
        return (expr, cur);

      case 'atan':
        final (expr, cur) = insertFunction(e, c, FunctionName.atan);
        return (expr, cur);

      case 'sinh':
        final (expr, cur) = insertFunction(e, c, FunctionName.sinh);
        return (expr, cur);

      case 'cosh':
        final (expr, cur) = insertFunction(e, c, FunctionName.cosh);
        return (expr, cur);

      case 'tanh':
        final (expr, cur) = insertFunction(e, c, FunctionName.tanh);
        return (expr, cur);

      case 'asinh':
        final (expr, cur) = insertFunction(e, c, FunctionName.asinh);
        return (expr, cur);

      case 'acosh':
        final (expr, cur) = insertFunction(e, c, FunctionName.acosh);
        return (expr, cur);

      case 'atanh':
        final (expr, cur) = insertFunction(e, c, FunctionName.atanh);
        return (expr, cur);

      case 'log':
        final (expr, cur) = insertFunction(e, c, FunctionName.log);
        return (expr, cur);

      case 'ln':
        final (expr, cur) = insertFunction(e, c, FunctionName.ln);
        return (expr, cur);

      case 'pow10':
        final (expr, cur) = insertFunction(e, c, FunctionName.pow10);
        return (expr, cur);

      case 'exp':
        final (expr, cur) = insertFunction(e, c, FunctionName.exp);
        return (expr, cur);

      default:
        return (null, null);
    }
  }

  void _persistHistory(
      List<ASTNode> expression, String result, List<HistoryEntry> current) {
    addEntry(current, expression, result).then((updated) {
      _state = _state.copyWith(history: updated);
      notifyListeners();
    });
  }
}
