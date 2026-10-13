package com.personal.fittrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.health.HealthConnectManager
import com.personal.fittrack.data.repository.FoodLogRepository
import com.personal.fittrack.data.repository.ProfileRepository
import com.personal.fittrack.domain.calc.DateUtils
import com.personal.fittrack.domain.model.DailySummary
import com.personal.fittrack.domain.model.FoodLogEntry
import com.personal.fittrack.domain.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val HISTORY_WINDOW_DAYS = 365L

/**
 * Backs both sections of History: the day-wise log list and the Charts tab. Charts are
 * derived from the same [summaries] list in the UI layer (cheap - at most 365 small
 * records) rather than issuing extra DB queries, so there's exactly one source of truth.
 * Exposes the full [profile] (not just a precomputed goal number) so the UI can also read
 * goalType - summary cards need it to know whether a surplus is good or bad news.
 */
class HistoryViewModel(
    private val foodLogRepository: FoodLogRepository,
    profileRepository: ProfileRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    val summaries: StateFlow<List<DailySummary>> = foodLogRepository
        .observeRangeSummaries(
            startEpochDay = DateUtils.todayEpochDay() - HISTORY_WINDOW_DAYS,
            endEpochDay = DateUtils.todayEpochDay()
        )
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<UserProfile?> = profileRepository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun deleteEntry(id: Long) {
        viewModelScope.launch { foodLogRepository.deleteEntry(id) }
    }

    fun relogEntry(entry: FoodLogEntry) {
        viewModelScope.launch { foodLogRepository.relogEntry(entry) }
    }

    /** One Health Connect query covering the whole range History can show (the same 365-day
     * window as [summaries]) - cheap to fetch all at once and slice per day/week/month in the
     * UI, rather than issuing a separate query per chart tab. */
    suspend fun activeCaloriesBurnedByDay(startEpochDay: Long, endEpochDay: Long): Map<Long, Int> =
        healthConnectManager.activeCaloriesBurnedByDay(startEpochDay, endEpochDay)
}
