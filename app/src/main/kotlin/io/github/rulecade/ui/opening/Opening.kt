package io.github.rulecade.ui.opening

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.rulecade.ui.component.PixelFont
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Sizes
import io.github.rulecade.ui.theme.StaticColors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.launch

// Cold-start opening: the org icon (dot R over a road) grows into the wordmark, then lands on
// the home top bar wordmark while home fades in. Times are in milliseconds.
internal object OpeningTimeline {
    const val ROAD_STEP = 18
    const val R_START = 120
    const val R_STEP = 22
    const val POP = 120
    const val SLIDE_START = 650
    const val SLIDE_END = 1250
    const val COLUMN_START = 700
    const val COLUMN_STEP = 13
    const val SCROLL_START = 300
    const val SCROLL_STEP = 80
    const val LAND_START = 1450
    const val LAND_END = 1800
    const val ROAD_FADE_END = 1650
    const val HOME_START = 1700
    const val END = 2000
}

internal const val WORDMARK = "Rulecade"

// R lights up in stroke order: stem, bowl, then the leg.
private val R_STROKES = listOf(
    0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6,
    1 to 0, 2 to 0, 3 to 0, 4 to 1, 4 to 2, 3 to 3, 2 to 3, 1 to 3,
    2 to 4, 3 to 5, 4 to 6
)

internal data class TimedDot(val x: Int, val y: Int, val start: Int)

internal fun openingDots(): List<TimedDot> {
    val r = R_STROKES.mapIndexed { i, (x, y) ->
        TimedDot(x, y, OpeningTimeline.R_START + i * OpeningTimeline.R_STEP)
    }
    val rest = PixelFont.dots(WORDMARK).filter { (x, _) -> x > PixelFont.COLUMNS }.map { (x, y) ->
        val column = x - PixelFont.COLUMNS
        TimedDot(x, y, OpeningTimeline.COLUMN_START + column * OpeningTimeline.COLUMN_STEP)
    }
    return r + rest
}

@Stable
class OpeningState(play: Boolean) {
    private val time = Animatable(if (play) 0f else OpeningTimeline.END.toFloat())

    // Top-left of the home wordmark in root coordinates: where the animation lands.
    var target by mutableStateOf<Offset?>(null)

    val millis: Float get() = time.value
    val isRunning: Boolean get() = time.value < OpeningTimeline.END
    val homeAlpha: Float
        get() = Emphasized.transform(
            progress(millis, OpeningTimeline.HOME_START, OpeningTimeline.END)
        )

    suspend fun play() {
        val remaining = OpeningTimeline.END - time.value
        if (remaining > 0) {
            time.animateTo(
                OpeningTimeline.END.toFloat(),
                tween(remaining.toInt(), easing = LinearEasing)
            )
        }
    }

    suspend fun skip() = time.snapTo(OpeningTimeline.END.toFloat())
}

@Composable
fun OpeningOverlay(state: OpeningState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val colors = RulecadeTheme.colors
    val accent = colors.accent
    val flash = colors.accentFlash
    val dots = remember { openingDots() }
    Canvas(
        modifier
            .fillMaxSize()
            .pointerInput(state) { detectTapGestures { scope.launch { state.skip() } } }
    ) {
        val bigCell = minOf(BIG_CELL.toPx(), size.width * WIDTH_SHARE / WORD_WIDTH)
        val landCell = Sizes.wordmarkCell.toPx()
        drawOpening(state.millis, bigCell, state.target, landCell, dots, accent, flash)
    }
}

private val BIG_CELL = 24.dp
private const val WIDTH_SHARE = 0.66f
private const val ROAD_CELL = 0.4f
private const val ROAD_TOP = 7.6f
private const val ROAD_ROWS = 5
private const val ROAD_ICON_CELLS = 17
private const val ROAD_MARGIN = 1f
private const val DASH_PERIOD = 5
private const val DASH_GAP = 2
private const val POP_SCALE = 0.5f
private const val FADE_IN_SPEED = 2.5f

private val ICON_WIDTH = PixelFont.COLUMNS - 1 + PixelFont.DOT_RATIO
private val WORD_WIDTH = PixelFont.width(WORDMARK) - 1 + PixelFont.DOT_RATIO
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun progress(t: Float, start: Int, end: Int) =
    ((t - start) / (end - start)).coerceIn(0f, 1f)

private fun outCubic(x: Float) = 1 - (1 - x) * (1 - x) * (1 - x)

private fun mix(a: Float, b: Float, p: Float) = a + (b - a) * p

private fun DrawScope.drawOpening(
    t: Float,
    bigCell: Float,
    landOrigin: Offset?,
    landCell: Float,
    dots: List<TimedDot>,
    accent: Color,
    flash: Color
) {
    val slide = Emphasized.transform(
        progress(t, OpeningTimeline.SLIDE_START, OpeningTimeline.SLIDE_END)
    )
    val landing = Emphasized.transform(
        progress(t, OpeningTimeline.LAND_START, OpeningTimeline.LAND_END)
    )

    // icon centred -> word centred -> home top bar
    val blockHeight = (ROAD_TOP + ROAD_ROWS * ROAD_CELL) * bigCell
    val iconX = size.width / 2 - ICON_WIDTH * bigCell / 2
    val wordX = size.width / 2 - WORD_WIDTH * bigCell / 2
    val centerY = size.height / 2 - blockHeight / 2
    val target = landOrigin ?: Offset(wordX, centerY)
    val cell = mix(bigCell, landCell, landing)
    val origin = Offset(
        mix(mix(iconX, wordX, slide), target.x, landing),
        mix(centerY, target.y, landing)
    )

    drawRoad(t, origin, cell, slide)

    val dot = cell * PixelFont.DOT_RATIO
    dots.forEach { d ->
        val p = ((t - d.start) / OpeningTimeline.POP).coerceIn(0f, 1f)
        if (p <= 0f) return@forEach
        val e = outCubic(p)
        val side = dot * mix(POP_SCALE, 1f, e)
        val center = origin + Offset(d.x * cell + dot / 2, d.y * cell + dot / 2)
        drawRoundRect(
            color = lerp(flash, accent, e),
            topLeft = center - Offset(side / 2, side / 2),
            size = Size(side, side),
            cornerRadius = CornerRadius(side * PixelFont.CORNER_RATIO / PixelFont.DOT_RATIO),
            alpha = (p * FADE_IN_SPEED).coerceAtMost(1f)
        )
    }
}

// Road under the logo: 17 cells under the R at first, widening to the word. The yellow centre
// line steps left one cell at a time, like the road scrolling past.
private fun DrawScope.drawRoad(t: Float, origin: Offset, cell: Float, slide: Float) {
    val fade = 1 - progress(t, OpeningTimeline.LAND_START, OpeningTimeline.ROAD_FADE_END)
    if (fade <= 0f) return
    val rc = cell * ROAD_CELL
    val firstCell = ICON_WIDTH / 2 - ROAD_ICON_CELLS / 2f * ROAD_CELL
    val wordFirst = floor((-ROAD_MARGIN - firstCell) / ROAD_CELL).toInt()
    val wordLast = ceil((WORD_WIDTH + ROAD_MARGIN - firstCell) / ROAD_CELL).toInt() - 1
    val first = Math.round(mix(0f, wordFirst.toFloat(), slide))
    val last = Math.round(mix(ROAD_ICON_CELLS - 1f, wordLast.toFloat(), slide))
    val middle = ROAD_ICON_CELLS / 2
    val phase = maxOf(0, ((t - OpeningTimeline.SCROLL_START) / OpeningTimeline.SCROLL_STEP).toInt())
    val dot = rc * PixelFont.DOT_RATIO
    for (k in first..last) {
        val shown = t - minOf(abs(k - middle), middle) * OpeningTimeline.ROAD_STEP
        if (shown < 0) continue
        val alpha = outCubic((shown / OpeningTimeline.POP).coerceIn(0f, 1f)) * fade
        val dash = Math.floorMod(k - phase, DASH_PERIOD) >= DASH_GAP
        val x = origin.x + firstCell * cell + k * rc
        for (row in 0 until ROAD_ROWS) {
            val color = when (row) {
                0, ROAD_ROWS - 1 -> StaticColors.roadEdge
                ROAD_ROWS / 2 -> if (dash) StaticColors.signYellow else StaticColors.road
                else -> StaticColors.road
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, origin.y + ROAD_TOP * cell + row * rc),
                size = Size(dot, dot),
                cornerRadius = CornerRadius(rc * PixelFont.CORNER_RATIO),
                alpha = alpha
            )
        }
    }
}
