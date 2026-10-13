package com.personal.fittrack.data.remote

import com.personal.fittrack.domain.model.GoalType
import kotlinx.serialization.Serializable

/** Everything a provider needs to know besides the raw food description, so it can tailor
 * the motivational message to where the user actually stands today. */
data class NutritionRequestContext(
    val caloriesConsumedTodayBeforeThisMeal: Int,
    val dailyCalorieGoal: Int,
    val goalType: GoalType
)

/** Provider-agnostic result the rest of the app works with. */
data class FoodAnalysisResult(
    val foodName: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val sugarG: Double,
    val motivationMessage: String
)

/** Shape we ask every LLM to reply in. Kept minimal and flat on purpose - easy for any
 * model to produce reliably, and easy to extend with new fields later. */
@Serializable
internal data class FoodAnalysisJson(
    val foodName: String = "Unknown food",
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val motivationMessage: String = ""
)

internal fun FoodAnalysisJson.toResult() = FoodAnalysisResult(
    foodName = foodName,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    motivationMessage = motivationMessage
)

/** One prompt, shared by every provider, so tweaking the nutrition/motivation instructions
 * only ever happens in one place (DRY). */
internal object FoodPromptBuilder {

    fun build(description: String, context: NutritionRequestContext): String {
        return """
            You are a nutrition assistant inside a personal calorie-tracking app.
            A user just logged this meal in their own words: "$description"

            Use your knowledge (and live web/search results if you have access to them) to
            estimate realistic nutrition facts for this meal as actually eaten (assume a
            typical single-serving portion unless a quantity is stated).

            Write foodName as a SHORT, appetizing summary title for this log entry - like a
            menu heading, not a restatement of the user's raw input. Max 5 words, title case,
            no quantities/measurements (those belong in the nutrition numbers, not the title).
            Example: input "2 chappathi 100 ml chicken gravy 2 piece chicken nuggets" should
            produce foodName like "Chapati with Chicken Gravy & Nuggets", NOT a verbatim copy
            of the input.

            Context about the user's day so far (for tone only - do NOT do arithmetic on
            these numbers yourself, never state a specific remaining-calories figure):
            - Calories already logged today before this meal: ${context.caloriesConsumedTodayBeforeThisMeal}
            - Daily calorie goal: ${context.dailyCalorieGoal}
            - Goal type: ${context.goalType.label}

            Write motivationMessage as ONE short, warm sentence reacting to the food choice
            itself (e.g. praise a balanced/healthy meal, or gently note an indulgent one).
            Do NOT mention any specific calorie numbers, totals, or "remaining budget" figures
            in motivationMessage - the app appends a precisely-calculated budget line after
            your message itself, so a second mention from you would be redundant or wrong.

            Respond with ONLY a single valid JSON object, no markdown code fences, no extra
            commentary, matching EXACTLY this shape:
            {
              "foodName": string,
              "calories": integer,
              "proteinG": number,
              "carbsG": number,
              "fatG": number,
              "fiberG": number,
              "sugarG": number,
              "motivationMessage": string
            }
        """.trimIndent()
    }
}

/** Pulls a JSON object out of a raw LLM text response, tolerating markdown code fences or
 * stray commentary that some models add despite instructions not to. */
internal fun extractJsonObject(raw: String): String {
    val fenceStripped = raw
        .replace("```json", "")
        .replace("```", "")
        .trim()
    val start = fenceStripped.indexOf('{')
    val end = fenceStripped.lastIndexOf('}')
    if (start == -1 || end == -1 || end < start) return fenceStripped
    return fenceStripped.substring(start, end + 1)
}
