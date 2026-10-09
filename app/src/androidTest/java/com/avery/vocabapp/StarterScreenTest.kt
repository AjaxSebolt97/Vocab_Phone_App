package com.avery.vocabapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class StarterScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun starterScreenDisplaysAppName() {
        composeRule.onNodeWithText("Spanish Vocabulary").assertIsDisplayed()
    }

    @Test
    fun startupLoadsAllBundledVocabularyWords() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("5000 words available")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("5000 words available").assertIsDisplayed()
    }

    @Test
    fun gradingFromTheDailySessionAdvancesToTheNextCard() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Start session")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Start session").assertIsEnabled()
        composeRule.onNodeWithText("Start session").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Show answer").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("No cards due").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Loading session...")
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Loading session...").assertCountEquals(0)
        composeRule.onNodeWithText("Show answer").assertIsDisplayed()
        composeRule.onNodeWithText("Show answer").performClick()
        composeRule.onNodeWithText("Again").assertIsDisplayed()
        composeRule.onNodeWithText("Hard").assertIsDisplayed()
        composeRule.onNodeWithText("Good").assertIsDisplayed()
        composeRule.onNodeWithText("Easy").assertIsDisplayed()
        composeRule.onNodeWithText("Good").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Show answer")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Show answer").assertIsDisplayed()
    }
}