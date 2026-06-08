import { useState, useCallback } from 'react'
import { ASTNode, Cursor, CursorSegment, HistoryEntry, CalculatorState, MemorySlots, INITIAL_CURSOR, FunctionName } from '@/lib/ast/types'
import {
  insertDigit, insertDecimalPoint, insertOperator, insertFraction,
  insertExponent, insertRadical, insertSquare, insertFunction,
  insertConstant, insertFactorial, insertNcr, insertNpr, insertParen,
  insertReciprocal, insertExp, insertNegate, deleteCurrent, clearAll,
  insertCube, insertNthRadical, insertMixed, insertNumberLiteral,
} from '@/lib/ast/builder'
import { moveCursorLeft, moveCursorRight, moveCursorUp, moveCursorDown } from '@/lib/ast/cursor'
import { evaluate, toFraction, toMixed, fromMixed } from '@/lib/evaluator'
import { loadHistory, addEntry } from '@/lib/history'

type State = CalculatorState & { lastResult: string | null }

const INIT: State = {
  expression: [], cursor: INITIAL_CURSOR, result: null, resultMode: 'decimal',
  angleMode: 'DEG', shiftActive: false, hypActive: false,
  history: loadHistory(), lastResult: null,
  memory: { A: null, B: null, X: null, Y: null },
  stoMode: false,
}

export function useCalculator() {
  const [state, setState] = useState<State>(INIT)

  const handleButton = useCallback((id: string) => {
    setState(prev => {
      const { expression: e, cursor: c, angleMode, lastResult, history, shiftActive, hypActive, memory } = prev

      if (id === 'SHIFT')   return { ...prev, shiftActive: !shiftActive, hypActive: false }
      if (id === 'HYP')     return { ...prev, hypActive: !hypActive, shiftActive: false }
      if (id === 'DEG_RAD') return { ...prev, angleMode: angleMode === 'DEG' ? 'RAD' : 'DEG', shiftActive: false }

      if (id === 'Sto') return { ...prev, stoMode: true, shiftActive: false, hypActive: false }

      if (id === 'A' || id === 'B' || id === 'X' || id === 'Y') {
        const deshift = { shiftActive: false, hypActive: false, stoMode: false }
        if (prev.stoMode) {
          const val = prev.result !== null ? parseFloat(prev.result) : NaN
          if (!isNaN(val)) {
            return { ...prev, ...deshift, memory: { ...memory, [id]: val } }
          }
          return { ...prev, ...deshift }
        }
        const [newExpr, newCursor] = insertConstant(e, c, id as 'A' | 'B' | 'X' | 'Y')
        const evalResult = evaluate(newExpr, angleMode, lastResult, memory)
        return { ...prev, ...deshift, expression: newExpr, cursor: newCursor, result: evalResult?.value ?? null, resultMode: 'decimal' }
      }

      const deshift = { shiftActive: false, hypActive: false, stoMode: false }
      type Pair = [ASTNode[], Cursor]

      if (id === 'S_TO_D' || id === 'DecFrac') {
        if (!prev.result) return { ...prev, ...deshift }
        if (prev.resultMode === 'fraction') {
          return { ...prev, ...deshift, result: prev.lastResult, resultMode: 'decimal' }
        }
        const fracStr = toFraction(prev.result)
        if (!fracStr) return { ...prev, ...deshift }
        return { ...prev, ...deshift, lastResult: prev.result, result: fracStr, resultMode: 'fraction' }
      }

      if (id === 'MixedFrac') {
        if (!prev.result) return { ...prev, ...deshift }
        const mixed = toMixed(prev.result)
        if (mixed) return { ...prev, ...deshift, result: mixed, resultMode: 'fraction' }
        const improper = fromMixed(prev.result)
        if (improper) return { ...prev, ...deshift, result: improper, resultMode: 'fraction' }
        return { ...prev, ...deshift }
      }

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
        case 'cube':     pair = insertCube(e, c); break
        case 'nthRoot':  pair = insertNthRadical(e, c); break
        case 'mix':      pair = insertMixed(e, c); break
        case 'factorial': pair = insertFactorial(e, c); break
        case 'ncr':      pair = insertNcr(e, c); break
        case 'npr':      pair = insertNpr(e, c); break
        case 'paren_open':  pair = insertParen(e, c, 'open'); break
        case 'paren_close': pair = insertParen(e, c, 'close'); break
        case 'pi':         pair = insertConstant(e, c, 'pi'); break
        case 'e_const':    pair = insertConstant(e, c, 'e'); break
        case 'Ans':        pair = insertConstant(e, c, 'Ans'); break
        case 'reciprocal': pair = insertReciprocal(e, c); break
        case 'EXP':        pair = insertExp(e, c); break
        case 'negate':     pair = insertNegate(e, c); break
        case 'abs':
        case 'sin': case 'cos': case 'tan':
        case 'asin': case 'acos': case 'atan':
        case 'sinh': case 'cosh': case 'tanh':
        case 'asinh': case 'acosh': case 'atanh':
        case 'log': case 'ln': case 'pow10': case 'exp':
          pair = insertFunction(e, c, id as FunctionName); break
        case 'DEL':   pair = deleteCurrent(e, c); break
        case 'CLEAR': case 'AC': pair = clearAll(); break
        case 'LEFT':  return { ...prev, cursor: moveCursorLeft(e, c) }
        case 'RIGHT': return { ...prev, cursor: moveCursorRight(e, c) }
        case 'UP':    return { ...prev, cursor: moveCursorUp(e, c) }
        case 'DOWN':  return { ...prev, cursor: moveCursorDown(e, c) }
        case '=': {
          const evalResult = evaluate(e, angleMode, lastResult, memory)
          if (!evalResult) return { ...prev, ...deshift }
          const newHistory = addEntry(history, e, evalResult.value)
          return {
            ...prev, ...deshift,
            history: newHistory,
            result: evalResult.value,
            resultMode: 'decimal',
            lastResult: evalResult.value,
          }
        }
        default: return { ...prev, ...deshift }
      }

      const [newExpr, newCursor] = pair
      const evalResult = evaluate(newExpr, angleMode, lastResult, memory)
      return {
        ...prev, ...deshift,
        expression: newExpr,
        cursor: newCursor,
        result: evalResult?.value ?? null,
        resultMode: 'decimal',
      }
    })
  }, [])

  const handleCursorJump = useCallback((path: CursorSegment[], insertAt: number) => {
    setState(prev => ({ ...prev, cursor: { path, insertAt } }))
  }, [])

  const handlePaste = useCallback(async () => {
    try {
      const text = await navigator.clipboard.readText()
      const trimmed = text.trim()
      if (!trimmed || !/^-?[\d]+(\.[\d]+)?([eE][+-]?[\d]+)?$/.test(trimmed)) return
      setState(prev => {
        const [newExpr, newCursor] = insertNumberLiteral(prev.expression, prev.cursor, trimmed)
        const evalResult = evaluate(newExpr, prev.angleMode, prev.lastResult, prev.memory)
        return {
          ...prev, shiftActive: false, hypActive: false, stoMode: false,
          expression: newExpr, cursor: newCursor,
          result: evalResult?.value ?? null, resultMode: 'decimal',
        }
      })
    } catch { /* clipboard access denied */ }
  }, [])

  const handleRestore = useCallback((entry: HistoryEntry) => {
    setState(prev => ({
      ...prev,
      expression: entry.expression,
      cursor: { path: [], insertAt: entry.expression.length },
      result: entry.result,
      resultMode: 'decimal',
      shiftActive: false,
      hypActive: false,
      stoMode: false,
    }))
  }, [])

  return { state, handleButton, handleCursorJump, handlePaste, handleRestore }
}
