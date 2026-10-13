package com.personal.fittrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.health.HealthConnectManager
import com.personal.fittrack.data.repository.FoodLogRepository
import com.personal.fittrack.data.repository.ProfileRepository
import com.personal.fittrack.domain.calc.CalorieNeedCalculator
import com.personal.fittrack.domain.calc.DateUtils
import com.personal.fittrack.domain.model.DailySummary
import com.personal.fittrack.domain.model.FoodLogEntry
import com.personal.fittrack.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Home is deliberately today-only - multi-day trends (weekly/monthly/all-time) live in
 * History instead, so this screen stays a quick "where do I stand right now" glance. */
data class DashboardUiState(
    val profile: UserProfile? = null,
    val dailyCalorieGoal: Int = 0,
    val today: DailySummary? = null,
    val isLoading: Boolean = true
) {
    val caloriesConsumedToday: Int get() = today?.totalCalories ?: 0
    val caloriesRemainingToday: Int get() = (dailyCalorieGoal - caloriesConsumedToday).coerceAtLeast(0)
    val isOverBudgetToday: Boolean get() = dailyCalorieGoal > 0 && caloriesConsumedToday > dailyCalorieGoal
}

class DashboardViewModel(
    profileRepository: ProfileRepository,
    private val foodLogRepository: FoodLogRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        profileRepository.observeProfile(),
        foodLogRepository.observeDailySummary(DateUtils.todayEpochDay())
    ) { profile, today ->
        DashboardUiState(
            profile = profile,
            dailyCalorieGoal = profile?.let { CalorieNeedCalculator.dailyCalorieGoal(it) } ?: 0,
            today = today,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    // Health Connect has no built-in reactive Flow for records, so this is a plain
    // fetch-and-cache value refreshed whenever the Dashboard becomes visible (see
    // DashboardScreen's LaunchedEffect) rather than something that auto-updates live.
    private val _activeCaloriesBurnedToday = MutableStateFlow(0)
    val activeCaloriesBurnedToday: StateFlow<Int> = _activeCaloriesBurnedToday

    fun refreshActiveCaloriesBurned() {
        viewModelScope.launch {
            _activeCaloriesBurnedToday.value = healthConnectManager.activeCaloriesBurnedOn(DateUtils.todayEpochDay())
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { foodLogRepository.deleteEntry(id) }
    }

    fun relogEntry(entry: FoodLogEntry) {
        viewModelScope.launch { foodLogRepository.relogEntry(entry) }
    }
}
