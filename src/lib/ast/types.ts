export type NumberNode    = { type: 'number';    value: string }
export type OperatorNode  = { type: 'operator';  op: '+' | '-' | '×' | '÷' }
export type ConstantNode  = { type: 'constant';  name: 'pi' | 'e' | 'Ans' }
export type FractionNode  = { type: 'fraction';  numerator: ASTNode[]; denominator: ASTNode[] }
export type ExponentNode  = { type: 'exponent';  base: ASTNode[];      exponent: ASTNode[] }
export type RadicalNode   = { type: 'radical';   degree: ASTNode[];    radicand: ASTNode[] }
export type FunctionNode  = { type: 'function';  name: FunctionName;   argument: ASTNode[] }
export type FactorialNode = { type: 'factorial'; operand: ASTNode[] }
export type NcrNode       = { type: 'ncr';       n: ASTNode[];         r: ASTNode[] }
export type NprNode       = { type: 'npr';       n: ASTNode[];         r: ASTNode[] }
export type ParenNode     = { type: 'paren';     children: ASTNode[] }

export type FunctionName =
  | 'sin' | 'cos' | 'tan'
  | 'asin' | 'acos' | 'atan'
  | 'sinh' | 'cosh' | 'tanh'
  | 'asinh' | 'acosh' | 'atanh'
  | 'log' | 'ln' | 'pow10' | 'exp'

export type ASTNode =
  | NumberNode | OperatorNode | ConstantNode
  | FractionNode | ExponentNode | RadicalNode
  | FunctionNode | FactorialNode | NcrNode | NprNode | ParenNode

export interface CursorSegment {
  nodeIndex: number
  slot: string
}

export interface Cursor {
  path: CursorSegment[]
  insertAt: number
}

export const INITIAL_CURSOR: Cursor = { path: [], insertAt: 0 }

export interface HistoryEntry {
  id: string
  expression: ASTNode[]
  result: string
  timestamp: number
}

export interface CalculatorState {
  expression: ASTNode[]
  cursor: Cursor
  result: string | null
  resultMode: 'decimal' | 'fraction'
  angleMode: 'DEG' | 'RAD'
  shiftActive: boolean
  hypActive: boolean
  history: HistoryEntry[]
  lastResult: string | null
}
