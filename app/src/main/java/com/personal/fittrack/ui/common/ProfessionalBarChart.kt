package com.personal.fittrack.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.fittrack.domain.model.GoalType
import kotlin.math.ceil

/** One bar's worth of data: a short x-axis label (e.g. "Mon" or "10/13") and its total. */
data class ChartPoint(val label: String, val calories: Int)

/**
 * A proper labeled bar chart - gridlines with kcal values on the Y axis, a date/day label
 * under every bar, rounded bar tops, and a dashed reference line for the daily goal. Replaces
 * the earlier bare-bars version, which had no way to read actual values off the chart.
 */
@Composable
fun ProfessionalBarChart(
    points: List<ChartPoint>,
    goal: Int,
    goalType: GoalType,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisLabelStyle = TextStyle(fontSize = 11.sp, color = axisLabelColor, textAlign = TextAlign.Center)
    val goalLabelStyle = TextStyle(fontSize = 10.sp, color = axisLabelColor, textAlign = TextAlign.End)

    val rawMax = (points.maxOfOrNull { it.calories } ?: 0).coerceAtLeast(goal).coerceAtLeast(1)
    // Round the axis ceiling up to a "nice" multiple of 200 so gridline labels read cleanly.
    val axisMax = (ceil(rawMax / 200.0) * 200).toInt().coerceAtLeast(200)
    val gridLineCount = 4

    Canvas(modifier = modifier) {
        val leftAxisWidth = 52.dp.toPx()
        val bottomAxisHeight = 28.dp.toPx()
        val plotWidth = size.width - leftAxisWidth
        val plotHeight = size.height - bottomAxisHeight

        fun yForValue(value: Int): Float = plotHeight - (value.toFloat() / axisMax) * plotHeight

        // Gridlines + Y-axis kcal labels.
        for (i in 0..gridLineCount) {
            val value = axisMax * i / gridLineCount
            val y = yForValue(value)
            drawLine(
                color = gridColor,
                start = Offset(leftAxisWidth, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            val label = textMeasurer.measure(value.toString(), axisLabelStyle)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(leftAxisWidth - label.size.width - 6.dp.toPx(), y - label.size.height / 2f)
            )
        }

        // Bars + X-axis labels.
        val barSlot = plotWidth / points.size
        val barWidth = barSlot * 0.5f
        points.forEachIndexed { index, point ->
            val barHeight = plotHeight - yForValue(point.calories)
            val left = leftAxisWidth + index * barSlot + (barSlot - barWidth) / 2f
            val top = yForValue(point.calories)
            val color = calorieStatusColor(point.calories, goal, goalType)
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight.coerceAtLeast(2.dp.toPx())),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            val label = textMeasurer.measure(point.label, axisLabelStyle)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(left + barWidth / 2f - label.size.width / 2f, plotHeight + 6.dp.toPx())
            )
        }

        // Dashed goal reference line + label.
        if (goal in 1..axisMax) {
            val goalY = yForValue(goal)
            drawLine(
                color = axisLabelColor,
                start = Offset(leftAxisWidth, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
            val goalLabel = textMeasurer.measure("Goal $goal", goalLabelStyle)
            drawText(
                textLayoutResult = goalLabel,
                topLeft = Offset(size.width - goalLabel.size.width - 4.dp.toPx(), goalY - goalLabel.size.height - 4.dp.toPx())
            )
        }

        // Baseline.
        drawLine(
            color = gridColor,
            start = Offset(leftAxisWidth, plotHeight),
            end = Offset(size.width, plotHeight),
            strokeWidth = 1.dp.toPx()
        )
    }
}
