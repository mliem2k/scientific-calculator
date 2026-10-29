import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { getSettings } from '@/lib/settings'
import { applyTheme } from '@/lib/themes'
import './index.css'

applyTheme(getSettings().themeId)

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
