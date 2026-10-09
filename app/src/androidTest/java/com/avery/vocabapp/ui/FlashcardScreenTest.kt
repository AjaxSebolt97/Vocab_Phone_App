package com.avery.vocabapp.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.avery.vocabapp.data.CardDirection
import com.avery.vocabapp.data.CardEntity
import com.avery.vocabapp.data.StudyCardKind
import com.avery.vocabapp.data.StudySessionCard
import com.avery.vocabapp.data.WordEntity
import com.avery.vocabapp.scheduling.ReviewGrade
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FlashcardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun spanishToEnglishCardRevealsGlossAndAvailableMetadata() {
        composeRule.setContent {
            FlashcardScreen(item(CardDirection.ES_TO_EN))
        }

        composeRule.onNodeWithText("casa").assertIsDisplayed()
        composeRule.onAllNodesWithText("house").assertCountEquals(0)
        composeRule.onNodeWithText("Show answer").performClick()
        composeRule.onNodeWithText("house").assertIsDisplayed()
        composeRule.onNodeWithText("noun").assertIsDisplayed()
        composeRule.onNodeWithText("La casa es grande.").assertIsDisplayed()
        composeRule.onNodeWithText("Gender: feminine").assertIsDisplayed()
    }

    @Test
    fun englishToSpanishCardRevealsSpanishWordAndOmitsUnavailableMetadata() {
        composeRule.setContent {
            FlashcardScreen(item(CardDirection.EN_TO_ES, includeMetadata = false))
        }

        composeRule.onNodeWithText("house").assertIsDisplayed()
        composeRule.onAllNodesWithText("casa").assertCountEquals(0)
        composeRule.onNodeWithText("Show answer").performClick()
        composeRule.onNodeWithText("casa").assertIsDisplayed()
        composeRule.onNodeWithText("noun").assertIsDisplayed()
        composeRule.onAllNodesWithText("La casa es grande.").assertCountEquals(0)
        composeRule.onAllNodesWithText("Gender: feminine").assertCountEquals(0)
    }

    @Test
    fun gradingRevealedCardAdvancesToTheNextCard() {
        val grades = mutableListOf<ReviewGrade>()
        val firstCard = item(CardDirection.ES_TO_EN)
        val secondCard = item(CardDirection.EN_TO_ES).copy(
            word = item(CardDirection.EN_TO_ES).word.copy(spanishText = "perro", gloss = "dog"),
            card = item(CardDirection.EN_TO_ES).card.copy(id = 3),
        )
        val cards = listOf(
            firstCard,
            secondCard,
        )
        composeRule.setContent {
            ReviewSessionScreen(cards) { _, grade ->
                grades.add(grade)
            }
        }

        composeRule.onNodeWithText("casa").assertIsDisplayed()
        composeRule.onNodeWithText("Show answer").performClick()
        composeRule.onNodeWithText("Again").assertIsDisplayed()
        composeRule.onNodeWithText("Hard").assertIsDisplayed()
        composeRule.onNodeWithText("Good").performClick()

        composeRule.onNodeWithText("dog").assertIsDisplayed()
        composeRule.onNodeWithText("Show answer").assertIsDisplayed()
        assertEquals(listOf(ReviewGrade.GOOD), grades)
    }

    @Test
    fun gradeButtonsRemainReachableForLongGlosses() {
        val card = item(CardDirection.ES_TO_EN).copy(
            word = item(CardDirection.ES_TO_EN).word.copy(
                gloss = "A long dictionary definition ".repeat(25),
            ),
        )
        composeRule.setContent {
            FlashcardScreen(card) {}
        }

        composeRule.onNodeWithText("Show answer").performClick()
        composeRule.onNodeWithText("Good").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun exhaustedReviewQueueShowsNoCardsDue() {
        composeRule.setContent {
            ReviewSessionScreen(listOf(item(CardDirection.ES_TO_EN))) { _, _ -> }
        }

        composeRule.onNodeWithText("Show answer").performClick()
        composeRule.onNodeWithText("Easy").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("No cards due").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("No cards due").assertIsDisplayed()
    }

    private fun item(
        direction: CardDirection,
        includeMetadata: Boolean = true,
    ) = StudySessionCard(
        word = WordEntity(
            id = 1,
            rank = 1,
            spanishText = "casa",
            normalizedWord = "casa",
            frequency = 500,
            partOfSpeech = "noun",
            gloss = "house",
            exampleSentence = if (includeMetadata) "La casa es grande." else null,
            grammaticalGender = if (includeMetadata) """["feminine"]""" else null,
        ),
        card = CardEntity(
            id = if (direction == CardDirection.ES_TO_EN) 1 else 2,
            wordId = 1,
            direction = direction,
            easeFactor = 2.5,
            intervalDays = 0,
            dueDate = 0,
            introducedDate = 0,
        ),
        kind = StudyCardKind.NEW_WORD,
    )
}
