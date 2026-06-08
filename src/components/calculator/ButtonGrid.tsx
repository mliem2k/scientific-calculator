import { cn } from '@/lib/utils'
import { DPad } from './DPad'

// Button class presets by category
const DIGIT = 'bg-zinc-900 border border-zinc-700/70 text-white'
const FN    = 'bg-zinc-950 border border-zinc-800 text-zinc-300'
const OP    = 'bg-zinc-900 border border-zinc-700/70 text-sky-300'
const DEL   = 'bg-zinc-900 border border-zinc-700/70 text-red-400'
const ANS   = 'bg-zinc-900 border border-zinc-700/70 text-amber-300'
const EQ    = 'bg-blue-700 border border-blue-600 text-white'
const CTRL  = 'bg-zinc-800 border border-zinc-700 text-zinc-200'
const SHIFT_BTN = 'bg-orange-700 border border-orange-600 text-white'

interface BtnDef {
  id: string
  label: string
  shiftLabel?: string
  hypLabel?: string
  shiftHypLabel?: string
  cls: string
}

// Top row: SHIFT + DEG_RAD rendered beside DPad (col-span-2)
const TOP_ROW: BtnDef[] = [
  { id: 'SHIFT',   label: 'SHIFT', cls: SHIFT_BTN },
  { id: 'DEG_RAD', label: 'DEG',   cls: CTRL },
]

// FX-991ES function area — 4 rows × 4 columns
// Row 1: log/exp group + constants (π, e)
// Row 2: trig + HYP modifier (Casio groups these together)
// Row 3: power/root group — x² standalone, √ (SHIFT:∛), fraction (SHIFT:x⁻¹), x^y
// Row 4: combinatorics (nCr SHIFT:nPr), factorial, parentheses
const ROWS: BtnDef[][] = [
  [
    { id: 'log',     label: 'log',  shiftLabel: '10ˣ',   cls: FN },
    { id: 'ln',      label: 'ln',   shiftLabel: 'eˣ',    cls: FN },
    { id: 'pi',      label: 'π',                         cls: FN },
    { id: 'e_const', label: 'e',                         cls: FN },
  ],
  [
    { id: 'sin', label: 'sin', shiftLabel: 'sin⁻¹', hypLabel: 'sinh', shiftHypLabel: 'sinh⁻¹', cls: FN },
    { id: 'cos', label: 'cos', shiftLabel: 'cos⁻¹', hypLabel: 'cosh', shiftHypLabel: 'cosh⁻¹', cls: FN },
    { id: 'tan', label: 'tan', shiftLabel: 'tan⁻¹', hypLabel: 'tanh', shiftHypLabel: 'tanh⁻¹', cls: FN },
    { id: 'HYP', label: 'hyp',                                                                   cls: FN },
  ],
  [
    { id: 'square',   label: 'x²',                      cls: FN },
    { id: 'sqrt',     label: '√',   shiftLabel: '∛',    cls: FN },
    { id: 'fraction', label: 'a/b', shiftLabel: 'x⁻¹',  cls: FN },
    { id: 'exponent', label: 'xʸ',                      cls: FN },
  ],
  [
    { id: 'ncr',         label: 'nCr', shiftLabel: 'nPr', cls: FN },
    { id: 'factorial',   label: 'x!',                     cls: FN },
    { id: 'paren_open',  label: '(',                      cls: FN },
    { id: 'paren_close', label: ')',                      cls: FN },
  ],
  [
    { id: '7',   label: '7',   cls: DIGIT },
    { id: '8',   label: '8',   cls: DIGIT },
    { id: '9',   label: '9',   cls: DIGIT },
    { id: 'DEL', label: 'DEL', shiftLabel: 'AC', cls: DEL },
  ],
  [
    { id: '4',        label: '4', cls: DIGIT },
    { id: '5',        label: '5', cls: DIGIT },
    { id: '6',        label: '6', cls: DIGIT },
    { id: 'multiply', label: '×', cls: OP    },
  ],
  [
    { id: '1',      label: '1', cls: DIGIT },
    { id: '2',      label: '2', cls: DIGIT },
    { id: '3',      label: '3', cls: DIGIT },
    { id: 'divide', label: '÷', cls: OP    },
  ],
  [
    { id: '0',   label: '0',   cls: DIGIT },
    { id: '.',   label: '.',   cls: DIGIT },
    { id: 'Ans', label: 'Ans', cls: ANS   },
    { id: 'plus', label: '+',  cls: OP    },
  ],
  [
    { id: 'CLEAR', label: 'AC', cls: DEL  },
    { id: '=',     label: '=',  cls: EQ   },
    { id: '_gap',  label: '',   cls: 'invisible' },
    { id: 'minus', label: '−',  cls: OP   },
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
    if (hypActive   && btn.hypLabel)   return btn.hypLabel
    return btn.label
  }

  // Show SHIFT alt-label as a small legend only when SHIFT is not active
  function secondaryLabel(btn: BtnDef): string | null {
    if (!shiftActive && !hypActive && btn.shiftLabel) return btn.shiftLabel
    return null
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
    if (btn.id === 'log'      && shiftActive) return 'pow10'
    if (btn.id === 'ln'       && shiftActive) return 'exp'
    if (btn.id === 'sqrt'     && shiftActive) return 'cbrt'
    if (btn.id === 'fraction' && shiftActive) return 'reciprocal'
    if (btn.id === 'ncr'      && shiftActive) return 'npr'
    if (btn.id === 'DEL'      && shiftActive) return 'CLEAR'
    return btn.id
  }

  function renderBtn(btn: BtnDef, i: number) {
    if (btn.id === '_gap') return <div key={btn.id + i} />

    const secondary = secondaryLabel(btn)

    return (
      <button
        key={btn.id + i}
        onClick={() => onButton(actionId(btn))}
        className={cn(
          'rounded text-sm font-medium select-none',
          'active:scale-95 active:brightness-75 transition-all duration-75',
          secondary
            ? 'h-12 flex flex-col items-center justify-center gap-0'
            : 'h-11 flex items-center justify-center',
          btn.cls,
          btn.id === 'SHIFT' && shiftActive && 'ring-1 ring-orange-300 brightness-125',
          btn.id === 'HYP'   && hypActive   && 'ring-1 ring-indigo-300 brightness-125',
        )}
      >
        {secondary && (
          <span className="text-[7px] text-orange-400 leading-none mb-0.5 font-normal tracking-wide">
            {secondary}
          </span>
        )}
        <span className="leading-none">{label(btn)}</span>
      </button>
    )
  }

  return (
    <div className="grid grid-cols-4 gap-[3px] p-2 bg-zinc-950 flex-shrink-0">
      {/* Top row: SHIFT + DEG_RAD in cols 1-2, DPad spans cols 3-4 */}
      {TOP_ROW.map((btn, i) => renderBtn(btn, i))}
      <DPad onButton={onButton} />

      {ROWS.flat().map((btn, i) => renderBtn(btn, i + TOP_ROW.length))}
    </div>
  )
}
