package com.mliem.scientific_calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mliem.scientific_calculator.engine.Const
import com.mliem.scientific_calculator.engine.ConstantName
import com.mliem.scientific_calculator.engine.Cursor
import com.mliem.scientific_calculator.engine.Exponent
import com.mliem.scientific_calculator.engine.Factorial
import com.mliem.scientific_calculator.engine.Fraction
import com.mliem.scientific_calculator.engine.Func
import com.mliem.scientific_calculator.engine.FunctionName
import com.mliem.scientific_calculator.engine.Ncr
import com.mliem.scientific_calculator.engine.Node
import com.mliem.scientific_calculator.engine.Npr
import com.mliem.scientific_calculator.engine.Num
import com.mliem.scientific_calculator.engine.Op
import com.mliem.scientific_calculator.engine.Operator
import com.mliem.scientific_calculator.engine.Paren
import com.mliem.scientific_calculator.engine.Percent
import com.mliem.scientific_calculator.engine.Radical
import com.mliem.scientific_calculator.engine.Segment
import com.mliem.scientific_calculator.engine.Slot
import kotlinx.coroutines.delay

/**
 * What the renderer needs besides the tree. A null [cursor] draws no caret and a null [onJump] makes
 * the expression read only, as in the history list.
 */
@Immutable
class MathStyle(
    val color: Color,
    val dim: Color,
    val accent: Color,
    val cursor: Cursor? = null,
    val onJump: ((List<Segment>, Int) -> Unit)? = null,
)

private const val EXPONENT_SCALE = 0.65f
private const val NESTED_FRACTION_SCALE = 0.85f
private const val LABEL_SCALE = 0.6f

/** Draws [nodes] as written: stacked fractions, raised powers and roots under a bar. [size] is in sp. */
@Composable
fun MathNodes(nodes: List<Node>, size: Float, style: MathStyle, path: List<Segment> = emptyList()) {
    val cursor = style.cursor
    val caretAt = cursor?.takeIf { it.path == path }?.insertAt
    val jump = style.onJump
    var modifier: Modifier = Modifier
    if (path.isNotEmpty()) {
        // An empty slot shows a dashed box, and tapping any slot's free space puts the caret at its end.
        if (nodes.isEmpty()) modifier = modifier.emptySlot(size, style.dim)
        if (jump != null) modifier = modifier.tappable { jump(path, nodes.size) }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        nodes.forEachIndexed { index, node ->
            if (caretAt == index) Caret(size, style, nodes)
            NodeView(node, index, path, size, style)
        }
        if (caretAt == nodes.size) Caret(size, style, nodes)
    }
}

@Composable
private fun NodeView(node: Node, index: Int, path: List<Segment>, size: Float, style: MathStyle) {
    val tap = style.onJump?.let { jump -> Modifier.tappable { jump(path, index + 1) } } ?: Modifier
    fun slot(slot: Slot) = path + Segment(index, slot)
    when (node) {
        is Num -> MathText(node.value, size, style.color, tap)
        is Op -> MathText(
            if (node.op == Operator.Minus) "−" else node.op.symbol.toString(), size, style.color,
            tap.padding(horizontal = (size * 0.18f).dp),
        )
        is Const -> MathText(constantLabel(node.name), size, style.color, tap, italic = true)
        is Fraction -> {
            val inner = if (path.isEmpty()) size else size * NESTED_FRACTION_SCALE
            Column(
                Modifier.width(IntrinsicSize.Max).padding(horizontal = (size * 0.08f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.padding(horizontal = 4.dp).defaultMinSize(minWidth = 16.dp), contentAlignment = Alignment.Center) {
                    MathNodes(node.numerator, inner, style, slot(Slot.Numerator))
                }
                Box(Modifier.fillMaxWidth().height(maxOf(1f, inner * 0.045f).dp).background(style.color))
                Box(Modifier.padding(horizontal = 4.dp).defaultMinSize(minWidth = 16.dp), contentAlignment = Alignment.Center) {
                    MathNodes(node.denominator, inner, style, slot(Slot.Denominator))
                }
            }
        }
        is Exponent -> Row(verticalAlignment = Alignment.Top) {
            MathNodes(node.base, size, style, slot(Slot.Base))
            Box(Modifier.padding(start = 1.dp)) { MathNodes(node.exponent, size * EXPONENT_SCALE, style, slot(Slot.Exponent)) }
        }
        is Radical -> Row(verticalAlignment = Alignment.Bottom) {
            if (node.degree.isNotEmpty() || style.cursor?.path == slot(Slot.Degree)) {
                Box(Modifier.padding(end = 1.dp, bottom = 4.dp)) { MathNodes(node.degree, size * EXPONENT_SCALE, style, slot(Slot.Degree)) }
            }
            MathText("√", size * 1.25f, style.color)
            Box(
                Modifier
                    .drawBehind { drawLine(style.color, Offset(0f, 0f), Offset(this.size.width, 0f), 1.dp.toPx()) }
                    .padding(horizontal = 2.dp),
            ) { MathNodes(node.radicand, size, style, slot(Slot.Radicand)) }
        }
        is Func -> Row(verticalAlignment = Alignment.CenterVertically) {
            MathText(functionLabel(node.name) + "(", size * LABEL_SCALE, style.color)
            MathNodes(node.argument, size, style, slot(Slot.Argument))
            MathText(")", size * LABEL_SCALE, style.color)
        }
        is Factorial -> Row(verticalAlignment = Alignment.CenterVertically) {
            MathNodes(node.operand, size, style, slot(Slot.Operand))
            MathText("!", size, style.color)
        }
        is Percent -> Row(verticalAlignment = Alignment.CenterVertically) {
            MathNodes(node.operand, size, style, slot(Slot.Operand))
            MathText("%", size, style.color)
        }
        is Ncr -> Row(verticalAlignment = Alignment.CenterVertically) {
            MathNodes(node.n, size, style, slot(Slot.N))
            MathText("C", size * EXPONENT_SCALE, style.color, Modifier.padding(horizontal = 2.dp))
            MathNodes(node.r, size, style, slot(Slot.R))
        }
        is Npr -> Row(verticalAlignment = Alignment.CenterVertically) {
            MathNodes(node.n, size, style, slot(Slot.N))
            MathText("P", size * EXPONENT_SCALE, style.color, Modifier.padding(horizontal = 2.dp))
            MathNodes(node.r, size, style, slot(Slot.R))
        }
        is Paren -> Row(verticalAlignment = Alignment.CenterVertically) {
            MathText("(", size, style.color)
            MathNodes(node.children, size, style, slot(Slot.Children))
            MathText(")", size, style.color)
        }
    }
}

@Composable
private fun MathText(text: String, size: Float, color: Color, modifier: Modifier = Modifier, italic: Boolean = false) {
    BasicText(
        text,
        modifier,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = FontWeight.Light,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        ),
        softWrap = false,
        maxLines = 1,
    )
}

@Composable
private fun Caret(size: Float, style: MathStyle, scope: List<Node>) {
    var visible by remember { mutableStateOf(true) }
    val requester = remember { BringIntoViewRequester() }
    // The caret sits solid while keys arrive, then blinks; the tree changes on every edit, so it restarts.
    LaunchedEffect(scope, style.cursor) {
        visible = true
        requester.bringIntoView()
        while (true) {
            delay(530)
            visible = !visible
        }
    }
    val height = with(LocalDensity.current) { (size * 1.1f).sp.toDp() }
    Box(
        Modifier
            .bringIntoViewRequester(requester)
            .width(maxOf(2f, size * 0.05f).dp)
            .height(height)
            .background(if (visible) style.accent else Color.Transparent),
    )
}

private fun Modifier.emptySlot(size: Float, color: Color) = defaultMinSize((size * 0.7f).dp, (size * 0.9f).dp).drawBehind {
    val stroke = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
    drawRect(color, size = Size(this.size.width, this.size.height), style = stroke)
}

private fun Modifier.tappable(onClick: () -> Unit) = clickable(interactionSource = null, indication = null, onClick = onClick)

private fun constantLabel(name: ConstantName) = when (name) {
    ConstantName.Pi -> "π"
    ConstantName.E -> "e"
    else -> name.name
}

private fun functionLabel(name: FunctionName) = when (name) {
    FunctionName.Asin -> "sin⁻¹"
    FunctionName.Acos -> "cos⁻¹"
    FunctionName.Atan -> "tan⁻¹"
    FunctionName.Exp -> "eˣ"
    else -> name.name.lowercase()
}
