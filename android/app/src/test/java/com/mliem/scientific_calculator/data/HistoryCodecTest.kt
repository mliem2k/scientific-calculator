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

class CurrencyRatesTest {
    private val json = """{"result":"success","base_code":"USD","rates":{"USD":1,"EUR":0.5,"JPY":150.0}}"""

    @Test fun parsesRatesAndConvertsThroughTheDollar() {
        val rates = CurrencyRepository.parse(json, 42L)
        assertEquals(listOf("EUR", "JPY", "USD"), rates.codes)
        assertEquals(42L, rates.updatedAtMillis)
        assertEquals(300.0, rates.convert(1.0, "EUR", "JPY")!!, 1e-9)
        assertEquals(0.5, rates.convert(1.0, "USD", "EUR")!!, 1e-9)
        assertNull(rates.convert(1.0, "USD", "XXX"))
    }

    @Test fun aFailedResponseIsRejected() {
        assertNull(runCatching { CurrencyRepository.parse("""{"result":"error"}""", 0L) }.getOrNull())
    }
}
