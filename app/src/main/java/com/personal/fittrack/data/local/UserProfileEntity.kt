package com.personal.fittrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.personal.fittrack.domain.model.ActivityLevel
import com.personal.fittrack.domain.model.Gender
import com.personal.fittrack.domain.model.GoalType
import com.personal.fittrack.domain.model.UserProfile

/** Single-row table (id is always 1) holding the user's current profile. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val gender: String,
    val activityLevel: String,
    val goalType: String,
    val goalWeightKg: Double?,
    val calorieAdjustmentKcal: Int
)

fun UserProfileEntity.toDomain() = UserProfile(
    id = id,
    weightKg = weightKg,
    heightCm = heightCm,
    age = age,
    gender = Gender.valueOf(gender),
    activityLevel = ActivityLevel.valueOf(activityLevel),
    goalType = GoalType.valueOf(goalType),
    goalWeightKg = goalWeightKg,
    calorieAdjustmentKcal = calorieAdjustmentKcal
)

fun UserProfile.toEntity() = UserProfileEntity(
    id = id,
    weightKg = weightKg,
    heightCm = heightCm,
    age = age,
    gender = gender.name,
    activityLevel = activityLevel.name,
    goalType = goalType.name,
    goalWeightKg = goalWeightKg,
    calorieAdjustmentKcal = calorieAdjustmentKcal
)
