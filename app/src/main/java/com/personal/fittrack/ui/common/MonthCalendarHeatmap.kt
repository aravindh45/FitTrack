package com.personal.fittrack.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personal.fittrack.domain.model.GoalType
import java.time.LocalDate

private val MIN_BUBBLE = 10.dp
private val MAX_BUBBLE = 34.dp
private val WEEKDAY_HEADERS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/**
 * Google Fit-style "My Activity" calendar: one bubble per day, sized by how much was logged
 * relative to goal and colored with the same green/yellow/red language as the home-screen
 * ring. Unlogged and future days just show a bare day number, no bubble.
 */
@Composable
fun MonthCalendarHeatmap(
    year: Int,
    month: Int,
    caloriesByDay: Map<Int, Int>,
    goal: Int,
    goalType: GoalType,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val firstOfMonth = LocalDate.of(year, month, 1)
    val daysInMonth = firstOfMonth.lengthOfMonth()
    val leadingBlanks = firstOfMonth.dayOfWeek.value - 1 // Monday-first week, matching the rest of the app
    val cells: List<Int?> = List(leadingBlanks) { null } + (1..daysInMonth).toList()
    // Pad the tail to a full multiple of 7 - without this, the last (partial) week's Row only
    // has as many children as remaining days, so each cell's weight(1f) splits the row among
    // e.g. 2 cells instead of 7, pushing them to the wrong columns (this was the "March 30/31
    // drift to the middle" bug - every month with a partial last row had it to some degree).
    val trailingBlanks = (7 - cells.size % 7) % 7
    val rows = (cells + List(trailingBlanks) { null }).chunked(7)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            WEEKDAY_HEADERS.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val date = LocalDate.of(year, month, day)
                            if (!date.isAfter(today)) {
                                DayBubble(calories = caloriesByDay[day] ?: 0, goal = goal, goalType = goalType, day = day)
                            } else {
                                Text(
                                    text = "$day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayBubble(calories: Int, goal: Int, goalType: GoalType, day: Int) {
    val fraction = if (goal > 0) (calories.toFloat() / goal).coerceIn(0f, 1.3f) else 0f
    val diameter: Dp = MIN_BUBBLE + (MAX_BUBBLE - MIN_BUBBLE) * fraction
    val color: Color = if (calories <= 0) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    } else {
        calorieStatusColor(calories, goal, goalType)
    }
    val textColor = if (calories <= 0) MaterialTheme.colorScheme.onSurfaceVariant else Color.White

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(color.copy(alpha = if (calories <= 0) 1f else 0.85f))
        )
        Text(
            text = "$day",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (calories > 0) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
