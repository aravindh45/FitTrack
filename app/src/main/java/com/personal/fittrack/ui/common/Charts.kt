package com.personal.fittrack.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.personal.fittrack.domain.model.GoalType
import com.personal.fittrack.ui.theme.GoogleBlue
import kotlin.math.min

/** Default daily "move goal" used only to decide how full the inner activity ring looks -
 * matches the common Fitbit/Google Fit default. Not user-configurable yet; the ring still
 * reads correctly even past 100% (it just stays full), so this is a reasonable placeholder
 * until/if a custom burn goal becomes its own setting. */
private const val DEFAULT_ACTIVE_CALORIE_GOAL_KCAL = 300

/** A circular "consumed vs goal" ring, in the spirit of Google Fit's activity rings -
 * color shifts from on-track green to warning yellow to over-budget red. Whether going OVER
 * the goal counts as a warning at all depends on [goalType]: exceeding the goal is the whole
 * point for a Gain-weight target, so that case stays green instead of flashing red.
 *
 * A second, inner ring (teal, from Health Connect) shows calories burned through activity -
 * same two-rings-nested-together look as Google Fit/Fitbit's Heart Points + Steps rings, just
 * with "calories eaten" on the outside and "calories burned" on the inside instead. Only drawn
 * when [activeCaloriesBurned] is non-zero, so nothing changes visually for anyone who hasn't
 * connected Health Connect. */
@Composable
fun CalorieProgressRing(
    consumed: Int,
    goal: Int,
    goalType: GoalType,
    activeCaloriesBurned: Int = 0,
    modifier: Modifier = Modifier
) {
    val fraction = if (goal <= 0) 0f else (consumed.toFloat() / goal).coerceIn(0f, 1f)
    val isOver = goal in 1..<consumed
    val ringColor = calorieStatusColor(consumed, goal, goalType)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeFraction = (activeCaloriesBurned.toFloat() / DEFAULT_ACTIVE_CALORIE_GOAL_KCAL).coerceIn(0f, 1f)
    val activeColor = GoogleBlue

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerStroke = 18.dp.toPx()
            val outerDiameter = min(size.width, size.height) - outerStroke
            val outerTopLeft = Offset((size.width - outerDiameter) / 2f, (size.height - outerDiameter) / 2f)
            val outerSize = Size(outerDiameter, outerDiameter)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = outerTopLeft,
                size = outerSize,
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = outerTopLeft,
                size = outerSize,
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )

            if (activeCaloriesBurned > 0) {
                val innerStroke = 14.dp.toPx()
                val gap = 10.dp.toPx()
                val innerDiameter = outerDiameter - outerStroke - innerStroke - gap * 2
                val innerTopLeft = Offset((size.width - innerDiameter) / 2f, (size.height - innerDiameter) / 2f)
                val innerSize = Size(innerDiameter, innerDiameter)

                drawArc(
                    color = trackColor.copy(alpha = 0.5f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = activeColor,
                    startAngle = -90f,
                    sweepAngle = 360f * activeFraction,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                )
            }
        }
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$consumed", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = if (isOver) "over $goal kcal goal" else "of $goal kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (activeCaloriesBurned > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = activeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = " $activeCaloriesBurned kcal burned",
                            style = MaterialTheme.typography.labelSmall,
                            color = activeColor
                        )
                    }
                }
            }
        }
    }
}
