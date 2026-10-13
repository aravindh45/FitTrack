package com.personal.fittrack.ui.common

import androidx.compose.ui.graphics.Color
import com.personal.fittrack.domain.model.GoalType
import com.personal.fittrack.domain.model.treatsExcessAsBad
import com.personal.fittrack.ui.theme.CalorieApproachingLimit
import com.personal.fittrack.ui.theme.CalorieOnTrack
import com.personal.fittrack.ui.theme.CalorieOverBudget

private const val APPROACHING_GOAL_THRESHOLD = 0.85f

/**
 * The ONE place that decides what color a day's calories should render as - used by the
 * dashboard ring, the week bar chart, and the month calendar heatmap. These three used to
 * each carry their own copy of this logic and drifted out of sync (the week chart only had
 * a binary green/red check with no "approaching" tier, so the same day that showed yellow
 * on the month view showed green on the week view). Now there's exactly one source of truth
 * for all three.
 */
fun calorieStatusColor(consumed: Int, goal: Int, goalType: GoalType): Color {
    if (goal <= 0) return CalorieOnTrack
    val isOver = consumed > goal
    val fractionOfGoal = consumed.toFloat() / goal
    return when {
        isOver && goalType.treatsExcessAsBad() -> CalorieOverBudget
        isOver -> CalorieOnTrack // exceeding is the point for a Gain-weight goal, not a warning
        fractionOfGoal >= APPROACHING_GOAL_THRESHOLD -> CalorieApproachingLimit
        else -> CalorieOnTrack
    }
}
