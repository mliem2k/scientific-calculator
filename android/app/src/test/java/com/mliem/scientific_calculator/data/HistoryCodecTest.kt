package com.mliem.scientific_calculator.data

import com.mliem.scientific_calculator.engine.Const
import com.mliem.scientific_calculator.engine.ConstantName
import com.mliem.scientific_calculator.engine.Exponent
import com.mliem.scientific_calculator.engine.Factorial
import com.mliem.scientific_calculator.engine.Fraction
import com.mliem.scientific_calculator.engine.Func
import com.mliem.scientific_calculator.engine.FunctionName
import com.mliem.scientific_calculator.engine.HistoryEntry
import com.mliem.scientific_calculator.engine.Ncr
import com.mliem.scientific_calculator.engine.Npr
import com.mliem.scientific_calculator.engine.Num
import com.mliem.scientific_calculator.engine.Op
import com.mliem.scientific_calculator.engine.Operator
import com.mliem.scientific_calculator.engine.Paren
import com.mliem.scientific_calculator.engine.Radical
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryCodecTest {
    @Test fun everyNodeTypeSurvivesARoundTrip() {
        val expression = listOf(
            Num("1.5"), Op(Operator.Divide), Const(ConstantName.Pi),
            Fraction(listOf(Num("1")), listOf(Num("2"))),
            Exponent(listOf(Num("2")), listOf(Num("3"))),
            Radical(listOf(Num("3")), listOf(Num("8"))),
            Func(FunctionName.Sin, listOf(Num("30"))),
            Factorial(listOf(Num("4"))),
            Ncr(listOf(Num("5")), listOf(Num("2"))),
            Npr(listOf(Num("5")), listOf(Num("2"))),
            Paren(listOf(Paren(listOf(Num("7"))))),
        )
        val entries = listOf(HistoryEntry("a", expression, "42", 123L), HistoryEntry("b", emptyList(), "0", 9L))
        assertEquals(entries, HistoryCodec.decode(HistoryCodec.encode(entries)))
    }

    @Test fun anUnknownNodeTypeFailsToDecode() {
        val json = """[{"id":"a","result":"1","time":1,"expr":[{"t":"mystery"}]}]"""
        assertNull(runCatching { HistoryCodec.decode(json) }.getOrNull())
    }
}

class UpdaterTest {
    private fun releases(vararg tags: String) = JSONArray().also { array ->
        tags.forEach { tag ->
            array.put(
                org.json.JSONObject().put("tag_name", tag).put("html_url", "https://example.test/$tag")
                    .put("assets", JSONArray().put(org.json.JSONObject().put("name", "app-$tag.apk").put("browser_download_url", "https://example.test/$tag.apk"))),
            )
        }
    }

    @Test fun picksTheHighestBuildNewerThanTheInstalledOne() {
        val found = Updater.newest(releases("nightly-20260610-100", "nightly-20260612-300", "nightly-20260611-200"), installed = 150)
        assertEquals("nightly-20260612-300", found?.tagName)
        assertEquals("https://example.test/nightly-20260612-300.apk", found?.apkUrl)
    }

    @Test fun reportsNothingWhenUpToDateOrTagsHaveNoBuildNumber() {
        assertNull(Updater.newest(releases("nightly-20260610-100"), installed = 100))
        assertNull(Updater.newest(releases("v1.0"), installed = 1))
    }

    @Test fun formatsATagAsADate() {
        assertEquals("Nightly, Jun 16, 2026", Updater.formatTag("nightly-20260616-1781592056"))
        assertEquals("weird", Updater.formatTag("weird"))
    }
}
