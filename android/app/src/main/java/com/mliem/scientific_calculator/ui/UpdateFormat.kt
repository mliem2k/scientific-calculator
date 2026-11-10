package com.mliem.scientific_calculator.ui

import com.mliem.android.update.TagParser
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

private val NIGHTLY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

/** `nightly-20260616-1781592056` becomes "Nightly, Jun 16, 2026". */
fun formatTag(tag: String): String {
    val date = TagParser.parse(tag)?.date ?: return tag
    return "Nightly, ${NIGHTLY_DATE.format(date)}"
}

/** A build number is Unix seconds; zero marks a local build. */
fun versionLabel(buildNumber: Long): String {
    if (buildNumber == 0L) return "Dev build"
    val out = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    return "Build ${out.format(buildNumber * 1000)}"
}
