package com.personal.fittrack.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.personal.fittrack.domain.model.LlmProviderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * API keys are secrets, so they live in EncryptedSharedPreferences (backed by the Android
 * Keystore) rather than plain DataStore. This is the ONLY place in the app that touches
 * raw key strings; everything else asks [LlmProviderFactory] for an already-configured
 * provider instead of reading keys directly.
 */
class SecureSettingsStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "fittrack_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _selectedProvider = MutableStateFlow(readSelectedProvider())
    val selectedProvider: StateFlow<LlmProviderId> = _selectedProvider.asStateFlow()

    fun setSelectedProvider(provider: LlmProviderId) {
        prefs.edit().putString(KEY_SELECTED_PROVIDER, provider.name).apply()
        _selectedProvider.value = provider
    }

    fun getApiKey(provider: LlmProviderId): String? = prefs.getString(apiKeyPrefKey(provider), null)

    fun setApiKey(provider: LlmProviderId, key: String) {
        prefs.edit().putString(apiKeyPrefKey(provider), key.trim()).apply()
    }

    fun hasAnyApiKey(): Boolean = LlmProviderId.entries.any { !getApiKey(it).isNullOrBlank() }

    private fun readSelectedProvider(): LlmProviderId {
        val saved = prefs.getString(KEY_SELECTED_PROVIDER, null) ?: return LlmProviderId.GEMINI
        return runCatching { LlmProviderId.valueOf(saved) }.getOrDefault(LlmProviderId.GEMINI)
    }

    private fun apiKeyPrefKey(provider: LlmProviderId) = "api_key_${provider.name}"

    companion object {
        private const val KEY_SELECTED_PROVIDER = "selected_provider"
    }
}
