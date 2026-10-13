package com.personal.fittrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.personal.fittrack.domain.model.FoodLogEntry

@Entity(tableName = "food_log")
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val rawDescription: String,
    val foodName: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val sugarG: Double,
    val motivationMessage: String,
    val providerUsed: String,
    val epochMillis: Long,
    /** Local calendar day (epoch day) the entry belongs to - precomputed at insert time
     * so daily/weekly/monthly grouping is a cheap indexed query instead of per-row
     * timezone math at read time. */
    val epochDay: Long
)

fun FoodLogEntity.toDomain() = FoodLogEntry(
    id = id,
    rawDescription = rawDescription,
    foodName = foodName,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    motivationMessage = motivationMessage,
    providerUsed = providerUsed,
    epochMillis = epochMillis
)

fun FoodLogEntry.toEntity(epochDay: Long) = FoodLogEntity(
    id = id,
    rawDescription = rawDescription,
    foodName = foodName,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    motivationMessage = motivationMessage,
    providerUsed = providerUsed,
    epochMillis = epochMillis,
    epochDay = epochDay
)
