package io.github.rulecade.ui.quiz

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.rulecade.R
import io.github.rulecade.quiz.Scene
import io.github.rulecade.quiz.SceneVehicle
import io.github.rulecade.quiz.VehicleKind
import io.github.rulecade.ui.theme.RulecadeTheme
import kotlin.math.floor

// The road is a grid of square cells, like the pixel road on the license card; vehicles are
// drawn in finer detail on top, sized in cells.
private const val MEDIAN_CELLS = 2
private const val LANE_CELLS = 8
private const val SHOULDER_CELLS = 3
private const val DASH_CELLS = 3
private const val CAR_WIDTH = 4
private const val TRUCK_WIDTH = 5

private val LABEL_HEIGHT = 44.dp

// The road looks the same in day and night, like a photo of it would.
private val ASPHALT = Color(0xFF33373E)
private val SHOULDER = Color(0xFF3E434A)
private val MEDIAN = Color(0xFF8A9099)
private val LINE = Color(0xFFE8EAED)
private val GLASS = Color(0xFF1B1E23)
internal val OTHER_CAR = Color(0xFFB4BAC2)
private val OTHER_TRUCK = Color(0xFF8F969F)
private val TIRE = Color(0xFF16181C)
private val HEADLIGHT = Color(0xFFFFF1B8)
private val TAILLIGHT = Color(0xFFE5484D)
private const val CARGO_SHADE = 0.82f
private const val ROOF_SHADE = 0.88f
private const val RIB_SHADE = 0.8f

// A one-way highway seen from above, traffic moving up, lane 1 next to the median on the left.
@Composable
internal fun RoadScene(scene: Scene, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val colors = RulecadeTheme.colors
    val labelStyle = RulecadeTheme.typography.label.copy(color = colors.textTertiary)
    val subject = colors.accent
    val labels = (1..scene.lanes).map { stringResource(R.string.scene_lane, it) }
    val description = stringResource(R.string.scene_description, scene.lanes)
    Canvas(modifier.semantics { contentDescription = description }) {
        val columns = MEDIAN_CELLS + LANE_CELLS * scene.lanes + SHOULDER_CELLS
        val top = LABEL_HEIGHT.toPx()
        val cell = floor(minOf(size.width / columns, (size.height - top) / Scene.MIN_ROWS))
        val left = (size.width - cell * columns) / 2f
        val rows = floor((size.height - top) / cell).toInt()
        val grid = Grid(left, top, cell)
        drawLabels(measurer, labels, labelStyle, grid)
        drawRoad(scene.lanes, rows, grid)
        scene.vehicles.forEach { drawVehicle(it, rows, grid, subject) }
    }
}

private class Grid(val left: Float, val top: Float, val cell: Float) {
    fun x(column: Int) = left + column * cell

    fun y(row: Int) = top + row * cell

    fun laneStart(lane: Int) = MEDIAN_CELLS + LANE_CELLS * (lane - 1)
}

private fun DrawScope.cells(
    grid: Grid,
    column: Int,
    row: Int,
    width: Int,
    height: Int,
    color: Color
) = drawRect(
    color,
    Offset(grid.x(column), grid.y(row)),
    Size(width * grid.cell, height * grid.cell)
)

private fun DrawScope.drawLabels(
    measurer: TextMeasurer,
    labels: List<String>,
    style: TextStyle,
    grid: Grid
) {
    labels.forEachIndexed { i, label ->
        val layout = measurer.measure(label, style)
        val center = grid.x(grid.laneStart(i + 1)) + LANE_CELLS * grid.cell / 2f
        drawText(
            layout,
            topLeft = Offset(
                center - layout.size.width / 2f,
                (grid.top - layout.size.height) / 2f
            )
        )
    }
}

private fun DrawScope.drawRoad(lanes: Int, rows: Int, grid: Grid) {
    val roadColumns = LANE_CELLS * lanes
    cells(grid, 0, 0, MEDIAN_CELLS, rows, MEDIAN)
    cells(grid, MEDIAN_CELLS, 0, roadColumns, rows, ASPHALT)
    cells(grid, MEDIAN_CELLS + roadColumns, 0, SHOULDER_CELLS, rows, SHOULDER)
    // Solid edge line before the shoulder, dashed lines between lanes.
    cells(grid, MEDIAN_CELLS + roadColumns, 0, 1, rows, LINE)
    for (lane in 2..lanes) {
        val column = grid.laneStart(lane)
        var row = 0
        while (row < rows) {
            cells(grid, column, row, 1, minOf(DASH_CELLS, rows - row), LINE)
            row += DASH_CELLS * 2
        }
    }
}

private fun DrawScope.drawVehicle(vehicle: SceneVehicle, rows: Int, grid: Grid, subject: Color) {
    val truck = vehicle.kind == VehicleKind.Truck
    val width = if (truck) TRUCK_WIDTH else CAR_WIDTH
    val body = when {
        vehicle.subject -> subject
        truck -> OTHER_TRUCK
        else -> OTHER_CAR
    }
    val column = grid.laneStart(vehicle.lane) + (LANE_CELLS - width) / 2
    val frame = Frame(
        grid.x(column),
        grid.y(vehicle.row(rows)),
        width * grid.cell,
        vehicle.length * grid.cell
    )
    if (truck) drawTruck(frame, body) else drawCar(frame, body)
}

// A vehicle's box on screen; parts are placed in fractions of its width and length, front at
// the top.
internal class Frame(val left: Float, val top: Float, val width: Float, val length: Float) {
    fun x(f: Float) = left + width * f

    fun y(f: Float) = top + length * f
}

private fun DrawScope.part(
    frame: Frame,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    color: Color,
    round: Float = 0f
) = drawRoundRect(
    color,
    Offset(frame.x(x0), frame.y(y0)),
    Size(frame.width * (x1 - x0), frame.length * (y1 - y0)),
    CornerRadius(frame.width * round)
)

// A window narrower at one end: the top edge spans topX, the bottom edge spans bottomX.
private fun DrawScope.glass(
    frame: Frame,
    y0: Float,
    y1: Float,
    topX: ClosedFloatingPointRange<Float>,
    bottomX: ClosedFloatingPointRange<Float>
) = drawPath(
    Path().apply {
        moveTo(frame.x(topX.start), frame.y(y0))
        lineTo(frame.x(topX.endInclusive), frame.y(y0))
        lineTo(frame.x(bottomX.endInclusive), frame.y(y1))
        lineTo(frame.x(bottomX.start), frame.y(y1))
        close()
    },
    GLASS
)

private fun Color.shade(f: Float) = copy(red = red * f, green = green * f, blue = blue * f)

// Seen from above: wheels peeking out, hood with headlights, windshield, roof, rear window,
// tail lights.
internal fun DrawScope.drawCar(frame: Frame, body: Color) {
    for (y in listOf(0.14f, 0.70f)) {
        part(frame, -0.04f, y, 0.14f, y + 0.16f, TIRE, 0.04f)
        part(frame, 0.86f, y, 1.04f, y + 0.16f, TIRE, 0.04f)
    }
    part(frame, -0.07f, 0.27f, 0.10f, 0.32f, body, 0.03f)
    part(frame, 0.90f, 0.27f, 1.07f, 0.32f, body, 0.03f)
    part(frame, 0.06f, 0f, 0.94f, 1f, body, 0.3f)
    part(frame, 0.14f, 0.015f, 0.32f, 0.06f, HEADLIGHT, 0.04f)
    part(frame, 0.68f, 0.015f, 0.86f, 0.06f, HEADLIGHT, 0.04f)
    glass(frame, 0.22f, 0.36f, 0.24f..0.76f, 0.16f..0.84f)
    part(frame, 0.18f, 0.36f, 0.82f, 0.66f, body.shade(ROOF_SHADE), 0.08f)
    glass(frame, 0.66f, 0.78f, 0.16f..0.84f, 0.24f..0.76f)
    part(frame, 0.12f, 0.94f, 0.30f, 0.985f, TAILLIGHT, 0.03f)
    part(frame, 0.70f, 0.94f, 0.88f, 0.985f, TAILLIGHT, 0.03f)
}

// A cab with its windshield and mirrors, then a ribbed cargo box on two rear axles.
private fun DrawScope.drawTruck(frame: Frame, body: Color) {
    for (y in listOf(0.12f, 0.66f, 0.80f)) {
        part(frame, -0.03f, y, 0.10f, y + 0.10f, TIRE, 0.03f)
        part(frame, 0.90f, y, 1.03f, y + 0.10f, TIRE, 0.03f)
    }
    part(frame, -0.08f, 0.10f, 0.08f, 0.14f, body, 0.02f)
    part(frame, 0.92f, 0.10f, 1.08f, 0.14f, body, 0.02f)
    part(frame, 0.05f, 0f, 0.95f, 0.30f, body, 0.18f)
    part(frame, 0.10f, 0.01f, 0.28f, 0.04f, HEADLIGHT, 0.03f)
    part(frame, 0.72f, 0.01f, 0.90f, 0.04f, HEADLIGHT, 0.03f)
    glass(frame, 0.07f, 0.16f, 0.18f..0.82f, 0.12f..0.88f)
    part(frame, 0.12f, 0.16f, 0.88f, 0.28f, body.shade(ROOF_SHADE), 0.06f)
    val cargo = body.shade(CARGO_SHADE)
    part(frame, 0.02f, 0.31f, 0.98f, 1f, cargo, 0.04f)
    var rib = 0.40f
    while (rib < 0.96f) {
        part(frame, 0.06f, rib, 0.94f, rib + 0.008f, cargo.shade(RIB_SHADE))
        rib += 0.1f
    }
    part(frame, 0.06f, 0.975f, 0.22f, 0.995f, TAILLIGHT, 0.02f)
    part(frame, 0.78f, 0.975f, 0.94f, 0.995f, TAILLIGHT, 0.02f)
}
