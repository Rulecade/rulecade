package io.github.rulecade.ui.license

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.rulecade.R
import io.github.rulecade.quiz.License
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// Safety signs from Annex 6 of the Road Traffic Act Enforcement Rule (amended 2024-11-14).
// Shapes and symbols follow the annex drawings; the annex leaves panel colors to the police
// standard, so they were matched against published images of the same designs.
enum class RoadSign(val number: Int) {
    Crossroads(101),
    Roundabout(109),
    CautionCrosswalk(132),
    Children(133),
    NoPassing(217),
    NoStopping(218),
    NoParking(219),
    SpeedLimit(224),
    MinimumSpeed(225),
    Crosswalk(322),
    UnprotectedLeft(329);

    companion object {
        fun of(number: Int) = entries.firstOrNull { it.number == number }
    }
}

val License.sign: RoadSign
    get() = when (this) {
        License.Highway -> RoadSign.SpeedLimit
        License.Intersection -> RoadSign.Crossroads
        License.SchoolZone -> RoadSign.Children
        License.City -> RoadSign.Crosswalk
    }

private object SignColors {
    val caution = Color(0xFFF9A70C)
    val red = Color(0xFFD21D24)
    val blue = Color(0xFF1E50A3)
    val edge = Color.Black
    val white = Color.White
    val symbol = Color.Black
}

// Highway course questions use the 100 km/h limit of motorways with two or more lanes and the
// 50 km/h minimum of highways.
private const val HIGHWAY_LIMIT = "100"
private const val HIGHWAY_MINIMUM = "50"

@Composable
fun RoadSignImage(sign: RoadSign, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val crosswalkText = stringResource(R.string.sign_crosswalk_text)
    val unprotectedText = stringResource(R.string.sign_unprotected_text)
    Canvas(modifier) {
        when (sign) {
            RoadSign.Crossroads -> {
                cautionTriangle()
                crossroads()
            }
            RoadSign.Roundabout -> {
                cautionTriangle()
                roundabout()
            }
            RoadSign.CautionCrosswalk -> {
                cautionTriangle()
                pedestrianOnCrosswalk()
            }
            RoadSign.Children -> {
                cautionTriangle()
                children()
            }
            RoadSign.NoPassing -> {
                speedLimitCircle()
                noPassing()
            }
            RoadSign.NoStopping -> {
                parkingCircle()
                parkingBars(cross = true)
            }
            RoadSign.NoParking -> {
                parkingCircle()
                parkingBars(cross = false)
            }
            RoadSign.SpeedLimit -> {
                speedLimitCircle()
                val text = measurer.measure(
                    HIGHWAY_LIMIT,
                    TextStyle(
                        fontSize = (size.minDimension * 0.30f).toSp(),
                        fontWeight = FontWeight.Black
                    )
                )
                drawText(
                    text,
                    SignColors.symbol,
                    center - Offset(text.size.width / 2f, text.size.height / 2f)
                )
            }
            RoadSign.MinimumSpeed -> {
                speedLimitCircle()
                val r = size.minDimension / 2f
                val text = measurer.measure(
                    HIGHWAY_MINIMUM,
                    TextStyle(fontSize = (r * 0.62f).toSp(), fontWeight = FontWeight.Black)
                )
                drawText(
                    text,
                    SignColors.symbol,
                    center - Offset(text.size.width / 2f, text.size.height / 2f + r * 0.10f)
                )
                // The bar under the number tells it from the maximum.
                drawRect(
                    SignColors.symbol,
                    Offset(center.x - r * 0.30f, center.y + r * 0.34f),
                    Size(r * 0.60f, r * 0.09f)
                )
            }
            RoadSign.UnprotectedLeft -> {
                instructionPanel()
                unprotectedLeft()
                val box = panelBox()
                val text = measurer.measure(
                    unprotectedText,
                    TextStyle(fontSize = (box.width * 0.24f).toSp(), fontWeight = FontWeight.Bold)
                )
                drawText(
                    text,
                    SignColors.white,
                    Offset(
                        box.left + (box.width - text.size.width) / 2f,
                        box.top + box.height * 0.72f - text.size.height / 2f
                    )
                )
            }
            RoadSign.Crosswalk -> {
                crosswalkPentagon()
                crosswalk()
                val text = measurer.measure(
                    crosswalkText,
                    TextStyle(fontSize = (size.width * 0.13f).toSp(), fontWeight = FontWeight.Bold)
                )
                val box = pentagonBox()
                drawText(
                    text,
                    SignColors.white,
                    Offset(
                        box.left + (box.width - text.size.width) / 2f,
                        box.top + box.height * 0.80f - text.size.height / 2f
                    )
                )
            }
        }
    }
}

// Rounded polygon through the given corners.
private fun roundedPolygon(points: List<Offset>, radius: Float) = Path().apply {
    val n = points.size
    for (i in 0 until n) {
        val prev = points[(i + n - 1) % n]
        val corner = points[i]
        val next = points[(i + 1) % n]
        val inStart = towards(corner, prev, radius)
        val outEnd = towards(corner, next, radius)
        if (i == 0) moveTo(inStart.x, inStart.y) else lineTo(inStart.x, inStart.y)
        quadraticTo(corner.x, corner.y, outEnd.x, outEnd.y)
    }
    close()
}

private fun towards(from: Offset, to: Offset, distance: Float): Offset {
    val length = hypot(to.x - from.x, to.y - from.y)
    return from + (to - from) * (distance / length)
}

// Moves every edge of a convex, clockwise polygon inwards by [d].
private fun inset(points: List<Offset>, d: Float): List<Offset> {
    val n = points.size
    val lines = (0 until n).map { i ->
        val a = points[i]
        val b = points[(i + 1) % n]
        val len = hypot(b.x - a.x, b.y - a.y)
        val normal = Offset(-(b.y - a.y) / len, (b.x - a.x) / len)
        a + normal * d to b + normal * d
    }
    return (0 until n).map { i -> intersect(lines[(i + n - 1) % n], lines[i]) }
}

private fun intersect(l1: Pair<Offset, Offset>, l2: Pair<Offset, Offset>): Offset {
    val (p, p2) = l1
    val (q, q2) = l2
    val r = p2 - p
    val s = q2 - q
    val denominator = r.x * s.y - r.y * s.x
    val t = ((q.x - p.x) * s.y - (q.y - p.y) * s.x) / denominator
    return p + r * t
}

private fun DrawScope.layers(
    outline: List<Offset>,
    bands: List<Pair<Float, Color>>,
    corner: Float
) {
    bands.forEach { (depth, color) ->
        val shape = if (depth == 0f) outline else inset(outline, depth)
        drawPath(roundedPolygon(shape, corner * (1f - depth / size.minDimension)), color)
    }
}

private fun DrawScope.triangle(): List<Offset> {
    val w = size.width
    val h = w * 0.866f
    val top = (size.height - h) / 2f
    return listOf(Offset(w / 2f, top), Offset(w, top + h), Offset(0f, top + h))
}

private fun DrawScope.cautionTriangle() {
    val w = size.width
    layers(
        triangle(),
        listOf(
            0f to SignColors.edge,
            w * 0.006f to SignColors.white,
            w * 0.018f to SignColors.red,
            w * 0.105f to SignColors.caution
        ),
        corner = w * 0.07f
    )
}

private fun DrawScope.symbolOrigin(): Pair<Offset, Float> {
    val t = triangle()
    val h = t[1].y - t[0].y
    return Offset(size.width / 2f, t[0].y + h * 0.64f) to size.width
}

private fun DrawScope.crossroads() {
    val (c, w) = symbolOrigin()
    val arm = w * 0.29f
    val bar = w * 0.089f
    drawRect(SignColors.symbol, Offset(c.x - arm / 2f, c.y - bar / 2f), Size(arm, bar))
    drawRect(SignColors.symbol, Offset(c.x - bar / 2f, c.y - arm * 0.54f), Size(bar, arm * 1.08f))
}

// An adult in a skirt and a child walking hand in hand, as in the annex drawing.
private fun DrawScope.children() {
    val (c, w) = symbolOrigin()
    val u = w / 100f
    val ink = SignColors.symbol
    fun p(x: Float, y: Float) = Offset(c.x + x * u, c.y + y * u)
    // Adult
    drawCircle(ink, 4.2f * u, p(-9f, -19f))
    drawPath(
        Path().apply {
            moveTo(p(-13f, -14f).x, p(-13f, -14f).y)
            lineTo(p(-5f, -14f).x, p(-5f, -14f).y)
            lineTo(p(-1f, 4f).x, p(-1f, 4f).y)
            lineTo(p(-17f, 4f).x, p(-17f, 4f).y)
            close()
        },
        ink
    )
    drawLine(ink, p(-12f, 3f), p(-15f, 17f), 2.6f * u)
    drawLine(ink, p(-6f, 3f), p(-3f, 17f), 2.6f * u)
    drawLine(ink, p(-13f, -11f), p(-21f, -4f), 2.2f * u)
    // Joined hands
    drawLine(ink, p(-5f, -11f), p(3f, -4f), 2.2f * u)
    // Child
    drawCircle(ink, 3.4f * u, p(9f, -10f))
    drawPath(
        Path().apply {
            moveTo(p(5.5f, -6f).x, p(5.5f, -6f).y)
            lineTo(p(12.5f, -6f).x, p(12.5f, -6f).y)
            lineTo(p(12f, 6f).x, p(12f, 6f).y)
            lineTo(p(6f, 6f).x, p(6f, 6f).y)
            close()
        },
        ink
    )
    drawLine(ink, p(7f, 5f), p(5f, 17f), 2.4f * u)
    drawLine(ink, p(11f, 5f), p(13f, 17f), 2.4f * u)
    drawLine(ink, p(6f, -4f), p(3f, -4f), 2.2f * u)
    drawLine(ink, p(12f, -4f), p(16f, 2f), 2.0f * u)
}

private fun DrawScope.speedLimitCircle() {
    val r = size.minDimension / 2f
    drawCircle(SignColors.edge, r, center)
    drawCircle(SignColors.white, r * 300f / 302f, center)
    drawCircle(SignColors.red, r * 290f / 302f, center)
    drawCircle(SignColors.white, r * 220f / 302f, center)
}

private fun DrawScope.pentagonBox(): Rect {
    val h = size.height
    val w = h / 1.14f
    val left = (size.width - w) / 2f
    return Rect(left, 0f, left + w, h)
}

private fun DrawScope.crosswalkPentagon() {
    val b = pentagonBox()
    val shoulder = b.top + b.height * 0.735f
    val outline = listOf(
        Offset(b.center.x, b.top),
        Offset(b.right, shoulder),
        Offset(b.right, b.bottom),
        Offset(b.left, b.bottom),
        Offset(b.left, shoulder)
    )
    layers(
        outline,
        listOf(
            0f to SignColors.edge,
            b.width * 0.008f to SignColors.white,
            b.width * 0.04f to SignColors.blue
        ),
        corner = b.width * 0.05f
    )
}

private fun DrawScope.crosswalk() {
    val b = pentagonBox()
    val u = b.width / 100f
    val ink = SignColors.white
    fun p(x: Float, y: Float) = Offset(b.left + x * u, b.top + y * u)
    // Zebra stripes
    for (i in 0 until 4) {
        val x = 20f + i * 16f
        drawPath(
            Path().apply {
                moveTo(p(x + 3f, 74f).x, p(x + 3f, 74f).y)
                lineTo(p(x + 12f, 74f).x, p(x + 12f, 74f).y)
                lineTo(p(x + 10f, 82f).x, p(x + 10f, 82f).y)
                lineTo(p(x, 82f).x, p(x, 82f).y)
                close()
            },
            ink
        )
    }
    // Pedestrian walking right
    drawCircle(ink, 4.6f * u, p(52f, 25f))
    drawLine(ink, p(51f, 31f), p(49f, 52f), 6.4f * u)
    drawLine(ink, p(49f, 51f), p(40f, 72f), 3.6f * u)
    drawLine(ink, p(50f, 51f), p(60f, 64f), 3.6f * u)
    drawLine(ink, p(60f, 64f), p(66f, 72f), 3.6f * u)
    drawLine(ink, p(51f, 34f), p(40f, 44f), 2.8f * u)
    drawLine(ink, p(51f, 34f), p(60f, 44f), 2.8f * u)
}

// Three arrows turning counterclockwise around the centre, as in the annex drawing.
private fun DrawScope.roundabout() {
    val (c, w) = symbolOrigin()
    val u = w / 100f
    val radius = 12f * u
    val ink = SignColors.symbol
    fun at(degrees: Float, r: Float): Offset {
        val a = Math.toRadians(degrees.toDouble())
        return Offset(c.x + r * cos(a).toFloat(), c.y - r * sin(a).toFloat())
    }
    listOf(10f to 80f, 130f to 200f, 250f to 320f).forEach { (from, to) ->
        drawArc(
            ink,
            startAngle = -to,
            sweepAngle = to - from,
            useCenter = false,
            topLeft = Offset(c.x - radius, c.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 4.6f * u)
        )
        val outer = at(to, radius + 6.5f * u)
        val tip = at(to + 32f, radius)
        val inner = at(to, radius - 6.5f * u)
        drawPath(
            Path().apply {
                moveTo(outer.x, outer.y)
                lineTo(tip.x, tip.y)
                lineTo(inner.x, inner.y)
                close()
            },
            ink
        )
    }
}

// A person walking over zebra stripes, as in the caution sign for crosswalks.
private fun DrawScope.pedestrianOnCrosswalk() {
    val (c, w) = symbolOrigin()
    val u = w / 100f
    val ink = SignColors.symbol
    fun p(x: Float, y: Float) = Offset(c.x + x * u, c.y + y * u)
    for (i in 0 until 4) {
        val x = -21f + i * 11f
        drawPath(
            Path().apply {
                moveTo(p(x + 2f, 13f).x, p(x + 2f, 13f).y)
                lineTo(p(x + 9f, 13f).x, p(x + 9f, 13f).y)
                lineTo(p(x + 7f, 18f).x, p(x + 7f, 18f).y)
                lineTo(p(x, 18f).x, p(x, 18f).y)
                close()
            },
            ink
        )
    }
    drawCircle(ink, 3.6f * u, p(2f, -20f))
    drawLine(ink, p(1f, -15f), p(-1f, -1f), 5f * u)
    drawLine(ink, p(-1f, -2f), p(-8f, 11f), 3f * u)
    drawLine(ink, p(-1f, -2f), p(5f, 5f), 3f * u)
    drawLine(ink, p(5f, 5f), p(9f, 11f), 3f * u)
    drawLine(ink, p(1f, -13f), p(-7f, -6f), 2.4f * u)
    drawLine(ink, p(1f, -13f), p(7f, -6f), 2.4f * u)
}

// An arrow overtaking a car, behind the red bar of a prohibition.
private fun DrawScope.noPassing() {
    val r = size.minDimension / 2f
    val s = r / 100f
    val inner = r * 220f / 302f
    fun p(x: Float, y: Float) = Offset(center.x + x * s, center.y + y * s)
    val ink = SignColors.symbol
    clipPath(Path().apply { addOval(Rect(center, inner)) }) {
        drawLine(SignColors.red, p(-80f, -80f), p(80f, 80f), 12f * s)
    }
    val arrow = Path().apply {
        moveTo(p(-30f, 56f).x, p(-30f, 56f).y)
        lineTo(p(-30f, 28f).x, p(-30f, 28f).y)
        cubicTo(
            p(-30f, 12f).x,
            p(-30f, 12f).y,
            p(-42f, 8f).x,
            p(-42f, 8f).y,
            p(-40f, -6f).x,
            p(-40f, -6f).y
        )
        cubicTo(
            p(-38f, -18f).x,
            p(-38f, -18f).y,
            p(-30f, -20f).x,
            p(-30f, -20f).y,
            p(-30f, -36f).x,
            p(-30f, -36f).y
        )
    }
    drawPath(arrow, ink, style = Stroke(width = 10f * s))
    drawPath(
        Path().apply {
            moveTo(p(-30f, -64f).x, p(-30f, -64f).y)
            lineTo(p(-45f, -34f).x, p(-45f, -34f).y)
            lineTo(p(-15f, -34f).x, p(-15f, -34f).y)
            close()
        },
        ink
    )
    // A car seen from the front.
    drawRoundRect(ink, p(8f, -6f), Size(36f * s, 20f * s), CornerRadius(5f * s))
    drawPath(
        Path().apply {
            moveTo(p(13f, -5f).x, p(13f, -5f).y)
            lineTo(p(17f, -19f).x, p(17f, -19f).y)
            lineTo(p(35f, -19f).x, p(35f, -19f).y)
            lineTo(p(39f, -5f).x, p(39f, -5f).y)
            close()
        },
        ink
    )
    drawPath(
        Path().apply {
            moveTo(p(17f, -7f).x, p(17f, -7f).y)
            lineTo(p(20f, -16f).x, p(20f, -16f).y)
            lineTo(p(32f, -16f).x, p(32f, -16f).y)
            lineTo(p(35f, -7f).x, p(35f, -7f).y)
            close()
        },
        SignColors.white
    )
    drawCircle(SignColors.white, 3f * s, p(14f, 4f))
    drawCircle(SignColors.white, 3f * s, p(38f, 4f))
    drawRect(ink, p(10f, 13f), Size(7f * s, 7f * s))
    drawRect(ink, p(35f, 13f), Size(7f * s, 7f * s))
}

// The no-stopping and no-parking signs have a blue field inside the red ring.
private fun DrawScope.parkingCircle() {
    val r = size.minDimension / 2f
    drawCircle(SignColors.edge, r, center)
    drawCircle(SignColors.white, r * 300f / 302f, center)
    drawCircle(SignColors.red, r * 290f / 302f, center)
    drawCircle(SignColors.blue, r * 220f / 302f, center)
}

// A red cross for no stopping, a single bar for no parking. The annex also writes the words
// (주정차금지, 주차금지) across the field; they are left out here.
private fun DrawScope.parkingBars(cross: Boolean) {
    val r = size.minDimension / 2f
    val inner = r * 220f / 302f
    val band = r * 40f / 302f
    clipPath(Path().apply { addOval(Rect(center, inner)) }) {
        drawLine(SignColors.red, center - Offset(r, r), center + Offset(r, r), band)
        if (cross) drawLine(SignColors.red, center + Offset(r, -r), center + Offset(-r, r), band)
    }
}

private fun DrawScope.panelBox(): Rect {
    val h = size.height
    val w = h * 0.66f
    val left = (size.width - w) / 2f
    return Rect(left, 0f, left + w, h)
}

private fun DrawScope.instructionPanel() {
    val b = panelBox()
    layers(
        listOf(b.topLeft, b.topRight, b.bottomRight, b.bottomLeft),
        listOf(
            0f to SignColors.edge,
            b.width * 0.01f to SignColors.white,
            b.width * 0.05f to SignColors.blue
        ),
        corner = b.width * 0.08f
    )
}

// A left-turn arrow above the words; the words are drawn by the caller.
private fun DrawScope.unprotectedLeft() {
    val b = panelBox()
    val u = b.width / 100f
    fun p(x: Float, y: Float) = Offset(b.left + x * u, b.top + y * u)
    val ink = SignColors.white
    drawPath(
        Path().apply {
            moveTo(p(66f, 66f).x, p(66f, 66f).y)
            lineTo(p(66f, 38f).x, p(66f, 38f).y)
            quadraticTo(p(66f, 24f).x, p(66f, 24f).y, p(52f, 24f).x, p(52f, 24f).y)
            lineTo(p(40f, 24f).x, p(40f, 24f).y)
        },
        ink,
        style = Stroke(width = 9f * u)
    )
    drawPath(
        Path().apply {
            moveTo(p(20f, 24f).x, p(20f, 24f).y)
            lineTo(p(42f, 10f).x, p(42f, 10f).y)
            lineTo(p(42f, 38f).x, p(42f, 38f).y)
            close()
        },
        ink
    )
}
