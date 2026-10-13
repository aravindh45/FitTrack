package com.personal.fittrack.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.personal.fittrack.domain.calc.CalorieNeedCalculator
import com.personal.fittrack.domain.model.LlmProviderId
import com.personal.fittrack.domain.model.UserProfile
import com.personal.fittrack.ui.theme.CalorieApproachingLimit
import com.personal.fittrack.ui.theme.CalorieOnTrack
import com.personal.fittrack.ui.theme.GoogleBlue
import com.personal.fittrack.ui.theme.GoogleGreen
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onEditProfileClick: () -> Unit,
    onConnectHealthClick: () -> Unit,
    healthPermissionRefreshTrigger: Int = 0
) {
    val selectedProvider by viewModel.selectedProvider.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val healthConnectGranted by viewModel.healthConnectGranted.collectAsState()
    var keyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadKeyForCurrentProvider() }
    LaunchedEffect(healthPermissionRefreshTrigger) { viewModel.refreshHealthConnectStatus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        profile?.let { EditProfileRow(it, onEditProfileClick) }

        ConnectHealthRow(
            isAvailable = viewModel.healthConnectManager.isAvailable(),
            isGranted = healthConnectGranted,
            onClick = onConnectHealthClick
        )

        profile?.let { CalorieGoalSection(it, viewModel) }

        Text("AI Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "Pick which LLM analyzes your meals. Your API key stays encrypted on this device only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LlmProviderId.entries.forEach { provider ->
                FilterChip(
                    selected = selectedProvider == provider,
                    onClick = {
                        viewModel.selectProvider(provider)
                        keyVisible = false
                    },
                    label = { Text(provider.label, style = MaterialTheme.typography.labelMedium) },
                    trailingIcon = if (viewModel.hasKeyFor(provider)) {
                        { Icon(Icons.Filled.CheckCircle, contentDescription = "Key saved", tint = GoogleGreen, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${selectedProvider.label} API key",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = viewModel.apiKeyInput,
                    onValueChange = { viewModel.apiKeyInput = it },
                    label = { Text("API key") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                imageVector = if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle key visibility",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = viewModel::saveApiKey, modifier = Modifier.fillMaxWidth()) {
                    Text("Save key", style = MaterialTheme.typography.labelLarge)
                }
                viewModel.saveConfirmation?.let { confirmation ->
                    Text(confirmation, color = GoogleGreen, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Text(
            text = "Only one LLM call is made per logged meal - used to estimate nutrition " +
                "facts and write your motivational message. Nothing else in the app talks to an LLM.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Standard Material You "navigate to a sub-page to edit things" list row - tapping opens
 * the same form used during onboarding, pre-filled with the saved profile. */
@Composable
private fun EditProfileRow(profile: UserProfile, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick) {
        ListItem(
            headlineContent = { Text("Edit Profile", fontWeight = FontWeight.SemiBold) },
            supportingContent = {
                Text("${profile.weightKg.toInt()} kg \u2022 ${profile.heightCm.toInt()} cm \u2022 ${profile.goalType.label}")
            },
            leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        )
    }
}

/** Grants Health Connect read access to active-calories-burned data - this is what powers
 * the inner "burned" ring on the Dashboard and the activity-aware deficit math in History.
 * Entirely optional: everything works fine without it, this row just lights up green once
 * connected. */
@Composable
private fun ConnectHealthRow(isAvailable: Boolean, isGranted: Boolean, onClick: () -> Unit) {
    val subtitle = when {
        !isAvailable -> "Health Connect app not installed"
        isGranted -> "Connected - activity burn included in your deficit"
        else -> "Tap to include calories burned through activity"
    }
    ElevatedCard(onClick = onClick, enabled = isAvailable) {
        ListItem(
            headlineContent = { Text("Connect Health Data", fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(subtitle) },
            leadingContent = {
                Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = if (isGranted) GoogleGreen else GoogleBlue)
            },
            trailingContent = {
                if (isGranted) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Connected", tint = GoogleGreen)
                } else {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        )
    }
}

private const val CALORIE_MAGNITUDE_MAX_KCAL = 1000f
private const val CALORIE_STEP_KCAL = 25

private enum class GoalDirection(val label: String) {
    DEFICIT("Lose"),
    MAINTAIN("Maintain"),
    SURPLUS("Gain")
}

@Composable
private fun CalorieGoalSection(profile: UserProfile, viewModel: SettingsViewModel) {
    val maintenance = remember(profile.id, profile.weightKg, profile.heightCm, profile.age, profile.gender, profile.activityLevel) {
        CalorieNeedCalculator.tdee(profile).roundToInt()
    }

    // Keyed on the whole profile (not just id, which never changes in this single-user app)
    // so editing the goal direction on the Edit Profile screen correctly resets this slider
    // instead of showing stale state from before the edit.
    var direction by remember(profile) {
        mutableStateOf(
            when {
                profile.calorieAdjustmentKcal < 0 -> GoalDirection.DEFICIT
                profile.calorieAdjustmentKcal > 0 -> GoalDirection.SURPLUS
                else -> GoalDirection.MAINTAIN
            }
        )
    }
    var magnitude by remember(profile) { mutableFloatStateOf(abs(profile.calorieAdjustmentKcal).toFloat()) }
    var magnitudeText by remember(profile) { mutableStateOf(abs(profile.calorieAdjustmentKcal).toString()) }

    val signedAdjustment = when (direction) {
        GoalDirection.DEFICIT -> -magnitude.roundToInt()
        GoalDirection.SURPLUS -> magnitude.roundToInt()
        GoalDirection.MAINTAIN -> 0
    }
    val dailyGoal = maintenance + signedAdjustment
    val isDirty = signedAdjustment != profile.calorieAdjustmentKcal
    val accentColor = when (direction) {
        GoalDirection.DEFICIT -> CalorieOnTrack
        GoalDirection.SURPLUS -> CalorieApproachingLimit
        GoalDirection.MAINTAIN -> MaterialTheme.colorScheme.primary
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                Text("Daily Calorie Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "$dailyGoal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "kcal / day",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Maintenance $maintenance kcal \u2022 ${signPhrase(signedAdjustment)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                GoalDirection.entries.forEach { option ->
                    FilterChip(
                        selected = direction == option,
                        onClick = { direction = option },
                        label = { Text(option.label, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (direction != GoalDirection.MAINTAIN) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Slider(
                        value = magnitude,
                        onValueChange = {
                            magnitude = it
                            magnitudeText = it.roundToInt().toString()
                        },
                        valueRange = 0f..CALORIE_MAGNITUDE_MAX_KCAL,
                        steps = (CALORIE_MAGNITUDE_MAX_KCAL / CALORIE_STEP_KCAL).toInt() - 1,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = magnitudeText,
                        onValueChange = { typed ->
                            val digitsOnly = typed.filter { it.isDigit() }.take(4)
                            magnitudeText = digitsOnly
                            magnitude = (digitsOnly.toIntOrNull() ?: 0).coerceIn(0, CALORIE_MAGNITUDE_MAX_KCAL.toInt()).toFloat()
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(68.dp)
                    )
                }
                Text(
                    text = "0-${CALORIE_MAGNITUDE_MAX_KCAL.toInt()} kcal/day \u2022 drag or type",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { viewModel.saveCalorieAdjustment(signedAdjustment) },
                enabled = isDirty,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Goal", style = MaterialTheme.typography.labelLarge)
            }
            viewModel.calorieGoalSaveConfirmation?.let { confirmation ->
                Text(confirmation, color = GoogleGreen, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun signPhrase(adjustmentKcal: Int): String = when {
    adjustmentKcal < 0 -> "minus ${-adjustmentKcal} kcal deficit"
    adjustmentKcal > 0 -> "plus $adjustmentKcal kcal surplus"
    else -> "no deficit or surplus"
}
