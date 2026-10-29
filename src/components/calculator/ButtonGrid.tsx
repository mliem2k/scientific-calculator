import { cn } from '@/lib/utils'
import { Haptics, ImpactStyle } from '@capacitor/haptics'
import { useSettings } from '@/lib/settings'

type Role = 'digit' | 'fn' | 'op' | 'del' | 'eq'

interface BtnDef {
  id: string
  label: string
  shiftLabel?: string
  role: Role
  flex?: number
}

const ROWS: BtnDef[][] = [
  [
    { id: 'SHIFT', label: 'SHIFT', role: 'fn' },
    { id: 'HYP', label: 'HYP', role: 'fn' },
    { id: 'pi', label: 'π', role: 'fn' },
    { id: 'e_const', label: 'e', shiftLabel: 'eˣ', role: 'fn' },
    { id: 'Ans', label: 'Ans', role: 'fn' },
  ],
  [
    { id: 'Sto', label: 'Sto', shiftLabel: 'Paste Code', role: 'fn' },
    { id: 'exponent', label: 'xʸ', shiftLabel: 'ⁿ√', role: 'fn' },
    { id: 'sin', label: 'sin(', shiftLabel: 'sin⁻¹(', role: 'fn' },
    { id: 'cos', label: 'cos(', shiftLabel: 'cos⁻¹(', role: 'fn' },
    { id: 'tan', label: 'tan(', shiftLabel: 'tan⁻¹(', role: 'fn' },
  ],
  [
    { id: 'abs', label: 'Abs', role: 'fn' },
    { id: 'fraction', label: 'a/b', shiftLabel: 'mix', role: 'fn' },
    { id: 'sqrt', label: '√', shiftLabel: '³√', role: 'fn' },
    { id: 'log', label: 'log(', shiftLabel: 'a↔cd/e', role: 'fn' },
    { id: 'ln', label: 'ln(', shiftLabel: 'Dec↔a/b', role: 'fn' },
  ],
  [
    { id: 'factorial', label: 'x!', shiftLabel: 'A', role: 'fn' },
    { id: 'cube', label: 'x³', shiftLabel: 'B', role: 'fn' },
    { id: 'square', label: 'x²', shiftLabel: 'X', role: 'fn' },
    { id: 'paren_open', label: '(', role: 'fn' },
    { id: 'paren_close', label: ')', shiftLabel: 'Y', role: 'fn' },
  ],
  [
    { id: '7', label: '7', role: 'digit' },
    { id: '8', label: '8', role: 'digit' },
    { id: '9', label: '9', role: 'digit' },
    { id: 'DEL', label: 'DEL', role: 'del' },
    { id: 'AC', label: 'AC', role: 'del' },
  ],
  [
    { id: '4', label: '4', role: 'digit' },
    { id: '5', label: '5', role: 'digit' },
    { id: '6', label: '6', role: 'digit' },
    { id: 'multiply', label: '×', shiftLabel: 'nPr', role: 'op' },
    { id: 'divide', label: '÷', shiftLabel: 'nCr', role: 'op' },
  ],
  [
    { id: '1', label: '1', role: 'digit' },
    { id: '2', label: '2', role: 'digit' },
    { id: '3', label: '3', role: 'digit' },
    { id: 'plus', label: '+', role: 'op' },
    { id: 'minus', label: '−', role: 'op' },
  ],
  [
    { id: '0', label: '0', role: 'digit', flex: 2 },
    { id: '.', label: '.', shiftLabel: 'EXP', role: 'digit' },
    { id: '=', label: '=', shiftLabel: 'S⇔D', role: 'eq' },
  ],
]

const SHIFT_ACTION: Record<string, string> = {
  sin: 'asin', cos: 'acos', tan: 'atan', exponent: 'nthRoot', sqrt: 'cbrt',
  log: 'MixedFrac', ln: 'DecFrac', fraction: 'mix', multiply: 'npr', divide: 'ncr',
  '=': 'S_TO_D', factorial: 'A', cube: 'B', square: 'X', paren_close: 'Y',
  Sto: 'PasteCode', e_const: 'exp', '.': 'EXP',
}

// Plain names for screen readers; the shifted name differs where the key does.
const SPOKEN: Record<string, [string, string?]> = {
  SHIFT: ['Shift'], HYP: ['Hyperbolic'], pi: ['Pi'], e_const: ['e', 'e to the x'],
  Ans: ['Previous answer'], Sto: ['Store', 'Paste code'], exponent: ['Power', 'Nth root'],
  abs: ['Absolute value'], fraction: ['Fraction', 'Mixed fraction'],
  sqrt: ['Square root', 'Cube root'], log: ['Log', 'Mixed fraction toggle'],
  ln: ['Natural log', 'Decimal fraction toggle'], factorial: ['Factorial', 'Variable A'],
  cube: ['Cube', 'Variable B'], square: ['Square', 'Variable X'],
  paren_open: ['Open parenthesis'], paren_close: ['Close parenthesis', 'Variable Y'],
  DEL: ['Delete'], AC: ['All clear'], multiply: ['Multiply', 'Permutation'],
  divide: ['Divide', 'Combination'], plus: ['Plus'], minus: ['Minus'],
  '.': ['Decimal point', 'Exponent'], '=': ['Equals', 'Fraction decimal toggle'],
}

const TEXT: Record<Role, string> = {
  digit: 'text-digit', fn: 'text-fn', op: 'text-op', del: 'text-del', eq: 'text-on-eq',
}

interface Props {
  shiftActive: boolean
  onButton: (id: string) => void
  onPaste: () => void
}

export function ButtonGrid({ shiftActive, onButton, onPaste }: Props) {
  const { showShiftLabels, hapticFeedback } = useSettings()

  function handleClick(btn: BtnDef) {
    if (hapticFeedback) Haptics.impact({ style: ImpactStyle.Light }).catch(() => {})
    const id = shiftActive ? (SHIFT_ACTION[btn.id] ?? btn.id) : btn.id
    if (id === 'PasteCode') onPaste()
    else onButton(id)
  }

  return (
    <div className="grid h-full grid-rows-8 border-l border-t border-keyline bg-key" role="group" aria-label="Keypad">
      {ROWS.map((row, rowIdx) => (
        <div key={rowIdx} className="flex min-h-0">
          {row.map((btn) => {
            const label = shiftActive && btn.shiftLabel ? btn.shiftLabel : btn.label
            const spoken = SPOKEN[btn.id]
            const name = spoken ? (shiftActive && spoken[1] ? spoken[1] : spoken[0]) : label
            return (
              <button
                key={btn.id}
                aria-label={name}
                onClick={() => handleClick(btn)}
                style={{ flex: btn.flex ?? 1 }}
                className={cn(
                  'relative min-w-0 select-none border-b border-r border-keyline text-[1.375rem] font-bold leading-none',
                  'transition-colors duration-100',
                  btn.role === 'eq' ? 'bg-eq active:opacity-80' : 'bg-key active:bg-keyline',
                  TEXT[btn.role],
                )}
              >
                {showShiftLabels && !shiftActive && btn.shiftLabel && (
                  <span aria-hidden className="absolute left-1 right-1 top-0.5 truncate text-left text-xs font-medium text-dim">
                    {btn.shiftLabel}
                  </span>
                )}
                <span aria-hidden className="block truncate px-0.5">{label}</span>
              </button>
            )
          })}
        </div>
      ))}
    </div>
  )
}
