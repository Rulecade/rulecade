package io.github.rulecade.ui.quiz

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.rulecade.R
import io.github.rulecade.quiz.Arm
import io.github.rulecade.quiz.Crossing
import io.github.rulecade.quiz.CrossingCar
import io.github.rulecade.quiz.Lamp
import io.github.rulecade.quiz.Signal
import io.github.rulecade.quiz.Spot
import io.github.rulecade.quiz.Turn
import io.github.rulecade.ui.license.RoadSign
import io.github.rulecade.ui.license.RoadSignImage
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.StaticColors

// The plan is drawn in units of a 360 × 400 area and scaled to fit: two two-way roads, a 64-unit
// lane each way on either side of a double yellow center line, crossing in the middle.
private const val PLAN_WIDTH = 360f
private const val PLAN_HEIGHT = 400f
private const val CENTER_X = 180f
private const val CENTER_Y = 200f
private const val HALF_ROAD = 68f
private const val LANE_CENTER = 36f
private const val CENTER_LINE = 3f
private const val CENTER_GAP = 1f
private const val CROSSWALK_GAP = 4f
private const val CROSSWALK_DEPTH = 24f
private const val STRIPE = 8f
private const val STOP_LINE_GAP = 6f
private const val STOP_LINE = 4f
private const val CAR_WIDTH = 32f
private const val CAR_LENGTH = 48f
private const val CAR_GAP = 8f
private const val INSIDE = 20f
private const val CORNER = 16f
private const val PATH_START = 6f
private const val PATH_END = 8f
private const val PATH_WIDTH = 5f
private const val TURN_RADIUS = 26f
private const val LAMP_RADIUS = 9f
private const val LAMP_PITCH = 24f
private const val HEAD_HEIGHT = 30f
private const val HEAD_TOP = 8f
private const val HEAD_RIGHT = 12f
private const val SIGN_SIZE = 56f
private const val SIGN_LEFT = 262f
private const val SIGN_TOP = 300f

// Stripe offsets across a crosswalk, leaving the middle of the road clear.
private val STRIPES = listOf(4f, 20f, 36f, 52f, 76f, 92f, 108f, 124f)

// Like the highway scene, the picture looks the same in day and night.
private val GROUND = Color(0xFF2B2D33)
private val ASPHALT = Color(0xFF33373E)
private val LINE = Color(0xFFE8EAED)
private val HEAD = Color(0xFF131417)
private val LAMP_OFF = Color(0xFF44464E)
private val ARROW_OFF = Color(0xFF6A6D74)
private val LAMP_RED = Color(0xFFFF4D4D)
private val LAMP_YELLOW = Color(0xFFFFC23D)
private val LAMP_GREEN = Color(0xFF2FD18A)

// An intersection seen from above; the player's car comes from the bottom.
@Composable
internal fun CrossingScene(crossing: Crossing, modifier: Modifier = Modifier) {
    val subject = RulecadeTheme.colors.accent
    val description = stringResource(R.string.crossing_description)
    BoxWithConstraints(modifier.semantics { contentDescription = description }) {
        val scale = minOf(maxWidth / PLAN_WIDTH, maxHeight / PLAN_HEIGHT)
        val left = (maxWidth - scale * PLAN_WIDTH) / 2f
        val top = (maxHeight - scale * PLAN_HEIGHT) / 2f
        Canvas(Modifier.fillMaxSize()) {
            val plan = Plan(left.toPx(), top.toPx(), scale.toPx())
            drawRoads(plan, crossing.signal != null)
            // The way goes under the cars, so a car in it shows what the player must wait for.
            crossing.cars.filter { it.subject }.forEach { car ->
                car.turn?.let {
                    drawPath(plan, it, waitingFront(car.from, crossing.signal != null))
                }
            }
            crossing.cars.forEach { drawCrossingCar(plan, it, crossing.signal != null, subject) }
            crossing.signal?.let { drawSignal(plan, it) }
        }
        crossing.sign?.let(RoadSign::of)?.let { sign ->
            RoadSignImage(
                sign,
                Modifier
                    .offset(left + scale * SIGN_LEFT, top + scale * SIGN_TOP)
                    .size(scale * SIGN_SIZE)
            )
        }
    }
}

// Plan units to pixels.
private class Plan(val left: Float, val top: Float, val scale: Float) {
    fun x(u: Float) = left + u * scale

    fun y(u: Float) = top + u * scale

    fun at(x: Float, y: Float) = Offset(x(x), y(y))

    fun size(w: Float, h: Float) = Size(w * scale, h * scale)
}

private fun DrawScope.box(plan: Plan, x: Float, y: Float, w: Float, h: Float, color: Color) =
    drawRect(color, plan.at(x, y), plan.size(w, h))

private fun DrawScope.drawRoads(plan: Plan, signalized: Boolean) {
    drawRoundRect(
        GROUND,
        plan.at(0f, 0f),
        plan.size(PLAN_WIDTH, PLAN_HEIGHT),
        CornerRadius(CORNER * plan.scale)
    )
    val roadLeft = CENTER_X - HALF_ROAD
    val roadTop = CENTER_Y - HALF_ROAD
    val road = HALF_ROAD * 2
    box(plan, roadLeft, 0f, road, PLAN_HEIGHT, ASPHALT)
    box(plan, 0f, roadTop, PLAN_WIDTH, road, ASPHALT)
    // Where the center line stops: at the stop line, or at the intersection without a signal.
    val stopAt = if (signalized) CROSSWALK_GAP + CROSSWALK_DEPTH + STOP_LINE_GAP + STOP_LINE else 0f
    val northEnd = roadTop - stopAt
    val southStart = roadTop + road + stopAt
    val westEnd = roadLeft - stopAt
    val eastStart = roadLeft + road + stopAt
    val yellow = StaticColors.signYellow
    for (offset in listOf(-CENTER_GAP - CENTER_LINE, CENTER_GAP)) {
        box(plan, CENTER_X + offset, 0f, CENTER_LINE, northEnd, yellow)
        box(plan, CENTER_X + offset, southStart, CENTER_LINE, PLAN_HEIGHT - southStart, yellow)
        box(plan, 0f, CENTER_Y + offset, westEnd, CENTER_LINE, yellow)
        box(plan, eastStart, CENTER_Y + offset, PLAN_WIDTH - eastStart, CENTER_LINE, yellow)
    }
    if (!signalized) return
    val near = CROSSWALK_GAP
    val far = CROSSWALK_GAP + CROSSWALK_DEPTH
    STRIPES.forEach { s ->
        box(plan, roadLeft + s, roadTop - far, STRIPE, CROSSWALK_DEPTH, LINE)
        box(plan, roadLeft + s, roadTop + road + near, STRIPE, CROSSWALK_DEPTH, LINE)
        box(plan, roadLeft - far, roadTop + s, CROSSWALK_DEPTH, STRIPE, LINE)
        box(plan, roadLeft + road + near, roadTop + s, CROSSWALK_DEPTH, STRIPE, LINE)
    }
    // Each approach's stop line spans its own lane, behind the crosswalk.
    val line = far + STOP_LINE_GAP
    val lane = HALF_ROAD - CENTER_GAP - CENTER_LINE
    box(plan, roadLeft, roadTop - line - STOP_LINE, lane, STOP_LINE, LINE)
    box(plan, CENTER_X + CENTER_GAP + CENTER_LINE, roadTop + road + line, lane, STOP_LINE, LINE)
    box(plan, roadLeft + road + line, roadTop, STOP_LINE, lane, LINE)
    box(
        plan,
        roadLeft - line - STOP_LINE,
        CENTER_Y + CENTER_GAP + CENTER_LINE,
        STOP_LINE,
        lane,
        LINE
    )
}

// How far from the intersection a waiting car's front is.
private fun waitingGap(signalized: Boolean) = CAR_GAP + if (signalized) {
    CROSSWALK_GAP + CROSSWALK_DEPTH + STOP_LINE_GAP + STOP_LINE
} else {
    0f
}

// The front of a car waiting on [arm], in plan units along its way in.
private fun waitingFront(arm: Arm, signalized: Boolean): Float {
    val gap = waitingGap(signalized)
    return when (arm) {
        Arm.South -> CENTER_Y + HALF_ROAD + gap
        Arm.North -> CENTER_Y - HALF_ROAD - gap
        Arm.East -> CENTER_X + HALF_ROAD + gap
        Arm.West -> CENTER_X - HALF_ROAD - gap
    }
}

// Cars keep right: each drives in the lane to the right of the center line.
private fun DrawScope.drawCrossingCar(
    plan: Plan,
    car: CrossingCar,
    signalized: Boolean,
    subject: Color
) {
    val front = waitingFront(car.from, signalized)
    val half = CAR_LENGTH / 2f
    val (center, degrees) = when (car.from) {
        Arm.South -> Offset(
            CENTER_X + LANE_CENTER,
            if (car.at == Spot.Inside) CENTER_Y + INSIDE else front + half
        ) to 0f
        Arm.North -> Offset(
            CENTER_X - LANE_CENTER,
            if (car.at == Spot.Inside) CENTER_Y - INSIDE else front - half
        ) to 180f
        Arm.East -> Offset(
            if (car.at == Spot.Inside) CENTER_X - INSIDE else front + half,
            CENTER_Y - LANE_CENTER
        ) to 270f
        Arm.West -> Offset(
            if (car.at == Spot.Inside) CENTER_X + INSIDE else front - half,
            CENTER_Y + LANE_CENTER
        ) to 90f
    }
    val pivot = plan.at(center.x, center.y)
    rotate(degrees, pivot) {
        drawCar(
            Frame(
                pivot.x - CAR_WIDTH * plan.scale / 2f,
                pivot.y - CAR_LENGTH * plan.scale / 2f,
                CAR_WIDTH * plan.scale,
                CAR_LENGTH * plan.scale
            ),
            if (car.subject) subject else OTHER_CAR
        )
    }
}

// The subject's way through, dotted, from just ahead of the car waiting at the bottom.
private fun DrawScope.drawPath(plan: Plan, turn: Turn, front: Float) {
    val x = CENTER_X + LANE_CENTER
    val start = front - PATH_START
    val path = Path().apply {
        moveTo(plan.x(x), plan.y(start))
        when (turn) {
            Turn.Straight -> lineTo(plan.x(x), plan.y(PATH_END))
            Turn.Right -> {
                val y = CENTER_Y + LANE_CENTER
                lineTo(plan.x(x), plan.y(y + TURN_RADIUS))
                quadraticTo(plan.x(x), plan.y(y), plan.x(x + TURN_RADIUS), plan.y(y))
                lineTo(plan.x(PLAN_WIDTH - PATH_END), plan.y(y))
            }
            Turn.Left -> {
                val y = CENTER_Y - LANE_CENTER
                val turnAt = CENTER_Y + LANE_CENTER - TURN_RADIUS
                lineTo(plan.x(x), plan.y(turnAt))
                quadraticTo(plan.x(x), plan.y(y), plan.x(x - LANE_CENTER * 2), plan.y(y))
                lineTo(plan.x(PATH_END), plan.y(y))
            }
        }
    }
    drawPath(
        path,
        Color.White,
        style = Stroke(
            width = PATH_WIDTH * plan.scale,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(2f * plan.scale, 10f * plan.scale)
            )
        )
    )
}

// The signal head over the far corner, facing the player's car.
private fun DrawScope.drawSignal(plan: Plan, signal: Signal) {
    val lamps = if (signal.lamps == 3) {
        listOf(Lamp.Red, Lamp.Yellow, Lamp.Green)
    } else {
        listOf(Lamp.Red, Lamp.Yellow, Lamp.Arrow, Lamp.Green)
    }
    val width = LAMP_PITCH * lamps.size + (HEAD_HEIGHT - LAMP_PITCH)
    val left = PLAN_WIDTH - HEAD_RIGHT - width
    drawRoundRect(
        HEAD,
        plan.at(left, HEAD_TOP),
        plan.size(width, HEAD_HEIGHT),
        CornerRadius(LAMP_RADIUS * plan.scale)
    )
    lamps.forEachIndexed { i, lamp ->
        val center = plan.at(
            left + HEAD_HEIGHT / 2f + i * LAMP_PITCH,
            HEAD_TOP + HEAD_HEIGHT / 2f
        )
        val on = lamp == signal.lit
        val color = when {
            !on || lamp == Lamp.Arrow -> LAMP_OFF
            lamp == Lamp.Red -> LAMP_RED
            lamp == Lamp.Yellow -> LAMP_YELLOW
            else -> LAMP_GREEN
        }
        drawCircle(color, LAMP_RADIUS * plan.scale, center)
        if (lamp == Lamp.Arrow) drawArrow(plan, center, if (on) LAMP_GREEN else ARROW_OFF)
    }
}

// A left arrow inside the arrow lamp.
private fun DrawScope.drawArrow(plan: Plan, center: Offset, color: Color) {
    val s = plan.scale
    val tip = Offset(center.x - 5.5f * s, center.y)
    val stroke = Stroke(width = 2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(
        Path().apply {
            moveTo(center.x + 6f * s, center.y)
            lineTo(tip.x, tip.y)
            moveTo(tip.x + 4f * s, tip.y - 4f * s)
            lineTo(tip.x, tip.y)
            lineTo(tip.x + 4f * s, tip.y + 4f * s)
        },
        color,
        style = stroke
    )
}
