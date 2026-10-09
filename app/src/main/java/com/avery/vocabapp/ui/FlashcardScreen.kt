package com.avery.vocabapp.ui

import org.json.JSONArray
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.avery.vocabapp.data.CardDirection
import com.avery.vocabapp.data.StudySessionCard
import com.avery.vocabapp.scheduling.ReviewGrade

@Composable
fun FlashcardScreen(
    item: StudySessionCard,
    onGrade: ((ReviewGrade) -> Unit)? = null,
) {
    var answerRevealed by remember(item.card.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (answerRevealed) {
                    AnswerContent(item)
                } else {
                    Text(
                        text = if (item.card.direction == CardDirection.ES_TO_EN) {
                            item.word.spanishText
                        } else {
                            item.word.gloss
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (!answerRevealed) {
            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = { answerRevealed = true },
            ) {
                Text("Show answer")
            }
        } else if (onGrade != null) {
            GradeButtons(onGrade)
        }
    }
}

@Composable
private fun GradeButtons(onGrade: (ReviewGrade) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradeButton(ReviewGrade.AGAIN, onGrade, Modifier.weight(1f))
            GradeButton(ReviewGrade.HARD, onGrade, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradeButton(ReviewGrade.GOOD, onGrade, Modifier.weight(1f))
            GradeButton(ReviewGrade.EASY, onGrade, Modifier.weight(1f))
        }
    }
}

@Composable
private fun GradeButton(
    grade: ReviewGrade,
    onGrade: (ReviewGrade) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(modifier = modifier, onClick = { onGrade(grade) }) {
        Text(
            when (grade) {
                ReviewGrade.AGAIN -> "Again"
                ReviewGrade.HARD -> "Hard"
                ReviewGrade.GOOD -> "Good"
                ReviewGrade.EASY -> "Easy"
            },
        )
    }
}

@Composable
private fun AnswerContent(item: StudySessionCard) {
    val word = item.word
    val answer = if (item.card.direction == CardDirection.ES_TO_EN) {
        word.gloss
    } else {
        word.spanishText
    }

    Text(
        text = answer,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Text(text = word.partOfSpeech)
    word.exampleSentence?.let { Text(text = it) }
    word.grammaticalGender?.let { Text(text = "Gender: ${formatGender(it)}") }
}

private fun formatGender(value: String): String {
    val genders = JSONArray(value)
    return (0 until genders.length())
        .map(genders::getString)
        .joinToString(", ")
}
