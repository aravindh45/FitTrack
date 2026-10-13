package com.personal.fittrack.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A single line of text that shrinks its own font size until it fits, instead of wrapping
 * or ellipsizing. Used for stat-card values where the content (e.g. BMI category word) is
 * short but unpredictable in length - "Normal" and "Underweight" both need to look
 * intentional, not truncated.
 *
 * Pre-dates Compose's built-in TextAutoSize (not yet in this project's Compose BOM) - this
 * is the standard manual technique: measure, shrink by 10% on overflow, repeat.
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    minFontSize: TextUnit = 11.sp
) {
    var resolvedStyle by remember(text, style) { mutableStateOf(style) }
    var readyToDraw by remember(text, style) { mutableStateOf(false) }

    Box(modifier = modifier) {
        Text(
            text = text,
            style = resolvedStyle,
            color = color,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
            modifier = Modifier.drawWithContent { if (readyToDraw) drawContent() },
            onTextLayout = { result ->
                val currentSize = resolvedStyle.fontSize
                if (result.didOverflowWidth && currentSize > minFontSize) {
                    val next = (currentSize.value * 0.9f).sp
                    resolvedStyle = resolvedStyle.copy(fontSize = if (next < minFontSize) minFontSize else next)
                } else {
                    readyToDraw = true
                }
            }
        )
    }
}
