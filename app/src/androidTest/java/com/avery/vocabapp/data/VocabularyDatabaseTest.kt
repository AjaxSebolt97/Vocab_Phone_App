package com.avery.vocabapp.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.avery.vocabapp.scheduling.ReviewGrade
import com.avery.vocabapp.scheduling.Sm2Scheduler
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VocabularyDatabaseTest {
    private lateinit var database: VocabularyDatabase

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, VocabularyDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertsAndReadsWordWithTwoIndependentlyScheduledCards() = runBlocking {
        val word = WordEntity(
            id = 1,
            rank = 1,
            spanishText = "casa",
            normalizedWord = "casa",
            frequency = 500,
            partOfSpeech = "noun",
            gloss = "house",
            exampleSentence = "La casa es grande.",
            grammaticalGender = """["feminine"]""",
        )
        database.wordDao().insert(word)

        database.cardDao().insertAll(
            listOf(
                CardEntity(
                    wordId = word.id,
                    direction = CardDirection.ES_TO_EN,
                    easeFactor = 2.5,
                    intervalDays = 1,
                    dueDate = 86_400_000,
                    introducedDate = 0,
                ),
                CardEntity(
                    wordId = word.id,
                    direction = CardDirection.EN_TO_ES,
                    easeFactor = 2.5,
                    intervalDays = 2,
                    dueDate = 172_800_000,
                    introducedDate = 0,
                ),
            ),
        )

        assertEquals(word, database.wordDao().findById(word.id))
        assertEquals(
            listOf(CardDirection.ES_TO_EN, CardDirection.EN_TO_ES),
            database.cardDao().findForWord(word.id).map { it.direction },
        )
        assertEquals(
            listOf(86_400_000L, 172_800_000L),
            database.cardDao().findForWord(word.id).map { it.dueDate },
        )
        assertEquals(1, database.wordDao().count())
    }

    @Test
    fun selectsNotIntroducedWordsByAscendingRank() = runBlocking {
        val firstWord = word(id = 1, rank = 1)
        val secondWord = word(id = 2, rank = 2)
        val thirdWord = word(id = 3, rank = 3)
        database.wordDao().insert(thirdWord)
        database.wordDao().insert(firstWord)
        database.wordDao().insert(secondWord)
        val repository = StudySessionRepository(database)

        assertEquals(
            listOf(firstWord, secondWord),
            repository.introduceNextWords(limit = 2, introducedAt = 100),
        )
        assertEquals(
            listOf(thirdWord),
            repository.introduceNextWords(limit = 2, introducedAt = 200),
        )
        assertEquals(emptyList<WordEntity>(), repository.introduceNextWords(limit = 0, introducedAt = 300))
        assertEquals(2, database.cardDao().findForWord(firstWord.id).size)
        assertEquals(2, database.cardDao().findForWord(secondWord.id).size)
        assertEquals(2, database.cardDao().findForWord(thirdWord.id).size)
    }

    @Test
    fun dueReviewLimitLeavesOverflowDueForTheNextDay() = runBlocking {
        val words = (1L..3L).map { word(id = it, rank = it.toInt()) }
        words.forEach { database.wordDao().insert(it) }
        val now = 1_000L
        database.cardDao().insertAll(
            words.mapIndexed { index, word ->
                card(
                    wordId = word.id,
                    direction = CardDirection.ES_TO_EN,
                    dueDate = 100L * (index + 1),
                )
            },
        )
        val repository = StudySessionRepository(database)

        val today = repository.selectDueReviews(reviewLimit = 2, now = now)
        val tomorrow = repository.selectDueReviews(
            reviewLimit = 3,
            now = now + Sm2Scheduler.MILLIS_PER_DAY,
        )

        assertEquals(listOf(100L, 200L), today.map { it.dueDate })
        assertTrue(tomorrow.any { it.wordId == words.last().id })
    }

    @Test
    fun dailySessionCombinesCappedReviewsAndBothDirectionsForNewWords() = runBlocking {
        val reviewWords = (1L..3L).map { word(id = it, rank = it.toInt()) }
        val newWords = (4L..5L).map { word(id = it, rank = it.toInt()) }
        (reviewWords + newWords).forEach { database.wordDao().insert(it) }
        val now = LocalDate.of(2026, 10, 1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        database.cardDao().insertAll(
            reviewWords.map { card(it.id, CardDirection.ES_TO_EN, dueDate = 1) },
        )
        val repository = StudySessionRepository(database, ZoneOffset.UTC)

        val session = repository.createDailySession(
            newWordLimit = 1,
            reviewLimit = 2,
            now = now,
        )

        val reviews = session.filter { it.kind == StudyCardKind.DUE_REVIEW }
        val newCards = session.filter { it.kind == StudyCardKind.NEW_WORD }
        assertEquals(2, reviews.size)
        assertEquals(setOf(4L), newCards.map { it.word.id }.toSet())
        assertEquals(setOf(CardDirection.ES_TO_EN, CardDirection.EN_TO_ES), newCards.map { it.card.direction }.toSet())
        assertEquals(4, session.size)
    }

    @Test
    fun skippedDaysDoNotIncreaseTheNextDaysNewWordLimit() = runBlocking {
        val words = (1L..8L).map { word(id = it, rank = it.toInt()) }
        words.forEach { database.wordDao().insert(it) }
        val repository = StudySessionRepository(database, ZoneOffset.UTC)
        val firstStudyDay = LocalDate.of(2026, 10, 1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val returnAfterSeveralSkippedDays = firstStudyDay + 4 * Sm2Scheduler.MILLIS_PER_DAY

        assertEquals(
            words.take(2),
            repository.introduceDailyNewWords(dailyLimit = 2, now = firstStudyDay),
        )
        assertEquals(
            emptyList<WordEntity>(),
            repository.introduceDailyNewWords(dailyLimit = 2, now = firstStudyDay + 1_000),
        )
        assertEquals(
            words.drop(2).take(2),
            repository.introduceDailyNewWords(dailyLimit = 2, now = returnAfterSeveralSkippedDays),
        )
    }

    @Test
    fun selectsOnlyCardsDueByTheRequestedTime() = runBlocking {
        val word = word(id = 1, rank = 1)
        database.wordDao().insert(word)
        database.cardDao().insertAll(
            listOf(
                card(word.id, CardDirection.ES_TO_EN, dueDate = 100),
                card(word.id, CardDirection.EN_TO_ES, dueDate = 300),
            ),
        )

        assertEquals(
            listOf(100L),
            database.cardDao().findDue(currentTime = 200, limit = 10).map { it.dueDate },
        )
    }

    @Test
    fun introducingAndGradingOneDirectionKeepsTheOtherScheduleIndependent() = runBlocking {
        val word = word(id = 1, rank = 1)
        database.wordDao().insert(word)
        val repository = CardRepository(database)
        val introducedAt = 1_000L

        repository.introduceWord(word.id, introducedAt)
        repository.introduceWord(word.id, introducedAt)

        val initialCards = database.cardDao().findForWord(word.id)
        assertEquals(2, initialCards.size)
        assertEquals(
            setOf(CardDirection.ES_TO_EN, CardDirection.EN_TO_ES),
            initialCards.map { it.direction }.toSet(),
        )
        assertTrue(initialCards.all { it.dueDate == introducedAt })

        val reviewedCard = initialCards.first { it.direction == CardDirection.ES_TO_EN }
        val untouchedCard = initialCards.first { it.direction == CardDirection.EN_TO_ES }
        val reviewedAt = introducedAt + 1
        val updatedCard = repository.gradeCard(reviewedCard.id, ReviewGrade.GOOD, reviewedAt)
        val reloadedReviewedCard = database.cardDao().findById(reviewedCard.id)
        val reloadedUntouchedCard = database.cardDao().findById(untouchedCard.id)

        assertEquals(updatedCard, reloadedReviewedCard)
        assertEquals(reviewedAt + Sm2Scheduler.MILLIS_PER_DAY, updatedCard.dueDate)
        assertEquals(untouchedCard, reloadedUntouchedCard)
    }

    @Test
    fun settingsCanBeReadAndReplaced() = runBlocking {
        val repository = SettingsRepository(database.settingsDao())
        assertEquals(StudySettings(), repository.load())

        repository.save(StudySettings(newWordsPerDay = 12, dailyReviewCap = 40))
        assertEquals(StudySettings(newWordsPerDay = 12, dailyReviewCap = 40), repository.load())
        repository.save(StudySettings(newWordsPerDay = 8, dailyReviewCap = 25))
        assertEquals(StudySettings(newWordsPerDay = 8, dailyReviewCap = 25), repository.load())
    }

    @Test
    fun settingsPersistAcrossDatabaseReopenAndControlNextSession() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "settings-reopen-test.db"
        context.deleteDatabase(databaseName)
        try {
            val firstOpen = Room.databaseBuilder(
                context,
                VocabularyDatabase::class.java,
                databaseName,
            ).build()
            try {
                SettingsRepository(firstOpen.settingsDao())
                    .save(StudySettings(newWordsPerDay = 1, dailyReviewCap = 1))
            } finally {
                firstOpen.close()
            }

            val reopened = Room.databaseBuilder(
                context,
                VocabularyDatabase::class.java,
                databaseName,
            ).build()
            try {
                assertEquals(
                    StudySettings(newWordsPerDay = 1, dailyReviewCap = 1),
                    SettingsRepository(reopened.settingsDao()).load(),
                )
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }

        val storedSettings = SettingsRepository(database.settingsDao())
        storedSettings.save(StudySettings(newWordsPerDay = 1, dailyReviewCap = 1))
        val reviewWord = word(id = 1, rank = 1)
        val firstNewWord = word(id = 2, rank = 2)
        val secondNewWord = word(id = 3, rank = 3)
        listOf(reviewWord, firstNewWord, secondNewWord).forEach { database.wordDao().insert(it) }
        database.cardDao().insertAll(listOf(card(reviewWord.id, CardDirection.ES_TO_EN, dueDate = 1)))

        val session = StudySessionRepository(database, ZoneOffset.UTC)
            .createDailySession(now = LocalDate.of(2026, 10, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())

        assertEquals(1, session.count { it.kind == StudyCardKind.DUE_REVIEW })
        assertEquals(2, session.count { it.kind == StudyCardKind.NEW_WORD })
        assertEquals(setOf(firstNewWord.id), session.filter { it.kind == StudyCardKind.NEW_WORD }
            .map { it.word.id }.toSet())
    }

    private fun word(id: Long, rank: Int) = WordEntity(
        id = id,
        rank = rank,
        spanishText = "palabra$id",
        normalizedWord = "palabra$id",
        frequency = 500 - rank,
        partOfSpeech = "noun",
        gloss = "word$id",
        exampleSentence = null,
        grammaticalGender = null,
    )

    private fun card(
        wordId: Long,
        direction: CardDirection,
        dueDate: Long = 0,
    ) = CardEntity(
        wordId = wordId,
        direction = direction,
        easeFactor = 2.5,
        intervalDays = 0,
        dueDate = dueDate,
        introducedDate = 0,
    )
}
