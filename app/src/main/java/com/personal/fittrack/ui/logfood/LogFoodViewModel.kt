package com.personal.fittrack.ui.logfood

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.repository.FoodLogRepository
import com.personal.fittrack.data.repository.NutritionRepository
import com.personal.fittrack.data.repository.ProfileRepository
import com.personal.fittrack.domain.model.FoodLogEntry
import kotlinx.coroutines.launch

sealed interface LogFoodUiState {
    data object Idle : LogFoodUiState
    data object Loading : LogFoodUiState
    data class Success(val entry: FoodLogEntry) : LogFoodUiState
    data class Error(val message: String) : LogFoodUiState
}

class LogFoodViewModel(
    private val nutritionRepository: NutritionRepository,
    private val profileRepository: ProfileRepository,
    private val foodLogRepository: FoodLogRepository
) : ViewModel() {

    var description by mutableStateOf("")
    var uiState by mutableStateOf<LogFoodUiState>(LogFoodUiState.Idle)
        private set

    fun submit() {
        val text = description.trim()
        if (text.isBlank()) {
            uiState = LogFoodUiState.Error("Tell me what you ate first.")
            return
        }
        uiState = LogFoodUiState.Loading
        viewModelScope.launch {
            val profile = profileRepository.getProfileOnce()
            if (profile == null) {
                uiState = LogFoodUiState.Error("Finish onboarding before logging meals.")
                return@launch
            }
            nutritionRepository.logMeal(text, profile).fold(
                onSuccess = { entry ->
                    uiState = LogFoodUiState.Success(entry)
                    description = ""
                },
                onFailure = { error ->
                    uiState = LogFoodUiState.Error(error.message ?: "Something went wrong, try again.")
                }
            )
        }
    }

    fun undoLastEntry(entryId: Long) {
        viewModelScope.launch { foodLogRepository.deleteEntry(entryId) }
        uiState = LogFoodUiState.Idle
    }

    fun resetToIdle() {
        uiState = LogFoodUiState.Idle
    }
}
