# Scientific Calculator

Scientific calculator with natural textbook display (fractions, roots and powers rendered as written). TypeScript, React and Tailwind, built with Bun and Vite, packaged for Android with Capacitor.

## Develop

```sh
bun install
bun run dev        # web, http://localhost:5173
bun run test       # vitest
bun run build      # type check and bundle to dist/
```

## Android

Needs JDK 17 and the Android SDK (`ANDROID_HOME`).

```sh
bun run cap:sync   # build web and copy it into android/
bun run release    # build a signed nightly APK and publish a GitHub prerelease
```

The landing page lives in `landing/` (Astro) and deploys to Vercel.
