#!/usr/bin/env bun
import { $ } from 'bun';

const date = new Date();
const dateStr = date.toISOString().slice(0, 10).replace(/-/g, '');
const build = Math.floor(date.getTime() / 1000);

// Tag embeds the build number so the app can compare exactly, avoiding the
// ~40s gap between the build number (script start) and published_at (release create).
const tag = `nightly-${dateStr}-${build}`;

const apk = 'android/app/build/outputs/apk/release/app-release.apk';
const renamedApk = `android/app/build/outputs/apk/release/scientific-calculator-${tag}.apk`;
const apkAsset = `${renamedApk}#scientific-calculator-${tag}.apk`;

const found = await $`which gh`.quiet().nothrow();
if (found.exitCode !== 0) { console.error('gh not found'); process.exit(1); }

console.log(`Building nightly APK (build ${build})...`);
await $`./gradlew assembleRelease -PbuildNumber=${build}`.cwd('android');
await $`cp ${apk} ${renamedApk}`;

// Delete any existing same-day releases. Prefix is nightly-YYYYMMDD (no trailing
// dash) so it also catches old-format tags like "nightly-20260613" that predate
// the embedded build number format.
const sameDayPrefix = `nightly-${dateStr}`;
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
