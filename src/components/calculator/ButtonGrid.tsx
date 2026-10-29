import { cn } from '@/lib/utils'
import { Haptics, ImpactStyle } from '@capacitor/haptics'

type Role = 'digit' | 'util' | 'op' | 'sci'

interface BtnDef {
  id: string
  label: string
  shiftLabel?: string
  role: Role
  wide?: boolean
}

const SCI: BtnDef[][] = [
  [
    { id: 'SHIFT', label: '2nd', role: 'sci' },
    { id: 'HYP', label: 'hyp', role: 'sci' },
    { id: 'pi', label: 'π', role: 'sci' },
    { id: 'e_const', label: 'e', shiftLabel: 'eˣ', role: 'sci' },
    { id: 'Ans', label: 'Ans', role: 'sci' },
  ],
  [
    { id: 'Sto', label: 'Sto', shiftLabel: 'Paste', role: 'sci' },
    { id: 'exponent', label: 'xʸ', shiftLabel: 'ⁿ√', role: 'sci' },
    { id: 'sin', label: 'sin', shiftLabel: 'sin⁻¹', role: 'sci' },
    { id: 'cos', label: 'cos', shiftLabel: 'cos⁻¹', role: 'sci' },
    { id: 'tan', label: 'tan', shiftLabel: 'tan⁻¹', role: 'sci' },
  ],
  [
    { id: 'abs', label: '|x|', role: 'sci' },
    { id: 'factorial', label: 'x!', shiftLabel: 'A', role: 'sci' },
    { id: 'sqrt', label: '√', shiftLabel: '³√', role: 'sci' },
    { id: 'log', label: 'log', shiftLabel: 'a b/c', role: 'sci' },
    { id: 'ln', label: 'ln', shiftLabel: 'F⇄D', role: 'sci' },
  ],
  [
    { id: 'square', label: 'x²', shiftLabel: 'X', role: 'sci' },
    { id: 'cube', label: 'x³', shiftLabel: 'B', role: 'sci' },
    { id: 'paren_open', label: '(', role: 'sci' },
    { id: 'paren_close', label: ')', shiftLabel: 'Y', role: 'sci' },
    { id: 'EXP', label: 'EXP', role: 'sci' },
  ],
]

const PAD: BtnDef[][] = [
  [
    { id: 'AC', label: 'AC', role: 'util' },
    { id: 'DEL', label: '⌫', role: 'util' },
    { id: 'fraction', label: 'a/b', shiftLabel: 'mix', role: 'util' },
    { id: 'divide', label: '÷', shiftLabel: 'nCr', role: 'op' },
  ],
  [
    { id: '7', label: '7', role: 'digit' },
    { id: '8', label: '8', role: 'digit' },
    { id: '9', label: '9', role: 'digit' },
    { id: 'multiply', label: '×', shiftLabel: 'nPr', role: 'op' },
  ],
  [
    { id: '4', label: '4', role: 'digit' },
    { id: '5', label: '5', role: 'digit' },
    { id: '6', label: '6', role: 'digit' },
    { id: 'minus', label: '−', role: 'op' },
  ],
  [
    { id: '1', label: '1', role: 'digit' },
    { id: '2', label: '2', role: 'digit' },
    { id: '3', label: '3', role: 'digit' },
    { id: 'plus', label: '+', role: 'op' },
  ],
  [
    { id: '0', label: '0', role: 'digit', wide: true },
    { id: '.', label: '.', role: 'digit' },
    { id: '=', label: '=', shiftLabel: 'S⇄D', role: 'op' },
  ],
]

const SHIFT_ACTION: Record<string, string> = {
  sin: 'asin', cos: 'acos', tan: 'atan', exponent: 'nthRoot', sqrt: 'cbrt',
  log: 'MixedFrac', ln: 'DecFrac', fraction: 'mix', multiply: 'npr', divide: 'ncr',
  '=': 'S_TO_D', factorial: 'A', cube: 'B', square: 'X', paren_close: 'Y',
  Sto: 'PasteCode', e_const: 'exp',
}
const HYP_ACTION: Record<string, string> = { sin: 'sinh', cos: 'cosh', tan: 'tanh' }
const HYP_LABEL: Record<string, string> = { sin: 'sinh', cos: 'cosh', tan: 'tanh' }

// Plain names for screen readers; the shifted name differs where the key does.
const SPOKEN: Record<string, [string, string?]> = {
  SHIFT: ['Second function'], HYP: ['Hyperbolic'], pi: ['Pi'], e_const: ['e', 'e to the x'],
  Ans: ['Previous answer'], Sto: ['Store', 'Paste code'], exponent: ['Power', 'Nth root'],
  abs: ['Absolute value'], fraction: ['Fraction', 'Mixed fraction'],
  sqrt: ['Square root', 'Cube root'], log: ['Log', 'Mixed fraction toggle'],
  ln: ['Natural log', 'Decimal fraction toggle'], factorial: ['Factorial', 'Variable A'],
  cube: ['Cube', 'Variable B'], square: ['Square', 'Variable X'],
  paren_open: ['Open parenthesis'], paren_close: ['Close parenthesis', 'Variable Y'],
  DEL: ['Delete'], AC: ['All clear'], multiply: ['Multiply', 'Permutation'],
  divide: ['Divide', 'Combination'], plus: ['Plus'], minus: ['Minus'], EXP: ['Exponent'],
  '.': ['Decimal point'], '=': ['Equals', 'Fraction decimal toggle'],
}

const ROLE: Record<Role, string> = {
  digit: 'bg-digit text-on-digit',
  util: 'bg-util text-on-util',
  op: 'bg-op text-on-op',
  sci: 'bg-sci text-on-sci',
}

// Textbook fraction glyph: box over bar over box, sized in em so it scales with the key text.
function FractionIcon({ mixed }: { mixed: boolean }) {
  const box = 'rounded-[0.08em] border-[0.07em] border-current'
  return (
    <span aria-hidden className="inline-flex items-center gap-[0.18em]">
      {mixed && <span className={cn(box, 'h-[0.95em] w-[0.5em]')} />}
      <span className="inline-flex flex-col items-center gap-[0.1em]">
        <span className={cn(box, 'h-[0.42em] w-[0.55em]')} />
        <span className="h-[0.07em] w-[0.75em] bg-current" />
        <span className={cn(box, 'h-[0.42em] w-[0.55em]')} />
      </span>
    </span>
  )
}

interface Props {
  shiftActive: boolean
  hypActive: boolean
  stoMode: boolean
  onButton: (id: string) => void
  onPaste: () => void
}

export function ButtonGrid({ shiftActive, hypActive, stoMode, onButton, onPaste }: Props) {
  function handleClick(btn: BtnDef) {
    Haptics.impact({ style: ImpactStyle.Light }).catch(() => {})
    let id = btn.id
    if (shiftActive) id = SHIFT_ACTION[id] ?? id
    else if (hypActive) id = HYP_ACTION[id] ?? id
    if (id === 'PasteCode') onPaste()
    else onButton(id)
  }

  function renderKey(btn: BtnDef, size: 'sci' | 'pad') {
    const label = shiftActive && btn.shiftLabel
      ? btn.shiftLabel
      : hypActive && HYP_LABEL[btn.id] ? HYP_LABEL[btn.id] : btn.label
    const spoken = SPOKEN[btn.id]
    const name = spoken ? (shiftActive && spoken[1] ? spoken[1] : spoken[0]) : label
    const lit = (btn.id === 'SHIFT' && shiftActive) || (btn.id === 'HYP' && hypActive) || (btn.id === 'Sto' && stoMode)
    const text = size === 'pad'
      ? (btn.id === 'fraction' ? 'text-[2.6rem]' : label.length > 2 ? 'text-[1.375rem]' : 'text-[2rem]')
      : 'text-[1.0625rem]'
    return (
      <button
        key={btn.id}
        aria-label={name}
        aria-pressed={btn.id === 'SHIFT' || btn.id === 'HYP' || btn.id === 'Sto' ? lit : undefined}
        onClick={() => handleClick(btn)}
        className={cn(
          'key flex select-none items-center justify-center rounded-full font-normal leading-none',
          ROLE[btn.role], text,
          lit && 'bg-ink text-bg',
          btn.wide && 'col-span-2 justify-start pl-[calc(var(--k)*0.38)]',
          size === 'sci' && 'min-w-0 font-medium',
        )}
      >
        {btn.id === 'fraction' ? <FractionIcon mixed={shiftActive} /> : label}
      </button>
    )
  }

  return (
    <div className="flex min-h-0 shrink-0 flex-col gap-2.5 px-4 pb-2">
      <div role="group" aria-label="Scientific functions" className="grid grid-cols-5 gap-x-2 gap-y-1.5 [grid-auto-rows:clamp(2rem,4.8dvh,2.75rem)]">
        {SCI.flat().map((b) => renderKey(b, 'sci'))}
      </div>
      <div role="group" aria-label="Number pad" className="pad-frame">
        <div className="pad">{PAD.flat().map((b) => renderKey(b, 'pad'))}</div>
      </div>
    </div>
  )
}
