package com.personal.fittrack.domain.model

/** Biological sex, used for BMR/TDEE formulas. Kept as its own type so it's easy to
 * extend later (e.g. "prefer not to say" using an average formula) without touching callers. */
enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female")
}

/** Standard activity multipliers used in the Mifflin-St Jeor TDEE formula. */
enum class ActivityLevel(val label: String, val multiplier: Double) {
    SEDENTARY("Sedentary (little/no exercise)", 1.2),
    LIGHT("Lightly active (1-3 days/week)", 1.375),
    MODERATE("Moderately active (3-5 days/week)", 1.55),
    ACTIVE("Very active (6-7 days/week)", 1.725),
    ATHLETE("Extra active (hard exercise + physical job)", 1.9)
}

/** The starting point for each goal's calorie slider in Settings - users can drag away
 * from these afterward, this is just a sane default when a profile is first created. */
enum class GoalType(val label: String, val defaultCalorieAdjustmentKcal: Int) {
    LOSE_WEIGHT("Lose weight", -500),
    MAINTAIN_WEIGHT("Maintain weight", 0),
    GAIN_WEIGHT("Gain weight", 300)
}

fun GoalType.treatsExcessAsBad(): Boolean = this != GoalType.GAIN_WEIGHT
