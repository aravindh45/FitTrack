package com.personal.fittrack.data.repository

import com.personal.fittrack.data.local.FoodLogDao
import com.personal.fittrack.data.local.FoodLogEntity
import com.personal.fittrack.data.local.toDomain
import com.personal.fittrack.data.local.toEntity
import com.personal.fittrack.domain.calc.DateUtils
import com.personal.fittrack.domain.model.DailySummary
import com.personal.fittrack.domain.model.FoodLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FoodLogRepository(private val dao: FoodLogDao) {

    suspend fun addEntry(entry: FoodLogEntry): Long {
        val epochDay = DateUtils.epochDayFor(entry.epochMillis)
        return dao.insert(entry.toEntity(epochDay))
    }

    suspend fun deleteEntry(id: Long) = dao.delete(id)

    /** Logs the same food again right now - no new LLM call needed, we already know its
     * nutrition. Used for "I had this again" (e.g. the same tea at breakfast and dinner). */
    suspend fun relogEntry(entry: FoodLogEntry): Long =
        addEntry(entry.copy(id = 0L, epochMillis = System.currentTimeMillis()))

    fun observeDailySummary(epochDay: Long): Flow<DailySummary> =
        dao.observeForDay(epochDay).map { it.toDailySummary(epochDay) }

    fun observeRangeSummaries(startEpochDay: Long, endEpochDay: Long): Flow<List<DailySummary>> =
        dao.observeForRange(startEpochDay, endEpochDay).map { entities ->
            entities.groupBy { DateUtils.epochDayFor(it.epochMillis) }
                .map { (day, dayEntities) -> dayEntities.toDailySummary(day) }
                .sortedByDescending { it.dateEpochDay }
        }

    fun observeAll(): Flow<List<FoodLogEntry>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Calories already logged "today" (local calendar day), used to build LLM context
     * and the dashboard progress ring. Suspends once rather than observing, since callers
     * that need this mid-flow (building a new log request) just need the current value. */
    suspend fun caloriesConsumedOn(epochDay: Long): Int =
        dao.observeForDay(epochDay).first().sumOf { it.calories }

    private fun List<FoodLogEntity>.toDailySummary(epochDay: Long): DailySummary = DailySummary(
        dateEpochDay = epochDay,
        totalCalories = sumOf { it.calories },
        totalProteinG = sumOf { it.proteinG },
        totalCarbsG = sumOf { it.carbsG },
        totalFatG = sumOf { it.fatG },
        totalFiberG = sumOf { it.fiberG },
        entries = map { it.toDomain() }
    )
}
