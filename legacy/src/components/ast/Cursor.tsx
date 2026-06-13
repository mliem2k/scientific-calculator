export function CursorView() {
  // animate-pulse keeps test compatibility; the step-end blink is via CSS override in index.css
  return <span className="inline-block w-0.5 h-[1.1em] bg-white animate-pulse mx-px align-middle" aria-hidden />
}
