package com.mliem.scientific_calculator.engine

import java.math.BigInteger

enum class UnitCategory { Length, Area, Volume, Weight, Temperature, Speed, Pressure, Power }

/** A unit and how many base units (metre, square metre, litre, kilogram, ...) one of it is worth. */
data class ConversionUnit(val id: String, val name: String, val symbol: String, val perBase: Double = 1.0)

object Units {
    private fun u(id: String, name: String, symbol: String, perBase: Double = 1.0) = ConversionUnit(id, name, symbol, perBase)

    val byCategory: Map<UnitCategory, List<ConversionUnit>> = mapOf(
        UnitCategory.Length to listOf(
            u("m", "Meter", "m"), u("km", "Kilometer", "km", 1000.0), u("cm", "Centimeter", "cm", 0.01),
            u("mm", "Millimeter", "mm", 0.001), u("um", "Micrometer", "µm", 1e-6), u("nm", "Nanometer", "nm", 1e-9),
            u("mi", "Mile", "mi", 1609.344), u("yd", "Yard", "yd", 0.9144), u("ft", "Foot", "ft", 0.3048),
            u("in", "Inch", "in", 0.0254), u("nmi", "Nautical mile", "nmi", 1852.0),
        ),
        UnitCategory.Area to listOf(
            u("m2", "Square meter", "m²"), u("km2", "Square kilometer", "km²", 1e6), u("ha", "Hectare", "ha", 1e4),
            u("cm2", "Square centimeter", "cm²", 1e-4), u("mm2", "Square millimeter", "mm²", 1e-6),
            u("ac", "Acre", "ac", 4046.8564224), u("mi2", "Square mile", "mi²", 2589988.110336),
            u("yd2", "Square yard", "yd²", 0.83612736), u("ft2", "Square foot", "ft²", 0.09290304),
            u("in2", "Square inch", "in²", 6.4516e-4),
        ),
        UnitCategory.Volume to listOf(
            u("l", "Liter", "L"), u("ml", "Milliliter", "mL", 0.001), u("m3", "Cubic meter", "m³", 1000.0),
            u("cm3", "Cubic centimeter", "cm³", 0.001), u("gal", "US gallon", "gal", 3.785411784),
            u("qt", "US quart", "qt", 0.946352946), u("pt", "US pint", "pt", 0.473176473),
            u("cup", "US cup", "cup", 0.2365882365), u("floz", "US fluid ounce", "fl oz", 0.0295735295625),
            u("tbsp", "US tablespoon", "tbsp", 0.01478676478125), u("tsp", "US teaspoon", "tsp", 0.00492892159375),
            u("ukgal", "UK gallon", "gal UK", 4.54609),
        ),
        UnitCategory.Weight to listOf(
            u("kg", "Kilogram", "kg"), u("g", "Gram", "g", 0.001), u("mg", "Milligram", "mg", 1e-6),
            u("t", "Tonne", "t", 1000.0), u("lb", "Pound", "lb", 0.45359237), u("oz", "Ounce", "oz", 0.028349523125),
            u("st", "Stone", "st", 6.35029318), u("ct", "Carat", "ct", 0.0002),
        ),
        // Temperatures are not linear in a shared base; see [convert].
        UnitCategory.Temperature to listOf(u("c", "Celsius", "°C"), u("f", "Fahrenheit", "°F"), u("k", "Kelvin", "K")),
        UnitCategory.Speed to listOf(
            u("ms", "Meter per second", "m/s"), u("kmh", "Kilometer per hour", "km/h", 1 / 3.6),
            u("mph", "Mile per hour", "mph", 0.44704), u("kn", "Knot", "kn", 1852.0 / 3600),
            u("fts", "Foot per second", "ft/s", 0.3048), u("mach", "Mach", "Ma", 340.29),
        ),
        UnitCategory.Pressure to listOf(
            u("pa", "Pascal", "Pa"), u("hpa", "Hectopascal", "hPa", 100.0), u("kpa", "Kilopascal", "kPa", 1000.0),
            u("mpa", "Megapascal", "MPa", 1e6), u("bar", "Bar", "bar", 1e5), u("atm", "Atmosphere", "atm", 101325.0),
            u("psi", "Pound per square inch", "psi", 6894.757293168), u("mmhg", "Millimeter of mercury", "mmHg", 133.322387415),
        ),
        UnitCategory.Power to listOf(
            u("w", "Watt", "W"), u("kw", "Kilowatt", "kW", 1000.0), u("mw", "Megawatt", "MW", 1e6),
            u("hp", "Horsepower", "hp", 745.69987158227), u("ps", "Metric horsepower", "PS", 735.49875),
            u("btuh", "BTU per hour", "BTU/h", 0.29307107), u("ftlbs", "Foot-pound per second", "ft·lbf/s", 1.3558179483314),
        ),
    )

    /** What a category opens with: the first unit and the one shown below it. */
    fun defaults(category: UnitCategory): Pair<ConversionUnit, ConversionUnit> {
        val units = checkNotNull(byCategory[category])
        val second = when (category) {
            UnitCategory.Length -> units.first { it.id == "mi" }
            UnitCategory.Temperature -> units[1]
            else -> units[1]
        }
        return units.first() to second
    }

    private fun toCelsius(unit: ConversionUnit, v: Double) = when (unit.id) {
        "f" -> (v - 32) * 5 / 9
        "k" -> v - 273.15
        else -> v
    }

    private fun fromCelsius(unit: ConversionUnit, c: Double) = when (unit.id) {
        "f" -> c * 9 / 5 + 32
        "k" -> c + 273.15
        else -> c
    }

    fun convert(category: UnitCategory, value: Double, from: ConversionUnit, to: ConversionUnit): Double =
        if (category == UnitCategory.Temperature) fromCelsius(to, toCelsius(from, value))
        else value * from.perBase / to.perBase
}

enum class NumberSystem(val radix: Int, val label: String, val abbreviation: String) {
    Binary(2, "Binary", "BIN"),
    Octal(8, "Octal", "OCT"),
    Decimal(10, "Decimal", "DEC"),
    Hexadecimal(16, "Hexadecimal", "HEX"),
}

object NumberSystems {
    /** Converts a whole number written in [from] to [to], or null when [text] is empty or not valid in [from]. */
    fun convert(text: String, from: NumberSystem, to: NumberSystem): String? {
        if (text.isEmpty()) return null
        val value = runCatching { BigInteger(text, from.radix) }.getOrNull() ?: return null
        return value.toString(to.radix).uppercase()
    }

    fun digitsFor(system: NumberSystem): String = "0123456789ABCDEF".take(system.radix)
}
