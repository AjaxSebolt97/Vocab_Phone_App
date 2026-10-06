package com.avery.vocabapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class StarterScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun starterScreenDisplaysAppName() {
        composeRule.onNodeWithText("Spanish Vocabulary").assertIsDisplayed()
    }
}