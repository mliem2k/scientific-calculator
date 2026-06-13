#!/usr/bin/env bun
import { $ } from 'bun';

const date = new Date();
const dateStr = date.toISOString().slice(0, 10).replace(/-/g, '');
const build = Math.floor(date.getTime() / 1000);

// Tag embeds the build number so the app can compare exactly, avoiding the
// ~40s gap between _buildNumber (script start) and published_at (release create).
const tag = `nightly-${dateStr}-${build}`;

const apk = 'build/app/outputs/flutter-apk/app-release.apk';
const renamedApk = `build/app/outputs/flutter-apk/scientific-calculator-${tag}.apk`;
const apkAsset = `${renamedApk}#scientific-calculator-${tag}.apk`;

for (const tool of ['fvm', 'gh']) {
  const found = await $`which ${tool}`.quiet().nothrow();
  if (found.exitCode !== 0) { console.error(`${tool} not found`); process.exit(1); }
}

console.log(`Building nightly APK (build ${build})...`);
await $`fvm flutter build apk --release --build-number=${build} --dart-define=BUILD_NUMBER=${build}`;
await $`cp ${apk} ${renamedApk}`;

// Delete any existing same-day releases (tags matching nightly-YYYYMMDD-*).
// Each run creates a distinct tag, so we clean up stale same-day builds here.
const sameDayPrefix = `nightly-${dateStr}-`;
const listJson = await $`gh release list --json tagName`.text().catch(() => '[]');
const allReleases = JSON.parse(listJson);
for (const { tagName: t } of allReleases) {
  if (t.startsWith(sameDayPrefix) && t !== tag) {
    console.log(`Removing stale same-day release ${t}...`);
    await $`gh release delete ${t} --yes`.nothrow().quiet();
    await $`git push origin :refs/tags/${t}`.nothrow().quiet();
  }
}

console.log(`Creating GitHub Release ${tag}...`);
await $`gh release create ${tag} --title ${'Nightly ' + dateStr} --notes ${'Automated nightly build (' + dateStr + ', build ' + build + ').'} --prerelease ${apkAsset}`;

const repo = await $`gh repo view --json nameWithOwner -q .nameWithOwner`.text();
console.log(`Done: https://github.com/${repo.trim()}/releases/tag/${tag}`);
