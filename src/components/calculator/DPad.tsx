interface Props {
  onButton: (id: string) => void
}

export function DPad({ onButton }: Props) {
  return (
    <div className="col-span-2 row-span-1 flex items-center justify-center py-0.5">
      <div className="relative w-[4.75rem] h-[4.75rem]">
        {/* Outer ring with depth shadow */}
        <div className="absolute inset-0 rounded-full bg-zinc-600 shadow-[inset_0_1px_0_rgba(255,255,255,0.12),0_3px_6px_rgba(0,0,0,0.6)]" />

        {/* UP */}
        <button
          aria-label="cursor up"
          onClick={() => onButton('UP')}
          className="absolute inset-x-0 top-0 h-[48%] flex items-start justify-center pt-1.5 rounded-t-full hover:bg-white/10 active:bg-black/20 transition-colors z-10"
        >
          <span className="text-zinc-200 text-[9px]">▲</span>
        </button>

        {/* DOWN */}
        <button
          aria-label="cursor down"
          onClick={() => onButton('DOWN')}
          className="absolute inset-x-0 bottom-0 h-[48%] flex items-end justify-center pb-1.5 rounded-b-full hover:bg-white/10 active:bg-black/20 transition-colors z-10"
        >
          <span className="text-zinc-200 text-[9px]">▼</span>
        </button>

        {/* LEFT */}
        <button
          aria-label="cursor left"
          onClick={() => onButton('LEFT')}
          className="absolute inset-y-0 left-0 w-[48%] flex items-center justify-start pl-1.5 rounded-l-full hover:bg-white/10 active:bg-black/20 transition-colors z-10"
        >
          <span className="text-zinc-200 text-[9px]">◄</span>
        </button>

        {/* RIGHT */}
        <button
          aria-label="cursor right"
          onClick={() => onButton('RIGHT')}
          className="absolute inset-y-0 right-0 w-[48%] flex items-center justify-end pr-1.5 rounded-r-full hover:bg-white/10 active:bg-black/20 transition-colors z-10"
        >
          <span className="text-zinc-200 text-[9px]">►</span>
        </button>

        {/* Centre cap — slightly recessed */}
        <div className="absolute inset-[27%] rounded-full bg-zinc-800 shadow-[inset_0_2px_4px_rgba(0,0,0,0.8)] z-20 pointer-events-none" />
      </div>
    </div>
  )
}
