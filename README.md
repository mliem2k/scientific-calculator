# Scientific Calculator

Scientific calculator for Android with a natural textbook display (fractions, roots and powers rendered as written). Native Kotlin and Jetpack Compose, styled after the ColorOS calculator.

## Develop

Needs the Android SDK (`sdk.dir` in `android/local.properties`); Gradle fetches JDK 21.

```sh
cd android
./gradlew testDebugUnitTest   # engine, editor, history and updater tests
./gradlew installDebug        # build and install on a connected device
```

## Release

```sh
bun scripts/release.js   # build a signed nightly APK and publish a GitHub prerelease
```

The app checks these releases for updates; a local build has build number 0 and skips the check.

The landing page lives in `landing/` (Astro) and deploys to Vercel.
