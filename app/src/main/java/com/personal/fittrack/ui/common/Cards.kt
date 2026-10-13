package com.personal.fittrack.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.fittrack.domain.calc.DateUtils
import com.personal.fittrack.domain.model.FoodLogEntry
import com.personal.fittrack.ui.theme.GoogleBlue
import com.personal.fittrack.ui.theme.GoogleGreen
import com.personal.fittrack.ui.theme.GoogleRed
import com.personal.fittrack.ui.theme.GoogleYellow

/** A small, colorful stat tile - reused across Dashboard/History for consistency. The value
 * uses AutoSizeText because this tile sits three-up in a row, and the longest BMI category
 * word ("Underweight"/"Overweight") would otherwise wrap or get ellipsized at a fixed size. */
@Composable
fun StatCard(
    title: String,
    value: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = accentColor.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AutoSizeText(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Tiny nutrient tile for the daily-card header row (calories/protein/carbs/fat at a glance) -
 * deliberately smaller than [StatCard] since four of these sit side by side. */
@Composable
fun NutrientMiniCard(
    label: String,
    value: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = accentColor.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AutoSizeText(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A labeled macro-nutrient value, e.g. "Protein · 32g". */
@Composable
fun NutrientRow(label: String, grams: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "${grams.toInt()} g",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

/** One logged meal, rendered the same way everywhere it shows up (today's log on the
 * Dashboard, and every day in History). Collapsed state is deliberately minimal: the full
 * meal title (wraps across lines rather than ever truncating - you typed it, you should be
 * able to read it back) and the time it was logged, nothing else competing for space. Tap
 * the card to expand into the full breakdown: calories, macros, fiber/sugar, and the
 * relog/delete actions. Never shows the user's raw typed prompt, only the cleaned-up title
 * the LLM returns as foodName.
 *
 * [onRelog] ("+") logs this exact food again right now - handy for "I had the same tea again"
 * without retyping or re-asking the LLM. [onDelete] ("-") removes a mis-logged entry. Both are
 * optional so the card still renders fine anywhere a read-only view is all that's needed;
 * both only appear once expanded, keeping the collapsed row free of competing tap targets. */
@Composable
fun MealLogCard(
    entry: FoodLogEntry,
    onRelog: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.foodName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = DateUtils.formatTime(entry.epochMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Hide details" else "Show details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        text = "${entry.calories} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoogleBlue
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        NutrientRow("Protein", entry.proteinG, GoogleGreen, modifier = Modifier.weight(1f))
                        NutrientRow("Carbs", entry.carbsG, GoogleYellow, modifier = Modifier.weight(1f))
                        NutrientRow("Fat", entry.fatG, GoogleRed, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        NutrientRow("Fiber", entry.fiberG, GoogleGreen, modifier = Modifier.weight(1f))
                        NutrientRow("Sugar", entry.sugarG, GoogleYellow, modifier = Modifier.weight(1f))
                    }
                    if (onRelog != null || onDelete != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            if (onRelog != null) {
                                IconButton(onClick = onRelog, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        Icons.Filled.AddCircleOutline,
                                        contentDescription = "Log this again",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (onDelete != null) {
                                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        Icons.Filled.RemoveCircleOutline,
                                        contentDescription = "Delete entry",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
