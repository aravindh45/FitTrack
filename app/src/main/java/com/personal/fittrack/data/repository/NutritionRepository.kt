package com.personal.fittrack.data.repository

import com.personal.fittrack.data.remote.LlmProviderFactory
import com.personal.fittrack.data.remote.NutritionRequestContext
import com.personal.fittrack.domain.calc.CalorieNeedCalculator
import com.personal.fittrack.domain.calc.DateUtils
import com.personal.fittrack.domain.model.FoodLogEntry
import com.personal.fittrack.domain.model.GoalType
import com.personal.fittrack.domain.model.UserProfile
import com.personal.fittrack.domain.model.treatsExcessAsBad

/**
 * Orchestrates one "I ate X" log action: figure out where the user stands today, ask the
 * configured LLM for nutrition + a tailored motivational message in a single call, then
 * persist the result. This is the ONLY place in the app that triggers an LLM request -
 * exactly once per logged meal, nothing more.
 */
class NutritionRepository(
    private val providerFactory: LlmProviderFactory,
    private val foodLogRepository: FoodLogRepository
) {

    suspend fun logMeal(description: String, profile: UserProfile): Result<FoodLogEntry> {
        val (provider, apiKey) = providerFactory.currentProviderWithKey().getOrElse {
            return Result.failure(it)
        }

        val today = DateUtils.todayEpochDay()
        val consumedSoFar = foodLogRepository.caloriesConsumedOn(today)
        val dailyGoal = CalorieNeedCalculator.dailyCalorieGoal(profile)

        val context = NutritionRequestContext(
            caloriesConsumedTodayBeforeThisMeal = consumedSoFar,
            dailyCalorieGoal = dailyGoal,
            goalType = profile.goalType
        )

        val analysis = provider.analyzeFood(description, context, apiKey).getOrElse {
            return Result.failure(it)
        }

        val entry = FoodLogEntry(
            rawDescription = description,
            foodName = analysis.foodName,
            calories = analysis.calories,
            proteinG = analysis.proteinG,
            carbsG = analysis.carbsG,
            fatG = analysis.fatG,
            fiberG = analysis.fiberG,
            sugarG = analysis.sugarG,
            motivationMessage = buildFinalMessage(analysis.motivationMessage, consumedSoFar, analysis.calories, dailyGoal, profile.goalType),
            providerUsed = provider.id.label,
            epochMillis = System.currentTimeMillis()
        )

        val id = foodLogRepository.addEntry(entry)
        return Result.success(entry.copy(id = id))
    }

    /** The LLM supplies tone/sentiment only (it is explicitly told not to state numbers).
     * We compute the exact remaining-or-over budget figure ourselves and append it, so the
     * number shown is always arithmetically correct regardless of the model's mental math.
     * Framing flips for a Gain-weight goal: exceeding the goal is the point, not a slip-up. */
    private fun buildFinalMessage(
        llmMessage: String,
        consumedSoFar: Int,
        thisMealCalories: Int,
        dailyGoal: Int,
        goalType: GoalType
    ): String {
        val remainingAfter = dailyGoal - consumedSoFar - thisMealCalories
        val budgetSentence = when {
            remainingAfter >= 0 -> "You have $remainingAfter kcal left today."
            goalType.treatsExcessAsBad() ->
                "You're ${-remainingAfter} kcal over your goal today - maybe ease off for the rest of the day."
            else ->
                "You're ${-remainingAfter} kcal past your target - nice, that's extra fuel toward your gain goal."
        }
        return listOf(llmMessage.trim(), budgetSentence).filter { it.isNotBlank() }.joinToString(" ")
    }
}

/** Fallback message used by the UI layer if an entry somehow has none (e.g. older/edited
 * data) - keeps GoalType usage DRY between here and the dashboard. */
fun GoalType.defaultDeficitLabel(): String = when (this) {
    GoalType.LOSE_WEIGHT -> "calorie deficit"
    GoalType.GAIN_WEIGHT -> "calorie surplus"
    GoalType.MAINTAIN_WEIGHT -> "maintenance"
}
