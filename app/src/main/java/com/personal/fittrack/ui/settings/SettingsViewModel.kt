package com.personal.fittrack.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.health.HealthConnectManager
import com.personal.fittrack.data.repository.ProfileRepository
import com.personal.fittrack.data.settings.SecureSettingsStore
import com.personal.fittrack.domain.model.LlmProviderId
import com.personal.fittrack.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SecureSettingsStore,
    private val profileRepository: ProfileRepository,
    val healthConnectManager: HealthConnectManager
) : ViewModel() {

    val selectedProvider: StateFlow<LlmProviderId> = settingsStore.selectedProvider

    val profile: StateFlow<UserProfile?> = profileRepository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var apiKeyInput by mutableStateOf("")
    var saveConfirmation by mutableStateOf<String?>(null)
        private set

    var calorieGoalSaveConfirmation by mutableStateOf<String?>(null)
        private set

    private val _healthConnectGranted = MutableStateFlow(false)
    val healthConnectGranted: StateFlow<Boolean> = _healthConnectGranted

    fun refreshHealthConnectStatus() {
        viewModelScope.launch {
            _healthConnectGranted.value = healthConnectManager.hasPermissions()
        }
    }

    fun selectProvider(provider: LlmProviderId) {
        settingsStore.setSelectedProvider(provider)
        apiKeyInput = settingsStore.getApiKey(provider).orEmpty()
        saveConfirmation = null
    }

    fun loadKeyForCurrentProvider() {
        apiKeyInput = settingsStore.getApiKey(selectedProvider.value).orEmpty()
    }

    fun saveApiKey() {
        if (apiKeyInput.isBlank()) return
        settingsStore.setApiKey(selectedProvider.value, apiKeyInput)
        saveConfirmation = "${selectedProvider.value.label} key saved."
    }

    fun hasKeyFor(provider: LlmProviderId): Boolean = !settingsStore.getApiKey(provider).isNullOrBlank()

    /** Persists a new deficit/surplus chosen on the Settings slider. Called once on explicit
     * save (not on every drag tick) to avoid hammering Room mid-gesture. */
    fun saveCalorieAdjustment(newAdjustmentKcal: Int) {
        val current = profile.value ?: return
        viewModelScope.launch {
            profileRepository.saveProfile(current.copy(calorieAdjustmentKcal = newAdjustmentKcal))
            calorieGoalSaveConfirmation = "Daily calorie goal updated."
        }
    }
}
