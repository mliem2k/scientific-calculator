export type ThemeId = 'dark' | 'amoled' | 'light'

export interface CalcTheme {
  background: string
  /** Grouped list rows in the settings and history screens. */
  surface: string
  line: string
  text: string
  dim: string
  digitBg: string
  digitText: string
  sciBg: string
  sciText: string
  utilBg: string
  utilText: string
  opBg: string
  opText: string
  /** Caret, selected state and the active 2nd key. */
  accent: string
  /** Pressed-key brightness multiplier. */
  press: string
}

export const THEMES: Record<ThemeId, { name: string; colors: CalcTheme }> = {
  dark: {
    name: 'Dark',
    colors: {
      background: '#000000', surface: '#1c1c1e', line: '#38383a', text: '#ffffff', dim: '#8e8e93',
      digitBg: '#333333', digitText: '#ffffff', sciBg: '#1c1c1e', sciText: '#ffffff',
      utilBg: '#a5a5a5', utilText: '#000000', opBg: '#ff9f0a', opText: '#ffffff',
      accent: '#ff9f0a', press: '1.35',
    },
  },
  amoled: {
    name: 'AMOLED',
    colors: {
      background: '#000000', surface: '#0f0f10', line: '#232325', text: '#ffffff', dim: '#8e8e93',
      digitBg: '#161618', digitText: '#ffffff', sciBg: '#0c0c0d', sciText: '#f2f2f7',
      utilBg: '#8e8e93', utilText: '#000000', opBg: '#ff9f0a', opText: '#000000',
      accent: '#ff9f0a', press: '1.6',
    },
  },
  light: {
    name: 'Light',
    colors: {
      background: '#f2f2f7', surface: '#ffffff', line: '#d1d1d6', text: '#000000', dim: '#6c6c70',
      digitBg: '#ffffff', digitText: '#000000', sciBg: '#e5e5ea', sciText: '#1c1c1e',
      utilBg: '#d1d1d6', utilText: '#000000', opBg: '#c93400', opText: '#ffffff',
      accent: '#c93400', press: '0.9',
    },
  },
}

export const THEME_IDS = Object.keys(THEMES) as ThemeId[]

// Camel case token to the kebab case CSS custom property Tailwind reads.
const cssVar = (key: string) => `--c-${key.replace(/[A-Z]/g, (m) => `-${m.toLowerCase()}`)}`

export function applyTheme(id: ThemeId): void {
  const { colors } = THEMES[id] ?? THEMES.dark
  const root = document.documentElement
  for (const [key, value] of Object.entries(colors)) root.style.setProperty(cssVar(key), value)
  root.style.colorScheme = id === 'light' ? 'light' : 'dark'
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', colors.background)
}
