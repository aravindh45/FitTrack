package com.personal.fittrack.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.fittrack.ui.common.CalorieProgressRing
import com.personal.fittrack.ui.common.MealLogCard
import com.personal.fittrack.ui.common.StatCard
import com.personal.fittrack.ui.theme.GoogleGreen
import com.personal.fittrack.ui.theme.GoogleYellow

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsState()
    val activeCaloriesBurned by viewModel.activeCaloriesBurnedToday.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshActiveCaloriesBurned() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Today's progress",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        val profile = state.profile
        if (profile == null) {
            Text(
                text = "Finish onboarding to see your personalized targets.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        CalorieProgressRing(
            consumed = state.caloriesConsumedToday,
            goal = state.dailyCalorieGoal,
            goalType = profile.goalType,
            activeCaloriesBurned = activeCaloriesBurned,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
        )

        state.today?.entries?.firstOrNull()?.motivationMessage?.takeIf { it.isNotBlank() }?.let { message ->
            MotivationBanner(message)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                title = "BMI",
                value = "%.1f".format(profile.bmi),
                accentColor = GoogleGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Category",
                value = profile.bmiCategory,
                accentColor = GoogleYellow,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Remaining",
                value = "${state.caloriesRemainingToday} kcal",
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Logged today",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        val todayEntries = state.today?.entries.orEmpty()
        if (todayEntries.isEmpty()) {
            Text("Nothing logged yet today - tap the meal icon to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                todayEntries.forEach { entry ->
                    MealLogCard(
                        entry = entry,
                        onRelog = { viewModel.relogEntry(entry) },
                        onDelete = { viewModel.deleteEntry(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MotivationBanner(message: String) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
