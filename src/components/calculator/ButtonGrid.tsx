import { cn } from '@/lib/utils'
import { DPad } from './DPad'

interface BtnDef {
  id: string; label: string; shiftLabel?: string; hypLabel?: string; shiftHypLabel?: string; cls?: string
}

// Row 0 handled separately — col 1-2 are SHIFT/DEG_RAD, col 3-4 are the DPad (col-span-2)
const TOP_ROW: BtnDef[] = [
  { id: 'SHIFT',   label: 'SHIFT',  cls: 'bg-orange-700' },
  { id: 'DEG_RAD', label: 'DEG' },
]

const ROWS: (BtnDef | null)[][] = [
  [
    { id: 'log', label: 'log', shiftLabel: '10ˣ' },
    { id: 'ln',  label: 'ln',  shiftLabel: 'eˣ'  },
    { id: 'HYP', label: 'hyp' },
    { id: 'factorial', label: 'x!' },
  ],
  [
    { id: 'sin', label: 'sin', shiftLabel: 'sin⁻¹', hypLabel: 'sinh', shiftHypLabel: 'sinh⁻¹' },
    { id: 'cos', label: 'cos', shiftLabel: 'cos⁻¹', hypLabel: 'cosh', shiftHypLabel: 'cosh⁻¹' },
    { id: 'tan', label: 'tan', shiftLabel: 'tan⁻¹', hypLabel: 'tanh', shiftHypLabel: 'tanh⁻¹' },
    { id: 'exponent', label: 'xʸ' },
  ],
  [
    { id: 'sqrt',    label: '√',   shiftLabel: 'x²' },
    { id: 'cbrt',    label: '∛' },
    { id: 'fraction', label: 'a/b' },
    { id: 'square',  label: 'x²' },
  ],
  [
    { id: 'ncr',         label: 'nCr' },
    { id: 'npr',         label: 'nPr' },
    { id: 'paren_open',  label: '('   },
    { id: 'paren_close', label: ')'   },
  ],
  [
    { id: '7', label: '7' }, { id: '8', label: '8' }, { id: '9', label: '9' },
    { id: 'DEL', label: 'DEL', cls: 'bg-zinc-700' },
  ],
  [
    { id: '4', label: '4' }, { id: '5', label: '5' }, { id: '6', label: '6' },
    { id: 'multiply', label: '×' },
  ],
  [
    { id: '1', label: '1' }, { id: '2', label: '2' }, { id: '3', label: '3' },
    { id: 'divide', label: '÷' },
  ],
  [
    { id: '0', label: '0' }, { id: '.', label: '.' }, { id: 'Ans', label: 'Ans' },
    { id: 'plus', label: '+' },
  ],
  [
    null,
    { id: '=', label: '=', cls: 'bg-blue-700' },
    null,
    { id: 'minus', label: '−' },
  ],
]

interface Props {
  angleMode: 'DEG' | 'RAD'
  shiftActive: boolean
  hypActive: boolean
  onButton: (id: string) => void
}

export function ButtonGrid({ angleMode, shiftActive, hypActive, onButton }: Props) {
  function label(btn: BtnDef): string {
    if (btn.id === 'DEG_RAD') return angleMode
    if (shiftActive && hypActive && btn.shiftHypLabel) return btn.shiftHypLabel
    if (shiftActive && btn.shiftLabel) return btn.shiftLabel
    if (hypActive && btn.hypLabel) return btn.hypLabel
    return btn.label
  }

  function actionId(btn: BtnDef): string {
    if (btn.id === 'sin') {
      if (shiftActive && hypActive) return 'asinh'
      if (shiftActive) return 'asin'
      if (hypActive)   return 'sinh'
      return 'sin'
    }
    if (btn.id === 'cos') {
      if (shiftActive && hypActive) return 'acosh'
      if (shiftActive) return 'acos'
      if (hypActive)   return 'cosh'
      return 'cos'
    }
    if (btn.id === 'tan') {
      if (shiftActive && hypActive) return 'atanh'
      if (shiftActive) return 'atan'
      if (hypActive)   return 'tanh'
      return 'tan'
    }
    if (btn.id === 'log'  && shiftActive) return 'pow10'
    if (btn.id === 'ln'   && shiftActive) return 'exp'
    if (btn.id === 'sqrt' && shiftActive) return 'square'
    return btn.id
  }

  function renderBtn(btn: BtnDef | null, i: number) {
    if (!btn) return <div key={i} />
    return (
      <button
        key={btn.id + i}
        onClick={() => onButton(actionId(btn))}
        className={cn(
          'h-12 rounded text-white text-sm font-medium active:scale-95 transition-transform select-none',
          btn.cls ?? 'bg-zinc-800 hover:bg-zinc-700',
          btn.id === 'SHIFT' && shiftActive && 'ring-1 ring-orange-400',
          btn.id === 'HYP'   && hypActive   && 'ring-1 ring-blue-400',
        )}
      >
        {label(btn)}
      </button>
    )
  }

  return (
    <div className="grid grid-cols-4 gap-1 p-2 bg-zinc-950 flex-shrink-0">
      {/* Top row: SHIFT + DEG/RAD take cols 1-2, DPad spans cols 3-4 */}
      {TOP_ROW.map((btn, i) => renderBtn(btn, i))}
      <DPad onButton={onButton} />

      {/* Remaining rows */}
      {ROWS.flat().map((btn, i) => renderBtn(btn, i + TOP_ROW.length))}
    </div>
  )
}
