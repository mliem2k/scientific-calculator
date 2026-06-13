#!/usr/bin/env bun
import { $ } from 'bun';

const date = new Date();
const dateStr = date.toISOString().slice(0, 10).replace(/-/g, '');
const build = Math.floor(date.getTime() / 1000);
const tag = `nightly-${dateStr}`;
const apk = 'build/app/outputs/flutter-apk/app-release.apk';
const apkAsset = `${apk}#scientific-calculator-${tag}.apk`;

for (const tool of ['flutter', 'gh']) {
  const found = await $`which ${tool}`.quiet().nothrow();
  if (found.exitCode !== 0) {
    console.error(`${tool} not found`);
    process.exit(1);
  }
}

console.log(`Building nightly APK (build ${build})...`);
await $`flutter build apk --release --build-number=${build} --dart-define=BUILD_NUMBER=${build}`;

// Delete existing same-day release so we can overwrite it
await $`gh release delete ${tag} --yes`.nothrow().quiet();
await $`git push origin :refs/tags/${tag}`.nothrow().quiet();

console.log(`Creating GitHub Release ${tag}...`);
await $`gh release create ${tag} --title ${'Nightly ' + dateStr} --notes ${'Automated nightly build (' + dateStr + ', build ' + build + ').'} --prerelease ${apkAsset}`;

const repo = await $`gh repo view --json nameWithOwner -q .nameWithOwner`.text();
console.log(`Done: https://github.com/${repo.trim()}/releases/tag/${tag}`);
