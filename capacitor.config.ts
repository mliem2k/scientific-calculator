import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.mliem.scientific_calculator',
  appName: 'Scientific Calculator',
  webDir: 'dist',
  android: { allowMixedContent: false },
}

export default config
