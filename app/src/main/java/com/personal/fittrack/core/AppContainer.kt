package com.personal.fittrack.core

import android.content.Context
import com.personal.fittrack.data.health.HealthConnectManager
import com.personal.fittrack.data.local.AppDatabase
import com.personal.fittrack.data.remote.LlmProviderFactory
import com.personal.fittrack.data.repository.FoodLogRepository
import com.personal.fittrack.data.repository.NutritionRepository
import com.personal.fittrack.data.repository.ProfileRepository
import com.personal.fittrack.data.settings.SecureSettingsStore

/**
 * Hand-rolled composition root. For an app this size a DI framework (Hilt/Koin) would be
 * pure ceremony - this single object wires every dependency exactly once and everything
 * else just asks for what it needs. If the app grows enough that this gets unwieldy,
 * swapping it for Hilt later only touches this file and call sites that read `appContainer`.
 */
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val secureSettingsStore = SecureSettingsStore(context)
    private val llmProviderFactory = LlmProviderFactory(secureSettingsStore)

    val profileRepository = ProfileRepository(database.userProfileDao())
    val foodLogRepository = FoodLogRepository(database.foodLogDao())
    val nutritionRepository = NutritionRepository(llmProviderFactory, foodLogRepository)
    val settingsStore = secureSettingsStore
    val healthConnectManager = HealthConnectManager(context.applicationContext)
}
