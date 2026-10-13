package com.personal.fittrack.domain.calc

import com.personal.fittrack.domain.model.Gender
import com.personal.fittrack.domain.model.UserProfile
import kotlin.math.roundToInt

/**
 * Mifflin-St Jeor BMR + activity multiplier = TDEE (maintenance calories). The daily goal
 * is TDEE plus the user's own slider-chosen adjustment (negative = deficit, positive =
 * surplus) - see UserProfile.calorieAdjustmentKcal and the Settings screen's slider.
 */
object CalorieNeedCalculator {

    private const val MIN_SAFE_DAILY_CALORIES = 1200

    fun bmr(profile: UserProfile): Double {
        val base = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age
        return when (profile.gender) {
            Gender.MALE -> base + 5
            Gender.FEMALE -> base - 161
        }
    }

    fun tdee(profile: UserProfile): Double = bmr(profile) * profile.activityLevel.multiplier

    /** The calorie target for today: maintenance plus the user's chosen adjustment. */
    fun dailyCalorieGoal(profile: UserProfile): Int {
        val target = tdee(profile) + profile.calorieAdjustmentKcal
        return target.roundToInt().coerceAtLeast(MIN_SAFE_DAILY_CALORIES)
    }
}
