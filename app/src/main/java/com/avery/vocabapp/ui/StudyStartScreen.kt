package com.avery.vocabapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avery.vocabapp.data.CardDirection
import com.avery.vocabapp.data.StudyCardKind
import com.avery.vocabapp.data.StudySessionPreview

@Composable
fun StudyStartScreen(
    preview: StudySessionPreview,
    onStartSession: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Today's study session", style = MaterialTheme.typography.headlineSmall)
        Text("Due reviews (${preview.dueReviews.size})", style = MaterialTheme.typography.titleMedium)
        Text("New words (${preview.newWords.size})", style = MaterialTheme.typography.titleMedium)
        if (preview.totalCardCount == 0) {
            Text("No cards due")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStartSession,
            enabled = preview.totalCardCount > 0,
        ) {
            Text("Start session")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenSettings,
        ) {
            Text("Settings")
        }
        preview.dueReviews.forEach { review ->
            Text(
                "${review.word.spanishText} — ${directionLabel(review.card.direction)}",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        preview.newWords.forEach { word ->
            Text(
                "${word.spanishText} — ${word.gloss} (both directions)",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private fun directionLabel(direction: CardDirection): String = when (direction) {
    CardDirection.ES_TO_EN -> "Spanish to English"
    CardDirection.EN_TO_ES -> "English to Spanish"
}
