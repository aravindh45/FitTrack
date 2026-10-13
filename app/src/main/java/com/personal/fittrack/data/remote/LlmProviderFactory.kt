package com.personal.fittrack.data.remote

import com.personal.fittrack.data.settings.SecureSettingsStore
import com.personal.fittrack.domain.model.LlmProviderId

/**
 * Looks up the currently-selected provider and its API key. This is the single place that
 * knows about every concrete provider implementation - add a new one here and it's
 * available everywhere else in the app for free.
 */
class LlmProviderFactory(private val settingsStore: SecureSettingsStore) {

    private val providers: Map<LlmProviderId, LlmProvider> = mapOf(
        LlmProviderId.GEMINI to GeminiProvider(),
        LlmProviderId.OPENAI to OpenAiProvider(),
        LlmProviderId.CLAUDE to ClaudeProvider()
    )

    /** Returns the active provider plus its stored API key, or a descriptive failure if the
     * user hasn't configured one yet. */
    fun currentProviderWithKey(): Result<Pair<LlmProvider, String>> {
        val providerId = settingsStore.selectedProvider.value
        val provider = providers.getValue(providerId)
        val apiKey = settingsStore.getApiKey(providerId)
        return if (apiKey.isNullOrBlank()) {
            Result.failure(
                IllegalStateException(
                    "No API key saved for ${providerId.label}. Add one in Settings first."
                )
            )
        } else {
            Result.success(provider to apiKey)
        }
    }
}
