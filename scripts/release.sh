#!/usr/bin/env bash
set -euo pipefail

command -v flutter >/dev/null 2>&1 || { echo "flutter not found"; exit 1; }
command -v gh      >/dev/null 2>&1 || { echo "gh CLI not found"; exit 1; }

DATE=$(date +%Y%m%d)
BUILD=$(date +%s)
TAG="nightly-${DATE}"
APK="build/app/outputs/flutter-apk/app-release.apk"
APK_ASSET="${APK}#scientific-calculator-${TAG}.apk"

echo "Building nightly APK (build ${BUILD})..."
flutter build apk --release --build-number="${BUILD}"

echo "Creating GitHub Release ${TAG}..."
gh release create "${TAG}" \
  --title "Nightly ${DATE}" \
  --notes "Automated nightly build (${DATE}, build ${BUILD})." \
  --prerelease \
  "${APK_ASSET}"

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
echo "Done: https://github.com/${REPO}/releases/tag/${TAG}"
