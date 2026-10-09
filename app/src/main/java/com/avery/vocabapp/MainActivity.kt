package com.avery.vocabapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avery.vocabapp.data.VocabularyDatabase
import com.avery.vocabapp.data.CardRepository
import com.avery.vocabapp.data.StudySessionCard
import com.avery.vocabapp.data.StudySessionRepository
import com.avery.vocabapp.data.StudySessionPreview
import com.avery.vocabapp.data.SettingsRepository
import com.avery.vocabapp.data.StudySettings
import com.avery.vocabapp.scheduling.ReviewGrade
import com.avery.vocabapp.ui.ReviewSessionScreen
import com.avery.vocabapp.ui.SettingsScreen
import com.avery.vocabapp.ui.StudyStartScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = VocabularyDatabase.getInstance(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StudyApp(database)
                }
            }
        }
    }
}

@Composable
private fun StudyApp(database: VocabularyDatabase) {
    val studyRepository = remember(database) { StudySessionRepository(database) }
    val cardRepository = remember(database) { CardRepository(database) }
    val settingsRepository = remember(database) { SettingsRepository(database.settingsDao()) }
    val scope = rememberCoroutineScope()
    var wordCount by remember { mutableStateOf<Int?>(null) }
    var preview by remember { mutableStateOf<StudySessionPreview?>(null) }
    var settings by remember { mutableStateOf<StudySettings?>(null) }
    var session by remember { mutableStateOf<List<StudySessionCard>?>(null) }
    var startingSession by remember { mutableStateOf(false) }
    var showingSettings by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(database) {
        wordCount = database.wordDao().count()
        val loadedSettings = settingsRepository.load()
        settings = loadedSettings
        preview = studyRepository.previewDailySession(
            loadedSettings.newWordsPerDay,
            loadedSettings.dailyReviewCap,
            System.currentTimeMillis(),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Spanish Vocabulary")
            Text(text = wordCount?.let { "$it words available" } ?: "Loading vocabulary...")
        }
        when {
            showingSettings -> {
                val currentSettings = settings
                if (currentSettings == null) {
                    Text("Loading settings...")
                } else {
                    SettingsScreen(
                        settings = currentSettings,
                        onSave = { updatedSettings ->
                            scope.launch {
                                settingsRepository.save(updatedSettings)
                                settings = updatedSettings
                                preview = studyRepository.previewDailySession(
                                    updatedSettings.newWordsPerDay,
                                    updatedSettings.dailyReviewCap,
                                    System.currentTimeMillis(),
                                )
                                showingSettings = false
                            }
                        },
                        onCancel = { showingSettings = false },
                    )
                }
            }
            session != null -> ReviewSessionScreen(session!!) { item, grade ->
                cardRepository.gradeCard(item.card.id, grade, System.currentTimeMillis())
            }
            startingSession -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading session...")
            }
            preview != null -> StudyStartScreen(
                preview = preview!!,
                onStartSession = {
                    startingSession = true
                    scope.launch {
                        try {
                            session = studyRepository.createDailySession(System.currentTimeMillis())
                        } finally {
                            startingSession = false
                        }
                    }
                },
                onOpenSettings = { showingSettings = true },
            )
            else -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading today's session...")
            }
        }
    }
}