package com.mliem.scientific_calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun num(v: String) = Num(v)
private fun op(o: Operator) = Op(o)
private val plus = op(Operator.Plus)

private fun eval(nodes: List<Node>, mode: AngleMode = AngleMode.Rad, answer: Double = 0.0, memory: Memory = Memory()) =
    evaluate(nodes, mode, answer, memory)

private fun value(nodes: List<Node>, mode: AngleMode = AngleMode.Rad): Double =
    (eval(nodes, mode) as Evaluation.Value).number

class EvaluatorTest {
    @Test fun emptyAndLoneOperatorAreIncomplete() {
        assertEquals(Evaluation.Incomplete, eval(emptyList()))
        assertEquals(Evaluation.Incomplete, eval(listOf(plus)))
        assertEquals(Evaluation.Incomplete, eval(listOf(num("2"), plus)))
    }

    @Test fun adjacentNumberNodesReadAsOneNumber() =
        assertEquals(975263.0, value(listOf(num("9"), num("75263"))), 0.0)

    @Test fun addition() = assertEquals(5.0, value(listOf(num("2"), plus, num("3"))), 0.0)

    @Test fun multiplicationBindsTighterThanAddition() =
        assertEquals(14.0, value(listOf(num("2"), plus, num("3"), op(Operator.Times), num("4"))), 0.0)

    @Test fun unaryMinusAfterAnOperator() =
        assertEquals(-6.0, value(listOf(num("3"), op(Operator.Times), op(Operator.Minus), num("2"))), 0.0)

    @Test fun implicitMultiplication() =
        assertEquals(6.0, value(listOf(num("2"), Paren(listOf(num("3"))))), 0.0)

    @Test fun fraction() =
        assertEquals(0.25, value(listOf(Fraction(listOf(num("1")), listOf(num("4"))))), 0.0)

    @Test fun emptyFractionSlotIsIncomplete() =
        assertEquals(Evaluation.Incomplete, eval(listOf(Fraction(listOf(num("1")), emptyList()))))

    @Test fun divisionByZeroIsAMathError() =
        assertEquals(Evaluation.MathError(MathErrorReason.DivideByZero), eval(listOf(num("1"), op(Operator.Divide), num("0"))))

    @Test fun negativeSquareRootIsAMathError() =
        assertEquals(Evaluation.MathError(MathErrorReason.NotANumber), eval(listOf(Radical(emptyList(), listOf(num("-4"))))))

    @Test fun squareRootPowerAndCubeRoot() {
        assertEquals(2.0, value(listOf(Radical(emptyList(), listOf(num("4"))))), 0.0)
        assertEquals(1024.0, value(listOf(Exponent(listOf(num("2")), listOf(num("10"))))), 0.0)
        assertEquals(-2.0, value(listOf(Radical(listOf(num("3")), listOf(num("-8"))))), 1e-12)
    }

    @Test fun negativeSignAppliesAfterPower() =
        assertEquals(-4.0, value(listOf(op(Operator.Minus), Exponent(listOf(num("2")), listOf(num("2"))))), 0.0)

    @Test fun factorialCombinationsPermutations() {
        assertEquals(120.0, value(listOf(Factorial(listOf(num("5"))))), 0.0)
        assertEquals(10.0, value(listOf(Ncr(listOf(num("5")), listOf(num("2"))))), 0.0)
        assertEquals(20.0, value(listOf(Npr(listOf(num("5")), listOf(num("2"))))), 0.0)
        assertTrue(eval(listOf(Ncr(listOf(num("2")), listOf(num("5"))))) is Evaluation.MathError)
        assertTrue(eval(listOf(Factorial(listOf(num("-1"))))) is Evaluation.MathError)
    }

    @Test fun factorialOfAFractionUsesTheGammaFunction() =
        assertEquals(0.886226925453, value(listOf(Factorial(listOf(num("0.5"))))), 1e-9)

    @Test fun trigonometryFollowsTheAngleMode() {
        assertEquals(1.0, value(listOf(Func(FunctionName.Sin, listOf(num("90")))), AngleMode.Deg), 1e-12)
        assertEquals(0.0, value(listOf(Func(FunctionName.Sin, listOf(num("180")))), AngleMode.Deg), 0.0)
        assertEquals(0.0, value(listOf(Func(FunctionName.Cos, listOf(num("90")))), AngleMode.Deg), 0.0)
        assertEquals(30.0, value(listOf(Func(FunctionName.Asin, listOf(num("0.5")))), AngleMode.Deg), 1e-9)
        assertEquals(0.0, value(listOf(Func(FunctionName.Sin, listOf(Const(ConstantName.Pi)))), AngleMode.Rad), 1e-12)
    }

    @Test fun tangentOfNinetyDegreesIsAMathError() =
        assertTrue(eval(listOf(Func(FunctionName.Tan, listOf(num("90")))), AngleMode.Deg) is Evaluation.MathError)

    @Test fun hyperbolicFunctionsIgnoreTheAngleMode() {
        val sinh = listOf(Func(FunctionName.Sinh, listOf(num("1"))))
        assertEquals(value(sinh, AngleMode.Rad), value(sinh, AngleMode.Deg), 0.0)
    }

    @Test fun logarithms() {
        assertEquals(2.0, value(listOf(Func(FunctionName.Log, listOf(num("100"))))), 1e-12)
        assertEquals(1.0, value(listOf(Func(FunctionName.Ln, listOf(Const(ConstantName.E))))), 1e-12)
    }

    @Test fun answerAndMemory() {
        assertEquals(6.0, (eval(listOf(Const(ConstantName.Ans), plus, num("1")), answer = 5.0) as Evaluation.Value).number, 0.0)
        assertEquals(3.0, value(listOf(Const(ConstantName.Ans), plus, num("3"))), 0.0)
        val memory = Memory().with(ConstantName.A, 7.0)
        assertEquals(7.0, (eval(listOf(Const(ConstantName.A)), memory = memory) as Evaluation.Value).number, 0.0)
        assertEquals(0.0, (eval(listOf(Const(ConstantName.B)), memory = memory) as Evaluation.Value).number, 0.0)
    }
}

class FormatTest {
    @Test fun wholeAndDecimalNumbers() {
        assertEquals("5", formatNumber(5.0))
        assertEquals("0.3", formatNumber(0.1 + 0.2))
        assertEquals("0", formatNumber(-0.0))
        assertEquals("3.141592654", formatNumber(Math.PI))
        assertEquals("-2.5", formatNumber(-2.5))
    }

    @Test fun veryLargeAndVerySmallUseExponents() {
        assertEquals("1e-7", formatNumber(1e-7))
        assertEquals("1.224646799e-16", formatNumber(1.2246467991473532e-16))
        assertEquals("1.23456789e+25", formatNumber(1.23456789e25))
        assertEquals("0.000001", formatNumber(1e-6))
    }

    @Test fun fixedPrecisionRoundsHalfUp() {
        assertEquals("3.1416", formatNumber(Math.PI, 4))
        assertEquals("2.5", formatNumber(2.5, 4))
        assertEquals("1", formatNumber(1.00004, 4))
    }

    @Test fun fractionConversion() {
        assertEquals("1/2", toFraction("0.5"))
        assertEquals("1/3", toFraction("0.3333333333"))
        assertEquals("-7/4", toFraction("-1.75"))
        assertNull(toFraction("3"))
        assertNull(toFraction("3.14159265359"))
    }

    @Test fun mixedNumbers() {
        assertEquals("2 1/3", toMixed("7/3"))
        assertNull(toMixed("1/3"))
        assertEquals("2", toMixed("6/3"))
        assertNull(toMixed("3.14"))
        assertEquals("7/3", fromMixed("2 1/3"))
        assertEquals("-7/3", fromMixed("-2 1/3"))
        assertNull(fromMixed("7/3"))
    }

    @Test fun spokenReadsFractionsAndPowers() {
        val text = listOf(Fraction(listOf(num("1")), listOf(num("4"))), plus, Exponent(listOf(num("2")), listOf(num("3")))).spoken()
        assertTrue(text, text.contains("(1) over (4)") && text.contains("2 to the power of 3"))
    }
}
