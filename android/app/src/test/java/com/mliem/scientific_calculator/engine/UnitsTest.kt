package com.mliem.scientific_calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitsTest {
    private fun unit(category: UnitCategory, id: String) = checkNotNull(Units.byCategory[category]).first { it.id == id }

    private fun convert(category: UnitCategory, value: Double, from: String, to: String) =
        Units.convert(category, value, unit(category, from), unit(category, to))

    @Test fun lengthStartsFromMetersToMiles() {
        val (top, bottom) = Units.defaults(UnitCategory.Length)
        assertEquals("m", top.id)
        assertEquals("mi", bottom.id)
        assertEquals("0.0006213711922", formatNumber(Units.convert(UnitCategory.Length, 1.0, top, bottom)))
    }

    @Test fun linearUnits() {
        assertEquals(1000.0, convert(UnitCategory.Length, 1.0, "km", "m"), 1e-9)
        assertEquals(12.0, convert(UnitCategory.Length, 1.0, "ft", "in"), 1e-9)
        assertEquals(1.0, convert(UnitCategory.Weight, 1000.0, "g", "kg"), 1e-12)
        assertEquals(3.785411784, convert(UnitCategory.Volume, 1.0, "gal", "l"), 1e-9)
        assertEquals(3.6, convert(UnitCategory.Speed, 1.0, "ms", "kmh"), 1e-9)
        assertEquals(1.01325, convert(UnitCategory.Pressure, 1.0, "atm", "bar"), 1e-9)
        assertEquals(745.69987158227, convert(UnitCategory.Power, 1.0, "hp", "w"), 1e-9)
        assertEquals(10000.0, convert(UnitCategory.Area, 1.0, "ha", "m2"), 1e-9)
    }

    @Test fun temperatureIsNotLinearInOneBase() {
        assertEquals(212.0, convert(UnitCategory.Temperature, 100.0, "c", "f"), 1e-9)
        assertEquals(273.15, convert(UnitCategory.Temperature, 0.0, "c", "k"), 1e-9)
        assertEquals(-40.0, convert(UnitCategory.Temperature, -40.0, "f", "c"), 1e-9)
    }

    @Test fun everyCategoryHasUniqueIdsAndPositiveFactors() {
        UnitCategory.entries.forEach { category ->
            val units = checkNotNull(Units.byCategory[category])
            assertEquals(category.name, units.size, units.map { it.id }.toSet().size)
            assertTrue(category.name, units.all { it.perBase > 0 })
        }
    }

    @Test fun numberSystems() {
        assertEquals("FF", NumberSystems.convert("255", NumberSystem.Decimal, NumberSystem.Hexadecimal))
        assertEquals("255", NumberSystems.convert("ff", NumberSystem.Hexadecimal, NumberSystem.Decimal))
        assertEquals("1010", NumberSystems.convert("12", NumberSystem.Octal, NumberSystem.Binary))
        assertNull(NumberSystems.convert("12", NumberSystem.Binary, NumberSystem.Decimal))
        assertNull(NumberSystems.convert("", NumberSystem.Decimal, NumberSystem.Binary))
        assertEquals("01", NumberSystems.digitsFor(NumberSystem.Binary))
    }
}

class ExpressionParserTest {
    @Test fun readsNumbersOperatorsAndParentheses() {
        val nodes = parseExpression("12.5 + (3 × 4)")
        assertEquals(listOf(Num("12.5"), Op(Operator.Plus), Paren(listOf(Num("3"), Op(Operator.Times), Num("4")))), nodes)
    }

    @Test fun percentWrapsThePreviousOperand() {
        assertEquals(listOf(Num("5"), Op(Operator.Plus), Percent(listOf(Num("10")))), parseExpression("5+10%"))
    }

    @Test fun rejectsOtherTextAndUnbalancedParentheses() {
        assertNull(parseExpression("hello"))
        assertNull(parseExpression("(1+2"))
        assertNull(parseExpression("1+2)"))
        assertNull(parseExpression("1.2.3"))
        assertNull(parseExpression("+%"))
        assertNull(parseExpression("  "))
    }

    @Test fun parsedTextEvaluates() {
        val value = evaluate(checkNotNull(parseExpression("2*(3+4)")), AngleMode.Deg) as Evaluation.Value
        assertEquals(14.0, value.number, 0.0)
    }
}
