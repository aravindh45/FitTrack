package com.personal.fittrack.domain.model

/** One LLM-analyzed meal entry, logged with a timestamp. */
data class FoodLogEntry(
    val id: Long = 0L,
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
    val epochMillis: Long
)

/** Aggregated totals for a single calendar day, used by the dashboard & history screens. */
data class DailySummary(
    val dateEpochDay: Long,
    val totalCalories: Int,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val totalFiberG: Double,
    val entries: List<FoodLogEntry>
)
