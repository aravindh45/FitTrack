package com.personal.fittrack.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.fittrack.domain.model.GoalType
import com.personal.fittrack.domain.model.treatsExcessAsBad
import com.personal.fittrack.ui.theme.CalorieOnTrack
import com.personal.fittrack.ui.theme.CalorieOverBudget
import kotlin.math.abs

/**
 * Net deficit/surplus summary for a period (Week, Month, or All Time). Reused across all
 * three History chart tabs. Whether a net deficit or a net surplus is the "good" outcome
 * depends on [goalType] - a Gain-weight user hitting a surplus should see green and
 * "Surplus" framed as success, not red like a Lose-weight user blowing their budget would.
 *
 * [activeCaloriesBurned] folds in exercise burn from Health Connect: goal - consumed + burn.
 * E.g. goal 1700, ate 1500, burned 300 through activity -> (1700-1500)+300 = 500 kcal deficit,
 * bigger than food tracking alone would show. Defaults to 0 (pure food-only math) when Health
 * Connect isn't connected, so this is fully backward compatible.
 */
@Composable
fun PeriodSummaryCard(
    periodLabel: String,
    goalType: GoalType,
    daysTracked: Int,
    totalConsumed: Int,
    totalGoal: Int,
    averageDailyCalories: Int,
    activeCaloriesBurned: Int = 0,
    modifier: Modifier = Modifier
) {
    val netDeficit = (totalGoal - totalConsumed) + activeCaloriesBurned
    val isDeficit = netDeficit >= 0
    val isOnTrack = if (isDeficit) true else !goalType.treatsExcessAsBad()
    val accentColor = if (isOnTrack) CalorieOnTrack else CalorieOverBudget

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = accentColor.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$periodLabel ${if (isDeficit) "Deficit" else "Surplus"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${abs(netDeficit)} kcal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            if (daysTracked > 0) {
                Text(
                    text = periodCaption(goalType, isDeficit, netDeficit),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (activeCaloriesBurned > 0) {
                Text(
                    text = "Includes $activeCaloriesBurned kcal burned through activity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn("Days tracked", "$daysTracked")
                StatColumn("Avg/day", "$averageDailyCalories kcal")
                StatColumn("Total logged", "$totalConsumed kcal")
                if (activeCaloriesBurned > 0) {
                    StatColumn("Active burn", "$activeCaloriesBurned kcal")
                }
            }
        }
    }
}

private fun periodCaption(goalType: GoalType, isDeficit: Boolean, netDeficit: Int): String = when {
    isDeficit && goalType == GoalType.GAIN_WEIGHT ->
        "You're under your gain target - try eating a bit more"
    isDeficit ->
        "\u2248 ${"%.2f".format(abs(netDeficit) / 7700.0)} kg of fat"
    goalType == GoalType.GAIN_WEIGHT ->
        "Nice - extra fuel toward your gain goal"
    else ->
        "Over your goal for this period"
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
