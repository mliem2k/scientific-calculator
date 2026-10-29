import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.mliem.scientific_calculator',
  appName: 'Scientific Calculator',
  webDir: 'dist',
  // Android 15 draws edge to edge; 'auto' pads the WebView so the header clears the status bar.
  android: { allowMixedContent: false, adjustMarginsForEdgeToEdge: 'auto' },
}

export default config
