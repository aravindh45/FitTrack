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
 * Google Gemini provider. Uses the `generateContent` REST endpoint. Google Search grounding
 * is available but OFF by default: it requires a billed Cloud project even within Gemini's
 * free tier and quickly trips 429 quota errors on a free API key. Flip [useSearchGrounding]
 * on once billing is set up if you want live-web-backed nutrition lookups instead of the
 * model's trained knowledge.
 */
class GeminiProvider(
    private val model: String = "gemini-3.5-flash-lite",
    private val useSearchGrounding: Boolean = false
) : LlmProvider {

    override val id: LlmProviderId = LlmProviderId.GEMINI

    override suspend fun analyzeFood(
        description: String,
        context: NutritionRequestContext,
        apiKey: String
    ): Result<FoodAnalysisResult> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = FoodPromptBuilder.build(description, context)

            val body = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", JsonPrimitive(prompt)) })
                        })
                    })
                })
                if (useSearchGrounding) {
                    put("tools", buildJsonArray {
                        add(buildJsonObject { put("google_search", buildJsonObject {}) })
                    })
                }
                put("generationConfig", buildJsonObject {
                    put("temperature", JsonPrimitive(0.3))
                    // Structured JSON output only works reliably without the search tool
                    // attached, so only request it in the no-grounding (default) path.
                    if (!useSearchGrounding) {
                        put("responseMimeType", JsonPrimitive("application/json"))
                    }
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$model:generateContent?key=$apiKey")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            NetworkClients.okHttp.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                check(response.isSuccessful) {
                    "Gemini request failed (${response.code}): ${responseText.take(300)}"
                }
                val text = extractCandidateText(responseText)
                val jsonPayload = NetworkClients.json.decodeFromString(
                    FoodAnalysisJson.serializer(),
                    extractJsonObject(text)
                )
                jsonPayload.toResult()
            }
        }
    }

    private fun extractCandidateText(rawResponseBody: String): String {
        val root = NetworkClients.json.parseToJsonElement(rawResponseBody).jsonObject
        val candidates = root["candidates"]?.jsonArray
            ?: error("Gemini response had no candidates: ${rawResponseBody.take(300)}")
        val firstCandidate = candidates.firstOrNull()?.jsonObject
            ?: error("Gemini response had an empty candidates array")
        val parts = firstCandidate["content"]?.jsonObject?.get("parts")?.jsonArray
            ?: error("Gemini candidate had no content parts")
        return parts.joinToString(separator = "") { part ->
            part.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
        }
    }

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
