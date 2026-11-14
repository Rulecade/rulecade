package io.github.rulecade.ui.license

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rulecade.R
import io.github.rulecade.catalog.Grade
import io.github.rulecade.quiz.License
import io.github.rulecade.ui.component.PixelWordmark
import io.github.rulecade.ui.home.label
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.StaticColors
import kotlin.math.roundToInt

val CARD_WIDTH = 820.dp
val CARD_HEIGHT = 540.dp
private val CARD_RADIUS = 40.dp
private val CARD_LINE = 3.dp
private val SHIELD_WIDTH = 176.dp
private val SHIELD_HEIGHT = 194.dp
private val ROAD_CELL = 12.dp
private const val ROAD_ROWS = 5
private const val MUTED = 0.64f
private val SIGN_TILE = 72.dp
private val SIGN_TILE_RADIUS = 18.dp
private val SIGN_TILE_LINE = 3.dp
private val SIGN_SIZE = 52.dp
private const val SIGN_TILE_FILL = 0.06f

// A sign on the license card with its grade when the card is drawn.
data class CardSign(val sign: RoadSign, val grade: Grade)

// Shrinks the card to [width], keeping its proportions; it never grows past its own size.
fun Modifier.fitCardWidth(width: Dp): Modifier = layout { measurable, _ ->
    val fullWidth = CARD_WIDTH.roundToPx()
    val fullHeight = CARD_HEIGHT.roundToPx()
    val scale = (width.toPx() / fullWidth).coerceIn(0f, 1f)
    val placeable = measurable.measure(Constraints.fixed(fullWidth, fullHeight))
    layout((fullWidth * scale).roundToInt(), (fullHeight * scale).roundToInt()) {
        placeable.placeWithLayer(0, 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

// The share image. It looks the same in day and night and never animates by itself.
@Composable
fun LicenseCard(
    license: License,
    owner: String,
    earned: String,
    signs: List<CardSign>,
    modifier: Modifier = Modifier
) {
    val type = RulecadeTheme.typography
    val accent = StaticColors.cardAccent
    val shape = RoundedCornerShape(CARD_RADIUS)
    Box(
        modifier
            .size(CARD_WIDTH, CARD_HEIGHT)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    0f to Color(0xFF1E2A25),
                    0.7f to StaticColors.cardBackground,
                    start = Offset.Zero,
                    end = Offset(820f, 1100f)
                )
            )
            .background(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(148f, 216f),
                    radius = 450f
                )
            )
            .border(CARD_LINE, accent.copy(alpha = 0.7f), shape)
    ) {
        Column(
            Modifier.padding(start = 44.dp, end = 44.dp, top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PixelWordmark(cell = 5.dp, color = accent)
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.license_kind),
                    style = type.label.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 8.sp
                    ),
                    color = Color.White.copy(alpha = MUTED)
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Shield(license)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(license.label),
                        style = type.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        stringResource(R.string.license_word),
                        style = type.titleSmall,
                        color = accent
                    )
                    Text(
                        stringResource(R.string.license_owner_date, owner, earned),
                        style = type.bodyMedium,
                        color = Color.White.copy(alpha = MUTED),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            SignRow(signs)
        }
        PixelRoad(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(ROAD_CELL * ROAD_ROWS)
        )
    }
}

@Composable
private fun Shield(license: License) {
    Box(Modifier.size(SHIELD_WIDTH, SHIELD_HEIGHT)) {
        Canvas(Modifier.size(SHIELD_WIDTH, SHIELD_HEIGHT)) {
            val k = size.width / 200f
            fun shield(top: Float, side: Float, shoulder: Float, bottom: Float) = Path().apply {
                moveTo(100f * k, top * k)
                lineTo((200f - side) * k, (top + 36f) * k)
                lineTo((200f - side) * k, shoulder * k)
                cubicTo(
                    (200f - side) * k,
                    (shoulder + 52f) * k,
                    148f * k,
                    bottom * k,
                    100f * k,
                    (bottom + 14f) * k
                )
                cubicTo(52f * k, bottom * k, side * k, (shoulder + 52f) * k, side * k, shoulder * k)
                lineTo(side * k, (top + 36f) * k)
                close()
            }
            drawPath(shield(8f, 14f, 106f, 198f), StaticColors.cardAccent)
            drawPath(shield(26f, 30f, 106f, 180f), StaticColors.cardBackground)
        }
        RoadSignImage(
            license.sign,
            Modifier
                .offset(SHIELD_WIDTH * (40f / 200f), SHIELD_HEIGHT * (52f / 220f))
                .size(SHIELD_WIDTH * (120f / 200f))
        )
    }
}

// The course's signs, outlined gold when collected and in the accent when mastered.
@Composable
private fun SignRow(signs: List<CardSign>) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        signs.forEach { (sign, grade) ->
            val shape = RoundedCornerShape(SIGN_TILE_RADIUS)
            val line = when (grade) {
                Grade.Mastered -> StaticColors.cardAccent
                Grade.Collected -> StaticColors.cardGold
                Grade.Uncollected, Grade.Discovered -> Color.Transparent
            }
            Box(
                Modifier
                    .size(SIGN_TILE)
                    .background(Color.White.copy(alpha = SIGN_TILE_FILL), shape)
                    .border(SIGN_TILE_LINE, line, shape),
                contentAlignment = Alignment.Center
            ) {
                RoadSignImage(sign, Modifier.size(SIGN_SIZE))
            }
        }
    }
}

// Two-lane road with a dashed yellow center line along the card's bottom edge.
@Composable
private fun PixelRoad(modifier: Modifier) {
    Canvas(modifier) {
        val cell = ROAD_CELL.toPx()
        val gap = 2f
        val columns = (size.width / cell).toInt() + 1
        val asphalt = Color(0xFF24262B)
        for (row in 0 until ROAD_ROWS) {
            for (column in 0 until columns) {
                val color = when (row) {
                    0, ROAD_ROWS - 1 -> Color.White.copy(alpha = 0.55f)
                    ROAD_ROWS / 2 -> if (column % 5 < 3) StaticColors.signYellow else asphalt
                    else -> asphalt
                }
                drawRoundRect(
                    color,
                    Offset(column * cell + gap / 2f, row * cell + gap / 2f),
                    Size(cell - gap, cell - gap),
                    CornerRadius(gap)
                )
            }
        }
    }
}
