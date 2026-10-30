package com.mliem.scientific_calculator.data

import com.mliem.scientific_calculator.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class UpdateInfo(val tagName: String, val releaseUrl: String, val apkUrl: String?)

/** Compares the installed build with the nightly releases on GitHub. */
class Updater(
    private val buildNumber: Long = BuildConfig.BUILD_NUMBER,
    private val fetch: suspend () -> String = ::fetchReleases,
) {
    /** A zero build number marks a local build, which never looks for updates. */
    val enabled get() = buildNumber > 0

    /** Returns null when up to date and throws when GitHub cannot be reached. */
    suspend fun check(): UpdateInfo? {
        if (!enabled) return null
        return newest(JSONArray(fetch()), buildNumber)
    }

    companion object {
        private const val RELEASES = "https://api.github.com/repos/mliem2k/scientific-calculator/releases?per_page=10"

        // Tags look like nightly-YYYYMMDD-BUILDNUM; anything else has no build number.
        private fun buildFromTag(tag: String) = tag.substringAfterLast('-').toLongOrNull()?.takeIf { it > 0 }

        /** Takes the highest build in the batch: a single "latest" call can return a stale release. */
        internal fun newest(releases: JSONArray, installed: Long): UpdateInfo? {
            var best: UpdateInfo? = null
            var bestBuild = installed
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                val tag = release.optString("tag_name")
                val url = release.optString("html_url")
                val build = buildFromTag(tag)
                if (tag.isEmpty() || url.isEmpty() || build == null || build <= bestBuild) continue
                bestBuild = build
                val assets = release.optJSONArray("assets") ?: JSONArray()
                val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk") }
                best = UpdateInfo(tag, url, apk?.optString("browser_download_url")?.takeIf { it.isNotEmpty() })
            }
            return best
        }

        private suspend fun fetchReleases(): String = withContext(Dispatchers.IO) {
            val connection = URL(RELEASES).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.useCaches = false
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                check(connection.responseCode == 200) { "GitHub returned ${connection.responseCode}" }
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }

        /** nightly-20260616-1781592056 becomes "Nightly, Jun 16, 2026". */
        fun formatTag(tag: String): String {
            val date = tag.split('-').getOrNull(1)?.takeIf { it.length == 8 } ?: return tag
            val parser = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
            val parsed = runCatching { parser.parse(date) }.getOrNull() ?: return tag
            val out = SimpleDateFormat("MMM d, yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            return "Nightly, ${out.format(parsed)}"
        }

        fun versionLabel(buildNumber: Long = BuildConfig.BUILD_NUMBER): String {
            if (buildNumber == 0L) return "Dev build"
            val out = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            return "Build ${out.format(buildNumber * 1000)}"
        }
    }
}
