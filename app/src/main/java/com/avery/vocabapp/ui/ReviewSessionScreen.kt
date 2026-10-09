package com.avery.vocabapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.avery.vocabapp.data.StudySessionCard
import com.avery.vocabapp.scheduling.ReviewGrade
import kotlinx.coroutines.launch

@Composable
fun ReviewSessionScreen(
    cards: List<StudySessionCard>,
    onGrade: suspend (StudySessionCard, ReviewGrade) -> Unit,
) {
    var currentIndex by remember(cards) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    if (currentIndex >= cards.size) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cards due")
        }
    } else {
        val currentCard = cards[currentIndex]
        FlashcardScreen(currentCard) { grade ->
            scope.launch {
                onGrade(currentCard, grade)
                currentIndex += 1
            }
        }
    }
}
