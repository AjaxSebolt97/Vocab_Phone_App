package com.avery.vocabapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.avery.vocabapp.data.CardDirection
import com.avery.vocabapp.data.CardEntity
import com.avery.vocabapp.data.StudyCardKind
import com.avery.vocabapp.data.StudySessionCard
import com.avery.vocabapp.data.StudySessionPreview
import com.avery.vocabapp.data.WordEntity
import org.junit.Rule
import org.junit.Test

class StudyStartScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun previewShowsDueReviewsAndNewWordsTogether() {
        val reviewWord = word(id = 1, spanish = "casa", gloss = "house")
        val newWord = word(id = 2, spanish = "perro", gloss = "dog")
        val preview = StudySessionPreview(
            dueReviews = listOf(
                StudySessionCard(
                    word = reviewWord,
                    card = card(reviewWord.id, CardDirection.ES_TO_EN),
                    kind = StudyCardKind.DUE_REVIEW,
                ),
            ),
            newWords = listOf(newWord),
        )
        composeRule.setContent {
            StudyStartScreen(preview, onStartSession = {})
        }

        composeRule.onNodeWithText("Today's study session").assertIsDisplayed()
        composeRule.onNodeWithText("Due reviews (1)").assertIsDisplayed()
        composeRule.onNodeWithText("casa — Spanish to English").assertIsDisplayed()
        composeRule.onNodeWithText("New words (1)").assertIsDisplayed()
        composeRule.onNodeWithText("perro — dog (both directions)").assertIsDisplayed()
        composeRule.onNodeWithText("Start session").assertIsEnabled()
    }

    @Test
    fun emptyPreviewShowsNoCardsDueAndDisablesStarting() {
        composeRule.setContent {
            StudyStartScreen(
                preview = StudySessionPreview(dueReviews = emptyList(), newWords = emptyList()),
                onStartSession = {},
            )
        }

        composeRule.onNodeWithText("No cards due").assertIsDisplayed()
        composeRule.onNodeWithText("Start session").assertIsNotEnabled()
    }

    private fun word(id: Long, spanish: String, gloss: String) = WordEntity(
        id = id,
        rank = id.toInt(),
        spanishText = spanish,
        normalizedWord = spanish,
        frequency = 100,
        partOfSpeech = "noun",
        gloss = gloss,
        exampleSentence = null,
        grammaticalGender = null,
    )

    private fun card(wordId: Long, direction: CardDirection) = CardEntity(
        id = wordId,
        wordId = wordId,
        direction = direction,
        easeFactor = 2.5,
        intervalDays = 1,
        dueDate = 0,
        introducedDate = 0,
    )
}
