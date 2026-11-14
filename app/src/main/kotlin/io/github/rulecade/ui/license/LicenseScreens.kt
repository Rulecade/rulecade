package io.github.rulecade.ui.license

import android.graphics.Paint
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rulecade.R
import io.github.rulecade.quiz.License
import io.github.rulecade.ui.component.COMPACT_WIDTH
import io.github.rulecade.ui.component.RcButton
import io.github.rulecade.ui.component.RcButtonStyle
import io.github.rulecade.ui.component.RcProgressBar
import io.github.rulecade.ui.home.CourseSummary
import io.github.rulecade.ui.home.label
import io.github.rulecade.ui.theme.Radius
import io.github.rulecade.ui.theme.RcColors
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Sizes
import io.github.rulecade.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val TEXT_RISE = 24.dp
private const val CARD_START_SCALE = 0.9f
private val COMPACT_CARD_MAX_WIDTH = 680.dp

class AwardStats(val solved: Int, val total: Int, val accuracy: Int?, val next: CourseSummary?)

@Composable
fun earnedText(isoDate: String): String {
    val pattern = stringResource(R.string.license_date_pattern)
    return LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern(pattern))
}

// The system animation scale: 0 turns the fireworks off, other values stretch them.
@Composable
private fun animationScale(): Float {
    val resolver = LocalContext.current.contentResolver
    return remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
}

// Shown once, right after the round that completes a course.
@Composable
fun LicenseAwardScreen(
    license: License,
    owner: String,
    earned: String,
    signs: List<CardSign>,
    stats: AwardStats,
    onResult: () -> Unit,
    onHome: () -> Unit
) {
    val colors = RulecadeTheme.colors
    val scale = animationScale()
    val animate = scale > 0f
    val clock =
        remember { mutableFloatStateOf(if (animate) 0f else Fireworks.END_MILLIS.toFloat()) }
    var done by remember { mutableStateOf(!animate) }
    BackHandler(onBack = onResult)
    LaunchedEffect(Unit) {
        if (!animate) return@LaunchedEffect
        val start = withFrameMillis { it }
        // A skip (a tap) sets the clock to the end; a frame must not undo it.
        while (!done && clock.floatValue < Fireworks.END_MILLIS) {
            withFrameMillis { if (!done) clock.floatValue = (it - start) / scale }
        }
        done = true
    }
    val skip = {
        clock.floatValue = Fireworks.END_MILLIS.toFloat()
        done = true
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(listOf(colors.accentSoft, Color.Transparent), radius = 1100f)
            )
    ) {
        // A split window stacks the card over the text; the dots that settle into decoration
        // would land on that text there, so they are left out.
        val compact = maxWidth < COMPACT_WIDTH
        val cardWidth = maxWidth - Spacing.extraLarge * 2
        // The settling dots become this decoration, so it appears when they land.
        if (done) {
            if (!compact) StaticDecor(colors)
        } else {
            val frame = remember { Fireworks.Frame() }
            val paint = remember { Paint().apply { strokeCap = Paint.Cap.SQUARE } }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        remember {
                            MutableInteractionSource()
                        },
                        indication = null,
                        onClick = skip
                    )
            ) {
                Fireworks.fill(clock.floatValue, frame)
                drawFrame(frame, paint, colors, settlers = !compact)
            }
        }
        val cardIn = Modifier.graphicsLayer {
            val p = Fireworks.progress(clock.floatValue, 0, Fireworks.CARD_IN_END)
            scaleX = CARD_START_SCALE + (1f - CARD_START_SCALE) * p
            scaleY = scaleX
            alpha = p
            compositingStrategy = CompositingStrategy.ModulateAlpha
        }
        val textIn = Modifier.graphicsLayer {
            val p = Fireworks.progress(
                clock.floatValue,
                Fireworks.TEXT_IN_START,
                Fireworks.TEXT_IN_END
            )
            translationY = TEXT_RISE.toPx() * (1f - p)
            alpha = p
            compositingStrategy = CompositingStrategy.ModulateAlpha
        }
        if (compact) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.extraLarge),
                verticalArrangement = Arrangement.spacedBy(Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val width = minOf(cardWidth, COMPACT_CARD_MAX_WIDTH)
                LicenseCard(
                    license,
                    owner,
                    earned,
                    signs,
                    Modifier.fitCardWidth(width).then(cardIn)
                )
                AwardText(license, stats, onResult, onHome, Modifier.fillMaxWidth().then(textIn))
            }
        } else {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = Spacing.screenHorizontal,
                        vertical = Spacing.screenVertical
                    ),
                horizontalArrangement = Arrangement.spacedBy(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LicenseCard(license, owner, earned, signs, cardIn)
                AwardText(license, stats, onResult, onHome, Modifier.weight(1f).then(textIn))
            }
        }
    }
}

@Composable
private fun AwardText(
    license: License,
    stats: AwardStats,
    onResult: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier
) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        Box(
            Modifier
                .background(colors.accentSoft, CircleShape)
                .padding(horizontal = Spacing.medium, vertical = Spacing.small)
        ) {
            Text(
                stringResource(R.string.license_award_chip),
                style = type.label,
                color = colors.accent
            )
        }
        Text(stringResource(R.string.license_award_title), style = type.headlineLarge)
        Text(
            stringResource(R.string.license_award_body, stringResource(license.label), stats.total),
            style = type.bodyLarge,
            color = colors.textSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Stat("${stats.solved}/${stats.total}", stringResource(R.string.license_stat_questions))
            if (stats.accuracy != null) {
                Stat(
                    stringResource(R.string.license_stat_percent, stats.accuracy),
                    stringResource(R.string.license_stat_accuracy)
                )
            }
        }
        stats.next?.let { next ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceCard, RoundedCornerShape(Radius.card))
                    .padding(horizontal = Spacing.large, vertical = Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.license_next),
                        style = type.bodyLarge,
                        color = colors.textTertiary
                    )
                    Box(Modifier.weight(1f))
                    Text(
                        stringResource(next.license.label) + "  " +
                            stringResource(R.string.home_progress, next.solved, next.total),
                        style = type.bodyLarge
                    )
                }
                RcProgressBar(next.share, Modifier.fillMaxWidth())
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            RcButton(
                text = stringResource(R.string.quiz_see_result),
                onClick = onResult,
                modifier = Modifier.weight(1f),
                style = RcButtonStyle.Accent,
                large = true
            )
            RcButton(
                text = stringResource(R.string.quiz_home),
                onClick = onHome,
                modifier = Modifier.weight(1f),
                large = true
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    Column(
        Modifier
            .widthIn(min = 200.dp)
            .background(colors.surfaceCard, RoundedCornerShape(Radius.tile))
            .padding(Spacing.medium)
    ) {
        Text(value, style = type.headlineSmall)
        Text(label, style = type.bodyMedium, color = colors.textTertiary)
    }
}

private fun RcColors.tint(index: Int) = when (Fireworks.Tint.entries[index]) {
    Fireworks.Tint.Accent -> accent
    Fireworks.Tint.Gold -> gradeGoldLine
    Fireworks.Tint.Silver -> gradeSilver
    Fireworks.Tint.Flash -> accentFlash
}

// The dots the fireworks leave behind; also drawn alone when the animation is skipped.
@Composable
private fun StaticDecor(colors: RcColors) {
    Canvas(Modifier.fillMaxSize()) {
        scaled {
            Fireworks.decor.forEach { d ->
                drawRoundRect(
                    colors.tint(d.tint.ordinal),
                    Offset(d.x, d.y),
                    Size(d.size, d.size),
                    CornerRadius(d.size * 0.2f),
                    alpha = Fireworks.DECOR_ALPHA
                )
            }
        }
    }
}

// The drawing keeps its proportions and is centered, so dots stay square in any window.
private inline fun DrawScope.scaled(block: DrawScope.() -> Unit) {
    val s = minOf(size.width / Fireworks.WIDTH, size.height / Fireworks.HEIGHT)
    withTransform({
        translate(
            (size.width - Fireworks.WIDTH * s) / 2f,
            (size.height - Fireworks.HEIGHT * s) / 2f
        )
        scale(s, s, Offset.Zero)
    }, block)
}

private fun DrawScope.drawFrame(
    frame: Fireworks.Frame,
    paint: Paint,
    colors: RcColors,
    settlers: Boolean
) = scaled {
    val canvas = drawContext.canvas.nativeCanvas
    for (bucket in frame.counts.indices) {
        val n = frame.counts[bucket]
        if (n == 0) continue
        paint.color = colors.tint(Fireworks.Frame.tintOf(bucket))
            .copy(alpha = Fireworks.Frame.alphaOf(bucket)).toArgb()
        paint.strokeWidth = Fireworks.Frame.sizeOf(bucket)
        canvas.drawPoints(frame.points[bucket], 0, n * 2, paint)
    }
    for (k in 0 until if (settlers) frame.settled else 0) {
        val s = frame.settlerSize[k]
        drawRoundRect(
            colors.tint(frame.settlerTint[k]),
            Offset(frame.settlerX[k] - s / 2f, frame.settlerY[k] - s / 2f),
            Size(s, s),
            CornerRadius(s * 0.2f),
            alpha = frame.settlerAlpha[k]
        )
    }
}

// Reopened from home; the same card, without fireworks.
@Composable
fun LicenseCardScreen(
    license: License,
    owner: String,
    earned: String,
    signs: List<CardSign>,
    solvedTotal: Int,
    onClose: () -> Unit
) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    BackHandler(onBack = onClose)
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        val compact = maxWidth < COMPACT_WIDTH
        val cardWidth = maxWidth - Spacing.extraLarge * 2
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    if (compact) {
                        PaddingValues(Spacing.extraLarge)
                    } else {
                        PaddingValues(
                            horizontal = Spacing.screenHorizontal,
                            vertical = Spacing.screenVertical
                        )
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(
                if (compact) Spacing.large else Spacing.extraLarge
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                RcButton(
                    text = stringResource(R.string.back),
                    onClick = onClose,
                    icon = R.drawable.ic_back,
                    iconOnly = true
                )
                Text(stringResource(R.string.license_card_title), style = type.titleSmall)
            }
            if (compact) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.large)
                ) {
                    LicenseCard(
                        license,
                        owner,
                        earned,
                        signs,
                        Modifier.fitCardWidth(cardWidth)
                    )
                    CardText(license, earned, solvedTotal, onClose, compact = true)
                }
            } else {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(64.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LicenseCard(license, owner, earned, signs)
                    Column(Modifier.weight(1f)) {
                        CardText(license, earned, solvedTotal, onClose, compact = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun CardText(
    license: License,
    earned: String,
    solvedTotal: Int,
    onClose: () -> Unit,
    compact: Boolean
) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.large)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Icon(
                painterResource(R.drawable.ic_crown),
                contentDescription = null,
                modifier = Modifier.size(Sizes.icon),
                tint = colors.gradeGold
            )
            Text(
                stringResource(R.string.quiz_result_license, stringResource(license.label)),
                style = type.titleSmall
            )
        }
        Text(
            stringResource(R.string.license_card_body, earned, solvedTotal),
            style = type.bodyLarge,
            color = colors.textSecondary
        )
        RcButton(
            text = stringResource(R.string.license_close),
            onClick = onClose,
            modifier = if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(min = 320.dp),
            large = true
        )
    }
}
