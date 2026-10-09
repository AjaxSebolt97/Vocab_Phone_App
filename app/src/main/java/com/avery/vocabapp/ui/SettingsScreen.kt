package com.avery.vocabapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.avery.vocabapp.data.StudySettings

@Composable
fun SettingsScreen(
    settings: StudySettings,
    onSave: (StudySettings) -> Unit,
    onCancel: () -> Unit,
) {
    var newWordsText by remember(settings) { mutableStateOf(settings.newWordsPerDay.toString()) }
    var reviewCapText by remember(settings) { mutableStateOf(settings.dailyReviewCap.toString()) }
    val newWords = newWordsText.toIntOrNull()?.takeIf { it >= 0 }
    val reviewCap = reviewCapText.toIntOrNull()?.takeIf { it >= 0 }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new-words-per-day"),
            value = newWordsText,
            onValueChange = { newWordsText = it },
            label = { Text("New words per day") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily-review-cap"),
            value = reviewCapText,
            onValueChange = { reviewCapText = it },
            label = { Text("Daily review cap") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        if (newWords == null || reviewCap == null) {
            Text("Enter a non-negative whole number for each setting.")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = newWords != null && reviewCap != null,
            onClick = {
                onSave(
                    StudySettings(
                        newWordsPerDay = requireNotNull(newWords),
                        dailyReviewCap = requireNotNull(reviewCap),
                    ),
                )
            },
        ) {
            Text("Save settings")
        }
        Button(modifier = Modifier.fillMaxWidth(), onClick = onCancel) {
            Text("Cancel")
        }
    }
}
