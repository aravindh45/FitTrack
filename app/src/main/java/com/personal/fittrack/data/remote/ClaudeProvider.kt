package com.personal.fittrack.data.remote

import com.personal.fittrack.domain.model.LlmProviderId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Anthropic Claude provider via the Messages API. Claude has no forced-JSON mode, so we
 * lean on [extractJsonObject] to tolerate any stray formatting despite our instructions.
 */
class ClaudeProvider(
    private val model: String = "claude-3-5-sonnet-20241022"
) : LlmProvider {

    override val id: LlmProviderId = LlmProviderId.CLAUDE

    override suspend fun analyzeFood(
        description: String,
        context: NutritionRequestContext,
        apiKey: String
    ): Result<FoodAnalysisResult> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = FoodPromptBuilder.build(description, context)

            val body = buildJsonObject {
                put("model", JsonPrimitive(model))
                put("max_tokens", JsonPrimitive(1024))
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", JsonPrimitive("user"))
                        put("content", JsonPrimitive(prompt))
                    })
                })
            }

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            NetworkClients.okHttp.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                check(response.isSuccessful) {
                    "Claude request failed (${response.code}): ${responseText.take(300)}"
                }
                val root = NetworkClients.json.parseToJsonElement(responseText).jsonObject
                val text = root["content"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("text")?.jsonPrimitive?.content
                    ?: error("Claude response had no text content")
                val jsonPayload = NetworkClients.json.decodeFromString(
                    FoodAnalysisJson.serializer(),
                    extractJsonObject(text)
                )
                jsonPayload.toResult()
            }
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
