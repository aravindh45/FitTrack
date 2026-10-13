package com.personal.fittrack.data.remote

import com.personal.fittrack.domain.model.LlmProviderId

/**
 * Strategy interface every LLM integration implements. The rest of the app depends only
 * on this - swapping/adding providers never touches ViewModels or UI.
 */
interface LlmProvider {
    val id: LlmProviderId

    suspend fun analyzeFood(
        description: String,
        context: NutritionRequestContext,
        apiKey: String
    ): Result<FoodAnalysisResult>
}
