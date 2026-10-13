package com.personal.fittrack.domain.model

/**
 * The user's profile. There is exactly one row of this in the local database for now
 * (single-user, personal app) but it's modeled as a normal entity so multi-profile support
 * is a additive change later, not a rewrite.
 *
 * calorieAdjustmentKcal is the user-tunable deviation from maintenance (TDEE): negative
 * for a weight-loss deficit, positive for a weight-gain surplus, zero to just maintain.
 * Set via a slider in Settings; GoalType.defaultCalorieAdjustmentKcal supplies a sensible
 * starting value when a profile is first created.
 */
data class UserProfile(
    val id: Long = 1L,
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val gender: Gender,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val goalWeightKg: Double?,
    val calorieAdjustmentKcal: Int = goalType.defaultCalorieAdjustmentKcal
) {
    val bmi: Double
        get() {
            val heightM = heightCm / 100.0
            return weightKg / (heightM * heightM)
        }

    val bmiCategory: String
        get() = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
}
