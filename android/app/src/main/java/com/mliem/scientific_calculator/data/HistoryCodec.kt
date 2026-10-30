package com.mliem.scientific_calculator.data

import com.mliem.scientific_calculator.engine.ConstantName
import com.mliem.scientific_calculator.engine.Const
import com.mliem.scientific_calculator.engine.Exponent
import com.mliem.scientific_calculator.engine.Factorial
import com.mliem.scientific_calculator.engine.Fraction
import com.mliem.scientific_calculator.engine.Func
import com.mliem.scientific_calculator.engine.FunctionName
import com.mliem.scientific_calculator.engine.HistoryEntry
import com.mliem.scientific_calculator.engine.Ncr
import com.mliem.scientific_calculator.engine.Node
import com.mliem.scientific_calculator.engine.Npr
import com.mliem.scientific_calculator.engine.Num
import com.mliem.scientific_calculator.engine.Op
import com.mliem.scientific_calculator.engine.Operator
import com.mliem.scientific_calculator.engine.Paren
import com.mliem.scientific_calculator.engine.Percent
import com.mliem.scientific_calculator.engine.Radical
import org.json.JSONArray
import org.json.JSONObject

/** JSON for history entries. Every node is `{"t": type, ...fields}`, with child lists as arrays. */
object HistoryCodec {
    fun encode(entries: List<HistoryEntry>): String = JSONArray().also { array ->
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("result", entry.result)
                    .put("time", entry.timestamp)
                    .put("expr", encodeNodes(entry.expression)),
            )
        }
    }.toString()

    fun decode(json: String): List<HistoryEntry> {
        val array = JSONArray(json)
        return List(array.length()) { i ->
            val o = array.getJSONObject(i)
            HistoryEntry(o.getString("id"), decodeNodes(o.getJSONArray("expr")), o.getString("result"), o.getLong("time"))
        }
    }

    private fun encodeNodes(nodes: List<Node>) = JSONArray().also { array -> nodes.forEach { array.put(encodeNode(it)) } }

    private fun encodeNode(node: Node): JSONObject = when (node) {
        is Num -> JSONObject().put("t", "num").put("v", node.value)
        is Op -> JSONObject().put("t", "op").put("v", node.op.name)
        is Const -> JSONObject().put("t", "const").put("v", node.name.name)
        is Fraction -> JSONObject().put("t", "frac").put("a", encodeNodes(node.numerator)).put("b", encodeNodes(node.denominator))
        is Exponent -> JSONObject().put("t", "pow").put("a", encodeNodes(node.base)).put("b", encodeNodes(node.exponent))
        is Radical -> JSONObject().put("t", "root").put("a", encodeNodes(node.degree)).put("b", encodeNodes(node.radicand))
        is Func -> JSONObject().put("t", "fn").put("v", node.name.name).put("a", encodeNodes(node.argument))
        is Factorial -> JSONObject().put("t", "fact").put("a", encodeNodes(node.operand))
        is Percent -> JSONObject().put("t", "pct").put("a", encodeNodes(node.operand))
        is Ncr -> JSONObject().put("t", "ncr").put("a", encodeNodes(node.n)).put("b", encodeNodes(node.r))
        is Npr -> JSONObject().put("t", "npr").put("a", encodeNodes(node.n)).put("b", encodeNodes(node.r))
        is Paren -> JSONObject().put("t", "paren").put("a", encodeNodes(node.children))
    }

    private fun decodeNodes(array: JSONArray) = List(array.length()) { decodeNode(array.getJSONObject(it)) }

    private fun decodeNode(o: JSONObject): Node {
        fun a() = decodeNodes(o.getJSONArray("a"))
        fun b() = decodeNodes(o.getJSONArray("b"))
        return when (val type = o.getString("t")) {
            "num" -> Num(o.getString("v"))
            "op" -> Op(Operator.valueOf(o.getString("v")))
            "const" -> Const(ConstantName.valueOf(o.getString("v")))
            "frac" -> Fraction(a(), b())
            "pow" -> Exponent(a(), b())
            "root" -> Radical(a(), b())
            "fn" -> Func(FunctionName.valueOf(o.getString("v")), a())
            "fact" -> Factorial(a())
            "pct" -> Percent(a())
            "ncr" -> Ncr(a(), b())
            "npr" -> Npr(a(), b())
            "paren" -> Paren(a())
            else -> throw IllegalArgumentException("Unknown node type $type")
        }
    }
}
