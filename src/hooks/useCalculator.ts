import { useState, useCallback } from 'react'
import { ASTNode, Cursor, HistoryEntry, CalculatorState, INITIAL_CURSOR, FunctionName } from '@/lib/ast/types'
import {
  insertDigit, insertDecimalPoint, insertOperator, insertFraction,
  insertExponent, insertRadical, insertSquare, insertFunction,
  insertConstant, insertFactorial, insertNcr, insertNpr, insertParen, deleteCurrent, clearAll,
} from '@/lib/ast/builder'
import { moveCursorLeft, moveCursorRight, moveCursorUp, moveCursorDown } from '@/lib/ast/cursor'
import { evaluate } from '@/lib/evaluator'
import { loadHistory, addEntry } from '@/lib/history'

type State = CalculatorState & { lastResult: string | null }

const INIT: State = {
  expression: [], cursor: INITIAL_CURSOR, result: null,
  angleMode: 'DEG', shiftActive: false, hypActive: false,
  history: loadHistory(), lastResult: null,
}

export function useCalculator() {
  const [state, setState] = useState<State>(INIT)

  const handleButton = useCallback((id: string) => {
    setState(prev => {
      const { expression: e, cursor: c, angleMode, lastResult, history, shiftActive, hypActive } = prev

      if (id === 'SHIFT')   return { ...prev, shiftActive: !shiftActive, hypActive: false }
      if (id === 'HYP')     return { ...prev, hypActive: !hypActive, shiftActive: false }
      if (id === 'DEG_RAD') return { ...prev, angleMode: angleMode === 'DEG' ? 'RAD' : 'DEG', shiftActive: false }

      const deshift = { shiftActive: false, hypActive: false }
      type Pair = [ASTNode[], Cursor]

      let pair: Pair
      switch (id) {
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
          pair = insertDigit(e, c, id); break
        case '.':        pair = insertDecimalPoint(e, c); break
        case 'plus':     pair = insertOperator(e, c, '+'); break
        case 'minus':    pair = insertOperator(e, c, '-'); break
        case 'multiply': pair = insertOperator(e, c, '×'); break
        case 'divide':   pair = insertOperator(e, c, '÷'); break
        case 'fraction': pair = insertFraction(e, c); break
        case 'exponent': pair = insertExponent(e, c); break
        case 'sqrt':     pair = insertRadical(e, c, false); break
        case 'cbrt':     pair = insertRadical(e, c, true); break
        case 'square':   pair = insertSquare(e, c); break
        case 'factorial': pair = insertFactorial(e, c); break
        case 'ncr':      pair = insertNcr(e, c); break
        case 'npr':      pair = insertNpr(e, c); break
        case 'paren_open':  pair = insertParen(e, c, 'open'); break
        case 'paren_close': pair = insertParen(e, c, 'close'); break
        case 'pi':  pair = insertConstant(e, c, 'pi'); break
        case 'Ans': pair = insertConstant(e, c, 'Ans'); break
        case 'sin': case 'cos': case 'tan':
        case 'asin': case 'acos': case 'atan':
        case 'sinh': case 'cosh': case 'tanh':
        case 'asinh': case 'acosh': case 'atanh':
        case 'log': case 'ln': case 'pow10': case 'exp':
          pair = insertFunction(e, c, id as FunctionName); break
        case 'DEL':   pair = deleteCurrent(e, c); break
        case 'CLEAR': pair = clearAll(); break
        case 'LEFT':  return { ...prev, cursor: moveCursorLeft(e, c) }
        case 'RIGHT': return { ...prev, cursor: moveCursorRight(e, c) }
        case 'UP':    return { ...prev, cursor: moveCursorUp(e, c) }
        case 'DOWN':  return { ...prev, cursor: moveCursorDown(e, c) }
        case '=': {
          const evalResult = evaluate(e, angleMode, lastResult)
          if (!evalResult) return { ...prev, ...deshift }
          const newHistory = addEntry(history, e, evalResult.value)
          return { ...prev, ...deshift, history: newHistory, result: evalResult.value, lastResult: evalResult.value }
        }
        default: return prev
      }

      const [newExpr, newCursor] = pair
      const evalResult = evaluate(newExpr, angleMode, lastResult)
      return { ...prev, ...deshift, expression: newExpr, cursor: newCursor, result: evalResult?.value ?? null }
    })
  }, [])

  const handleRestore = useCallback((entry: HistoryEntry) => {
    setState(prev => ({
      ...prev,
      expression: entry.expression,
      cursor: { path: [], insertAt: entry.expression.length },
      result: entry.result,
      shiftActive: false,
      hypActive: false,
    }))
  }, [])

  return { state, handleButton, handleRestore }
}
