package com.personal.fittrack.ui.logfood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.fittrack.ui.common.NutrientRow
import com.personal.fittrack.ui.theme.GoogleBlue
import com.personal.fittrack.ui.theme.GoogleGreen
import com.personal.fittrack.ui.theme.GoogleRed
import com.personal.fittrack.ui.theme.GoogleYellow

@Composable
fun LogFoodScreen(viewModel: LogFoodViewModel) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "What did you eat?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Describe it in your own words, e.g. \"2 mini idli and chicken biryani\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text("Meal description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    // Clearing focus/hiding the keyboard here (not just on navigation) matters:
                    // while the description field is still focused, the FIRST tap anywhere else
                    // on screen - including the bottom nav's Home button - is consumed by Android
                    // just dismissing the keyboard, not by the thing you tapped. Without this,
                    // "Home doesn't respond" right after logging a meal, when it's really just
                    // eating your first tap before a second one gets through.
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    viewModel.submit()
                },
                enabled = viewModel.uiState !is LogFoodUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.height(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log it")
            }

            when (val state = viewModel.uiState) {
                LogFoodUiState.Idle -> Unit
                LogFoodUiState.Loading -> LoadingCard()
                is LogFoodUiState.Success -> ResultCard(
                    state = state,
                    onUndo = { viewModel.undoLastEntry(state.entry.id) },
                    onDismiss = viewModel::resetToIdle
                )
                is LogFoodUiState.Error -> ErrorCard(state.message)
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    ElevatedCard {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.height(24.dp))
            Text("Checking nutrition facts…")
        }
    }
}

@Composable
private fun ResultCard(state: LogFoodUiState.Success, onUndo: () -> Unit, onDismiss: () -> Unit) {
    val entry = state.entry
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(entry.foodName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${entry.calories} kcal", style = MaterialTheme.typography.titleMedium, color = GoogleBlue)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NutrientRow("Protein", entry.proteinG, GoogleGreen, modifier = Modifier.weight(1f))
                NutrientRow("Carbs", entry.carbsG, GoogleYellow, modifier = Modifier.weight(1f))
                NutrientRow("Fat", entry.fatG, GoogleRed, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NutrientRow("Fiber", entry.fiberG, GoogleGreen, modifier = Modifier.weight(1f))
                NutrientRow("Sugar", entry.sugarG, GoogleYellow, modifier = Modifier.weight(1f))
            }

            if (entry.motivationMessage.isNotBlank()) {
                Text(
                    text = entry.motivationMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                TextButton(onClick = onUndo) { Text("Undo") }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
