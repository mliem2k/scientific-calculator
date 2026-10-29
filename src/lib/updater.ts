// Baked in at build time (VITE_BUILD_NUMBER=<unix seconds>). Zero is a dev build and skips update checks.
export const BUILD_NUMBER = Number(import.meta.env.VITE_BUILD_NUMBER ?? 0) || 0

const REPO = 'mliem2k/scientific-calculator'

export interface UpdateInfo {
  tagName: string
  releaseUrl: string
  apkUrl: string | null
}

interface GithubRelease {
  tag_name?: string
  html_url?: string
  assets?: { name?: string; browser_download_url?: string }[]
}

// Tags look like nightly-YYYYMMDD-BUILDNUM; anything else has no build number.
function buildFromTag(tag: string): number | null {
  const n = Number(tag.slice(tag.lastIndexOf('-') + 1))
  return Number.isInteger(n) && n > 0 ? n : null
}

// Scan a batch of releases and take the highest build number: a single "latest"
// call can return a stale item through CDN caching or a same-day re-release.
export async function checkForUpdate(): Promise<UpdateInfo | null> {
  if (BUILD_NUMBER === 0) return null
  try {
    const res = await fetch(`https://api.github.com/repos/${REPO}/releases?per_page=10`, {
      headers: { Accept: 'application/vnd.github+json', 'Cache-Control': 'no-cache' },
      signal: AbortSignal.timeout(15000),
    })
    if (!res.ok) return null
    const releases = (await res.json()) as GithubRelease[]

    let bestBuild = BUILD_NUMBER
    let best: UpdateInfo | null = null
    for (const r of releases) {
      if (!r.tag_name || !r.html_url) continue
      const build = buildFromTag(r.tag_name)
      if (build === null || build <= bestBuild) continue
      bestBuild = build
      const apk = r.assets?.find((a) => a.name?.endsWith('.apk'))
      best = { tagName: r.tag_name, releaseUrl: r.html_url, apkUrl: apk?.browser_download_url ?? null }
    }
    return best
  } catch {
    return null
  }
}

// nightly-20260616-1781592056 becomes "Nightly, Jun 16, 2026".
export function formatTag(tag: string): string {
  const date = tag.split('-')[1]
  if (!date || date.length !== 8) return tag
  const d = new Date(Date.UTC(+date.slice(0, 4), +date.slice(4, 6) - 1, +date.slice(6, 8)))
  if (Number.isNaN(d.getTime())) return tag
  return `Nightly, ${d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' })}`
}

export function buildVersionString(): string {
  if (BUILD_NUMBER === 0) return 'Dev build'
  return `Build ${new Date(BUILD_NUMBER * 1000).toISOString().slice(0, 10)}`
}
