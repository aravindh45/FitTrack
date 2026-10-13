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
 * OpenAI provider via the Chat Completions API with JSON-object response mode. No live
 * web-search grounding here (would require the separate Assistants/Responses API with
 * browsing tools) - straightforward to upgrade later behind this same interface.
 */
class OpenAiProvider(
    private val model: String = "gpt-4o-mini"
) : LlmProvider {

    override val id: LlmProviderId = LlmProviderId.OPENAI

    override suspend fun analyzeFood(
        description: String,
        context: NutritionRequestContext,
        apiKey: String
    ): Result<FoodAnalysisResult> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = FoodPromptBuilder.build(description, context)

            val body = buildJsonObject {
                put("model", JsonPrimitive(model))
                put("response_format", buildJsonObject { put("type", JsonPrimitive("json_object")) })
                put("temperature", JsonPrimitive(0.3))
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", JsonPrimitive("user"))
                        put("content", JsonPrimitive(prompt))
                    })
                })
            }

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            NetworkClients.okHttp.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                check(response.isSuccessful) {
                    "OpenAI request failed (${response.code}): ${responseText.take(300)}"
                }
                val root = NetworkClients.json.parseToJsonElement(responseText).jsonObject
                val content = root["choices"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("message")?.jsonObject?.get("content")
                    ?.jsonPrimitive?.content
                    ?: error("OpenAI response had no message content")
                val jsonPayload = NetworkClients.json.decodeFromString(
                    FoodAnalysisJson.serializer(),
                    extractJsonObject(content)
                )
                jsonPayload.toResult()
            }
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
