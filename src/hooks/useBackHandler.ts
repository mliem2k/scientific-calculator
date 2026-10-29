import { useEffect, useRef } from 'react'
import { App } from '@capacitor/app'

// Android back button: the most recently enabled handler wins, and with none the app exits.
// Registering a Capacitor listener also stops the WebView from handling back on its own,
// which would otherwise close the app when a sub screen is open.
const stack: Array<() => void> = []
let listening = false

function listen() {
  if (listening) return
  listening = true
  App.addListener('backButton', () => {
    const top = stack[stack.length - 1]
    if (top) top()
    else App.exitApp()
  }).catch(() => { listening = false })
}

export function useBackHandler(handler: () => void, enabled = true) {
  const latest = useRef(handler)
  latest.current = handler

  useEffect(() => {
    if (!enabled) return
    listen()
    const entry = () => latest.current()
    stack.push(entry)
    return () => {
      const i = stack.lastIndexOf(entry)
      if (i >= 0) stack.splice(i, 1)
    }
  }, [enabled])
}
