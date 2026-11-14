package io.github.rulecade.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Sizes

private const val WORDMARK = "Rulecade"

// 5x7 dot font shared with the org icon. Only the wordmark's letters are defined.
internal object PixelFont {
    const val COLUMNS = 5
    const val ROWS = 7
    const val DOT_RATIO = 0.86f
    const val CORNER_RATIO = 0.18f

    val glyphs = mapOf(
        'R' to listOf("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
        'u' to listOf("00000", "00000", "10001", "10001", "10001", "10011", "01101"),
        'l' to listOf("01100", "00100", "00100", "00100", "00100", "00100", "01110"),
        'e' to listOf("00000", "00000", "01110", "10001", "11111", "10000", "01110"),
        'c' to listOf("00000", "00000", "01110", "10000", "10000", "10001", "01110"),
        'a' to listOf("00000", "00000", "01110", "00001", "01111", "10001", "01111"),
        'd' to listOf("00001", "00001", "01101", "10011", "10001", "10001", "01111")
    )

    fun width(text: String) = text.length * (COLUMNS + 1) - 1

    fun dots(text: String): List<Pair<Int, Int>> = text.flatMapIndexed { index, char ->
        val left = index * (COLUMNS + 1)
        glyphs.getValue(char).flatMapIndexed { y, row ->
            row.indices.filter { row[it] == '1' }.map { x -> left + x to y }
        }
    }
}

@Composable
fun PixelWordmark(
    modifier: Modifier = Modifier,
    cell: Dp = Sizes.wordmarkCell,
    color: Color = RulecadeTheme.colors.accent
) {
    val dots = remember { PixelFont.dots(WORDMARK) }
    Canvas(
        modifier
            .size(cell * PixelFont.width(WORDMARK), cell * PixelFont.ROWS)
            .semantics { contentDescription = WORDMARK }
    ) {
        val step = cell.toPx()
        val dot = Size(step * PixelFont.DOT_RATIO, step * PixelFont.DOT_RATIO)
        val corner = CornerRadius(step * PixelFont.CORNER_RATIO)
        dots.forEach { (x, y) -> drawRoundRect(color, Offset(x * step, y * step), dot, corner) }
    }
}
