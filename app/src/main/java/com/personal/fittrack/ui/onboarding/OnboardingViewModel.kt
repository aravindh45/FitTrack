package com.personal.fittrack.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.repository.ProfileRepository
import com.personal.fittrack.domain.model.ActivityLevel
import com.personal.fittrack.domain.model.Gender
import com.personal.fittrack.domain.model.GoalType
import com.personal.fittrack.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

private const val SUGGESTED_GOAL_WEIGHT_DELTA_KG = 5.0

/**
 * Backs both first-time onboarding AND the "Edit Profile" screen reachable from Settings -
 * same form, same validation, same save path. If a profile already exists it's loaded into
 * the fields on init (onboarding never has one yet, so this is a no-op there); if none
 * exists the fields just start blank. One ViewModel, one Screen, two entry points.
 */
class OnboardingViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var weightText by mutableStateOf("")
        private set
    var heightText by mutableStateOf("")
    var ageText by mutableStateOf("")
    var gender by mutableStateOf(Gender.MALE)
    var activityLevel by mutableStateOf(ActivityLevel.MODERATE)
    var goalType by mutableStateOf(GoalType.LOSE_WEIGHT)
        private set
    var goalWeightText by mutableStateOf("")
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Remembers what was actually loaded from disk, so a save that doesn't touch the goal
    // direction keeps the user's own Settings-tuned calorie adjustment instead of silently
    // resetting it back to the goal's generic default.
    private var existingProfileId: Long? = null
    private var loadedGoalType: GoalType? = null
    private var loadedCalorieAdjustment: Int? = null

    // Goal weight auto-suggests from the goal direction (current weight minus/plus a delta)
    // until the user types their own value directly - then we stop overwriting it.
    private var goalWeightManuallyEdited = false

    private val _profileSaved = MutableSharedFlow<Unit>(replay = 0)
    val profileSaved: SharedFlow<Unit> = _profileSaved

    init {
        viewModelScope.launch {
            profileRepository.getProfileOnce()?.let { profile ->
                existingProfileId = profile.id
                loadedGoalType = profile.goalType
                loadedCalorieAdjustment = profile.calorieAdjustmentKcal
                weightText = profile.weightKg.toString()
                heightText = profile.heightCm.toString()
                ageText = profile.age.toString()
                gender = profile.gender
                activityLevel = profile.activityLevel
                goalType = profile.goalType
                goalWeightText = profile.goalWeightKg?.toString().orEmpty()
                goalWeightManuallyEdited = profile.goalWeightKg != null
            }
        }
    }

    fun updateWeightText(text: String) {
        weightText = text
        suggestGoalWeightIfNeeded()
    }

    fun updateGoalType(newGoalType: GoalType) {
        goalType = newGoalType
        suggestGoalWeightIfNeeded()
    }

    fun updateGoalWeightText(text: String) {
        goalWeightText = text
        goalWeightManuallyEdited = true
    }

    /** Lose -> current weight minus a sane delta, Gain -> plus, Maintain -> same as current.
     * Only runs while the user hasn't typed their own goal weight - a real target (e.g. a
     * doctor-given number) should never be silently overwritten by this guess. */
    private fun suggestGoalWeightIfNeeded() {
        if (goalWeightManuallyEdited) return
        val weight = weightText.toDoubleOrNull() ?: return
        val suggested = when (goalType) {
            GoalType.LOSE_WEIGHT -> weight - SUGGESTED_GOAL_WEIGHT_DELTA_KG
            GoalType.GAIN_WEIGHT -> weight + SUGGESTED_GOAL_WEIGHT_DELTA_KG
            GoalType.MAINTAIN_WEIGHT -> weight
        }.coerceAtLeast(1.0)
        goalWeightText = "%.1f".format(suggested)
    }

    fun submit() {
        val weight = weightText.toDoubleOrNull()
        val height = heightText.toDoubleOrNull()
        val age = ageText.toIntOrNull()
        val goalWeight = goalWeightText.toDoubleOrNull()

        if (weight == null || weight <= 0 || weight > 400) {
            errorMessage = "Enter a valid weight in kg."
            return
        }
        if (height == null || height <= 0 || height > 260) {
            errorMessage = "Enter a valid height in cm."
            return
        }
        if (age == null || age <= 0 || age > 120) {
            errorMessage = "Enter a valid age."
            return
        }
        errorMessage = null

        val calorieAdjustment = if (goalType == loadedGoalType) {
            loadedCalorieAdjustment ?: goalType.defaultCalorieAdjustmentKcal
        } else {
            goalType.defaultCalorieAdjustmentKcal
        }

        val profile = UserProfile(
            id = existingProfileId ?: 1L,
            weightKg = weight,
            heightCm = height,
            age = age,
            gender = gender,
            activityLevel = activityLevel,
            goalType = goalType,
            goalWeightKg = goalWeight,
            calorieAdjustmentKcal = calorieAdjustment
        )

        viewModelScope.launch {
            profileRepository.saveProfile(profile)
            _profileSaved.emit(Unit)
        }
    }
}
