import { cn } from '@/lib/utils'

const DIGIT = 'bg-zinc-900 border border-zinc-700/70 text-white'
const FN    = 'bg-zinc-950 border border-zinc-800 text-zinc-300'
const OP    = 'bg-zinc-900 border border-zinc-700/70 text-sky-300'
const DEL   = 'bg-zinc-900 border border-zinc-700/70 text-red-400'
const AC    = 'bg-zinc-900 border border-zinc-700/70 text-red-500 font-semibold'
const EQ    = 'bg-blue-700 border border-blue-600 text-white'

interface BtnDef {
  id: string
  label: string
  shiftLabel?: string
  cls: string
}

const ROWS: BtnDef[][] = [
  // Row 1: sto/root/trig
  [
    { id: 'Sto',         label: 'Sto',    shiftLabel: 'PasteCode', cls: FN },
    { id: 'exponent',    label: 'xʸ',     shiftLabel: 'ⁿ√',        cls: FN },
    { id: 'sin',         label: 'sin(',   shiftLabel: 'sin⁻¹(',    cls: FN },
    { id: 'cos',         label: 'cos(',   shiftLabel: 'cos⁻¹(',    cls: FN },
    { id: 'tan',         label: 'tan(',   shiftLabel: 'tan⁻¹(',    cls: FN },
  ],
  // Row 2: abs/fraction/root/log/ln
  [
    { id: 'abs',         label: 'Abs',                              cls: FN },
    { id: 'fraction',    label: 'a/b',    shiftLabel: 'mix',        cls: FN },
    { id: 'sqrt',        label: '√',      shiftLabel: '³√',         cls: FN },
    { id: 'log',         label: 'log(',   shiftLabel: 'a/b↔cd/e',   cls: FN },
    { id: 'ln',          label: 'ln(',    shiftLabel: 'Dec↔a/b',    cls: FN },
  ],
  // Row 3: combinatorics / power / paren
  [
    { id: 'factorial',   label: 'x!',     shiftLabel: 'A',          cls: FN },
    { id: 'cube',        label: 'x³',     shiftLabel: 'B',          cls: FN },
    { id: 'square',      label: 'x²',     shiftLabel: 'X',          cls: FN },
    { id: 'paren_open',  label: '(',                                cls: FN },
    { id: 'paren_close', label: ')',       shiftLabel: 'Y',          cls: FN },
  ],
  // Row 4: 7 8 9 DEL AC
  [
    { id: '7',   label: '7',   cls: DIGIT },
    { id: '8',   label: '8',   cls: DIGIT },
    { id: '9',   label: '9',   cls: DIGIT },
    { id: 'DEL', label: 'DEL', cls: DEL   },
    { id: 'AC',  label: 'AC',  cls: AC    },
  ],
  // Row 5: 4 5 6 × ÷
  [
    { id: '4',        label: '4',  cls: DIGIT },
    { id: '5',        label: '5',  cls: DIGIT },
    { id: '6',        label: '6',  cls: DIGIT },
    { id: 'multiply', label: '×',  shiftLabel: 'nPr', cls: OP },
    { id: 'divide',   label: '÷',  shiftLabel: 'nCr', cls: OP },
  ],
  // Row 6: 1 2 3 + −
  [
    { id: '1',     label: '1', cls: DIGIT },
    { id: '2',     label: '2', cls: DIGIT },
    { id: '3',     label: '3', cls: DIGIT },
    { id: 'plus',  label: '+', cls: OP    },
    { id: 'minus', label: '−', cls: OP    },
  ],
  // Row 7: 0 . EXP = (−)
  [
    { id: '0',      label: '0',   cls: DIGIT },
    { id: '.',      label: '.',   cls: DIGIT },
    { id: 'EXP',    label: 'EXP',              cls: FN    },
    { id: '=',      label: '=',   shiftLabel: 'S⇔D', cls: EQ },
    { id: 'negate', label: '(−)',              cls: DIGIT },
  ],
]

interface Props {
  shiftActive: boolean
  onButton: (id: string) => void
  onPaste: () => void
}

export function ButtonGrid({ shiftActive, onButton, onPaste }: Props) {
  function displayLabel(btn: BtnDef): string {
    if (shiftActive && btn.shiftLabel) return btn.shiftLabel
    return btn.label
  }

  function actionId(btn: BtnDef): string {
    if (shiftActive) {
      if (btn.id === 'sin')         return 'asin'
      if (btn.id === 'cos')         return 'acos'
      if (btn.id === 'tan')         return 'atan'
      if (btn.id === 'exponent')    return 'nthRoot'
      if (btn.id === 'sqrt')        return 'cbrt'
      if (btn.id === 'log')         return 'MixedFrac'
      if (btn.id === 'ln')          return 'DecFrac'
      if (btn.id === 'fraction')    return 'mix'
      if (btn.id === 'multiply')    return 'npr'
      if (btn.id === 'divide')      return 'ncr'
      if (btn.id === '=')           return 'S_TO_D'
      if (btn.id === 'factorial')   return 'A'
      if (btn.id === 'cube')        return 'B'
      if (btn.id === 'square')      return 'X'
      if (btn.id === 'paren_close') return 'Y'
      if (btn.id === 'Sto')         return 'PasteCode'
    }
    return btn.id
  }

  function handleClick(btn: BtnDef) {
    const id = actionId(btn)
    if (id === 'PasteCode') { onPaste(); return }
    onButton(id)
  }

  function renderBtn(btn: BtnDef, key: string) {
    const secondary = !shiftActive && btn.shiftLabel ? btn.shiftLabel : null
    return (
      <button
        key={key}
        onClick={() => handleClick(btn)}
        className={cn(
          'rounded text-sm font-medium select-none',
          'active:scale-95 active:brightness-75 transition-all duration-75',
          secondary
            ? 'h-12 flex flex-col items-center justify-center gap-0'
            : 'h-11 flex items-center justify-center',
          btn.cls,
        )}
      >
        {secondary && (
          <span className="text-[7px] text-orange-400 leading-none mb-0.5 font-normal tracking-wide">
            {secondary}
          </span>
        )}
        <span className="leading-none">{displayLabel(btn)}</span>
      </button>
    )
  }

  return (
    <div className="grid grid-cols-5 gap-[3px] p-2 bg-zinc-950 flex-shrink-0">
      {ROWS.flat().map((btn, i) => renderBtn(btn, btn.id + i))}
    </div>
  )
}
