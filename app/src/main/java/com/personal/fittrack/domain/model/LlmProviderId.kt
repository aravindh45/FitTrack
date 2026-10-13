package com.personal.fittrack.domain.model

/**
 * Supported LLM providers. Adding a new provider is: add an id here, add a matching
 * [com.personal.fittrack.data.remote.LlmProvider] implementation, and register it in
 * LlmProviderFactory. Nothing else in the app needs to know the difference.
 */
enum class LlmProviderId(val label: String) {
    GEMINI("Google Gemini"),
    OPENAI("OpenAI"),
    CLAUDE("Anthropic Claude")
}
