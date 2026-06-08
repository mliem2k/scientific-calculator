interface Props {
  onButton: (id: string) => void
}

export function DPad({ onButton }: Props) {
  return (
    <div className="col-span-2 row-span-1 flex items-center justify-center py-1">
      <div className="relative w-20 h-20">
        {/* Outer ring */}
        <div className="absolute inset-0 rounded-full bg-zinc-700" />

        {/* UP */}
        <button
          aria-label="cursor up"
          onClick={() => onButton('UP')}
          className="absolute inset-x-0 top-0 h-1/2 flex items-start justify-center pt-1 rounded-t-full active:bg-zinc-500/40 transition-colors z-10"
        >
          <span className="text-white text-[10px] mt-0.5">▲</span>
        </button>

        {/* DOWN */}
        <button
          aria-label="cursor down"
          onClick={() => onButton('DOWN')}
          className="absolute inset-x-0 bottom-0 h-1/2 flex items-end justify-center pb-1 rounded-b-full active:bg-zinc-500/40 transition-colors z-10"
        >
          <span className="text-white text-[10px] mb-0.5">▼</span>
        </button>

        {/* LEFT */}
        <button
          aria-label="cursor left"
          onClick={() => onButton('LEFT')}
          className="absolute inset-y-0 left-0 w-1/2 flex items-center justify-start pl-1 rounded-l-full active:bg-zinc-500/40 transition-colors z-10"
        >
          <span className="text-white text-[10px] ml-0.5">◄</span>
        </button>

        {/* RIGHT */}
        <button
          aria-label="cursor right"
          onClick={() => onButton('RIGHT')}
          className="absolute inset-y-0 right-0 w-1/2 flex items-center justify-end pr-1 rounded-r-full active:bg-zinc-500/40 transition-colors z-10"
        >
          <span className="text-white text-[10px] mr-0.5">►</span>
        </button>

        {/* Centre cap */}
        <div className="absolute inset-[28%] rounded-full bg-zinc-800 z-20 pointer-events-none" />
      </div>
    </div>
  )
}
