package com.personal.fittrack.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.personal.fittrack.core.AppContainer
import com.personal.fittrack.ui.dashboard.DashboardViewModel
import com.personal.fittrack.ui.history.HistoryViewModel
import com.personal.fittrack.ui.logfood.LogFoodViewModel
import com.personal.fittrack.ui.onboarding.OnboardingViewModel
import com.personal.fittrack.ui.settings.SettingsViewModel

/**
 * One small factory for every screen ViewModel. Each branch just forwards the
 * already-constructed repositories from [AppContainer] - adding a new screen means adding
 * one branch here, nothing else in the DI story changes.
 */
class AppViewModelFactory(private val appContainer: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        OnboardingViewModel::class.java ->
            OnboardingViewModel(appContainer.profileRepository) as T

        DashboardViewModel::class.java ->
            DashboardViewModel(appContainer.profileRepository, appContainer.foodLogRepository, appContainer.healthConnectManager) as T

        LogFoodViewModel::class.java ->
            LogFoodViewModel(
                appContainer.nutritionRepository,
                appContainer.profileRepository,
                appContainer.foodLogRepository
            ) as T

        HistoryViewModel::class.java ->
            HistoryViewModel(appContainer.foodLogRepository, appContainer.profileRepository, appContainer.healthConnectManager) as T

        SettingsViewModel::class.java ->
            SettingsViewModel(appContainer.settingsStore, appContainer.profileRepository, appContainer.healthConnectManager) as T

        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
