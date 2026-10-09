package com.avery.vocabapp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.avery.vocabapp.data.StudySettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savesBothConfiguredDailyLimits() {
        var savedSettings: StudySettings? = null
        composeRule.setContent {
            SettingsScreen(
                settings = StudySettings(),
                onSave = { savedSettings = it },
                onCancel = {},
            )
        }

        composeRule.onNodeWithTag("new-words-per-day").performTextReplacement("3")
        composeRule.onNodeWithTag("daily-review-cap").performTextReplacement("12")
        composeRule.onNodeWithText("Save settings").performClick()

        assertEquals(StudySettings(newWordsPerDay = 3, dailyReviewCap = 12), savedSettings)
    }
}
