import { useEffect } from 'react'

const KEY_MAP: Record<string, string> = {
  '0':'0','1':'1','2':'2','3':'3','4':'4','5':'5','6':'6','7':'7','8':'8','9':'9',
  '.':'.', '+':'plus', '-':'minus', '*':'multiply', '/':'divide',
  'Enter':'=', '=':'=',
  'Backspace':'DEL', 'Delete':'CLEAR',
  'ArrowLeft':'LEFT', 'ArrowRight':'RIGHT', 'ArrowUp':'UP', 'ArrowDown':'DOWN',
  'Escape':'CLEAR',
}

export function useKeyboard(onButton: (id: string) => void, enabled = true) {
  useEffect(() => {
    if (!enabled) return
    function onKey(e: KeyboardEvent) {
      const id = KEY_MAP[e.key]
      if (id) { e.preventDefault(); onButton(id) }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onButton, enabled])
}
