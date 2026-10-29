export type ThemeId = 'amoled' | 'dark' | 'light' | 'retro'

export interface CalcTheme {
  background: string
  displayBg: string
  buttonBg: string
  buttonBorder: string
  digitText: string
  fnText: string
  opText: string
  eqBg: string
  eqText: string
  delText: string
  secondaryLabel: string
  expressionText: string
  resultText: string
  shiftActive: string
  statusBadge: string
}

export const THEMES: Record<ThemeId, { name: string; colors: CalcTheme }> = {
  amoled: {
    name: 'AMOLED',
    colors: {
      background: '#000000', displayBg: '#000000', buttonBg: '#000000', buttonBorder: '#27272a',
      digitText: '#ffffff', fnText: '#ffffff', opText: '#ffffff', eqBg: '#ffffff', eqText: '#000000',
      delText: '#ef4444', secondaryLabel: '#a1a1aa', expressionText: '#ffffff', resultText: '#a1a1aa',
      shiftActive: '#fb923c', statusBadge: '#818cf8',
    },
  },
  dark: {
    name: 'Dark',
    colors: {
      background: '#121212', displayBg: '#1a1a1a', buttonBg: '#1e1e1e', buttonBorder: '#2d2d2d',
      digitText: '#ffffff', fnText: '#e0e0e0', opText: '#81d4fa', eqBg: '#448aff', eqText: '#ffffff',
      delText: '#ff5252', secondaryLabel: '#9e9e9e', expressionText: '#ffffff', resultText: '#bdbdbd',
      shiftActive: '#ffa726', statusBadge: '#7986cb',
    },
  },
  light: {
    name: 'Light',
    colors: {
      background: '#f5f5f5', displayBg: '#ffffff', buttonBg: '#ffffff', buttonBorder: '#e0e0e0',
      digitText: '#212121', fnText: '#424242', opText: '#1565c0', eqBg: '#1565c0', eqText: '#ffffff',
      delText: '#d32f2f', secondaryLabel: '#757575', expressionText: '#212121', resultText: '#616161',
      shiftActive: '#e65100', statusBadge: '#5c6bc0',
    },
  },
  retro: {
    name: 'Retro',
    colors: {
      background: '#0d0d0d', displayBg: '#0d0d0d', buttonBg: '#0d0d0d', buttonBorder: '#003300',
      digitText: '#00ff41', fnText: '#00cc33', opText: '#00ff41', eqBg: '#00cc33', eqText: '#000000',
      delText: '#ff3300', secondaryLabel: '#00a020', expressionText: '#00ff41', resultText: '#00cc33',
      shiftActive: '#ffff00', statusBadge: '#ff6600',
    },
  },
}

export const THEME_IDS = Object.keys(THEMES) as ThemeId[]

// Camel case token to the kebab case CSS custom property Tailwind reads.
const cssVar = (key: string) => `--c-${key.replace(/[A-Z]/g, (m) => `-${m.toLowerCase()}`)}`

export function applyTheme(id: ThemeId): void {
  const { colors } = THEMES[id]
  const root = document.documentElement
  for (const [key, value] of Object.entries(colors)) root.style.setProperty(cssVar(key), value)
  root.style.colorScheme = id === 'light' ? 'light' : 'dark'
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', colors.background)
}

// Readable text colour for a badge drawn on an arbitrary background.
export function contrastText(hex: string): '#000000' | '#ffffff' {
  const n = parseInt(hex.slice(1), 16)
  const lin = (c: number) => {
    const s = c / 255
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
  }
  const l = 0.2126 * lin((n >> 16) & 255) + 0.7152 * lin((n >> 8) & 255) + 0.0722 * lin(n & 255)
  return l > 0.179 ? '#000000' : '#ffffff'
}
