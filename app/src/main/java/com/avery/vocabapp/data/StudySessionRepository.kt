package com.avery.vocabapp.data

import androidx.room.withTransaction
import java.time.Instant
import java.time.ZoneId

class StudySessionRepository(
    private val database: VocabularyDatabase,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    private val cardRepository = CardRepository(database)
    private val settingsRepository = SettingsRepository(database.settingsDao())

    suspend fun introduceNextWords(limit: Int, introducedAt: Long): List<WordEntity> {
        require(limit >= 0) { "New-word limit must not be negative" }
        return database.withTransaction { selectAndIntroduce(limit, introducedAt) }
    }

    suspend fun introduceDailyNewWords(dailyLimit: Int, now: Long): List<WordEntity> {
        require(dailyLimit >= 0) { "Daily new-word limit must not be negative" }
        return database.withTransaction {
            selectDailyNewWords(dailyLimit, now)
        }
    }

    suspend fun selectDueReviews(reviewLimit: Int, now: Long): List<CardEntity> {
        require(reviewLimit >= 0) { "Daily review limit must not be negative" }
        return database.cardDao().findDue(currentTime = now, limit = reviewLimit)
    }

    suspend fun createDailySession(
        newWordLimit: Int,
        reviewLimit: Int,
        now: Long,
    ): List<StudySessionCard> {
        require(newWordLimit >= 0) { "Daily new-word limit must not be negative" }
        require(reviewLimit >= 0) { "Daily review limit must not be negative" }
        return database.withTransaction {
            val reviews = loadDueReviews(reviewLimit, now)
            val newWords = selectDailyNewWords(newWordLimit, now)
            val newCards = loadNewWordCards(newWords)
            interleave(reviews, newCards)
        }
    }

    suspend fun createDailySession(now: Long): List<StudySessionCard> {
        val settings = settingsRepository.load()
        return createDailySession(settings.newWordsPerDay, settings.dailyReviewCap, now)
    }

    suspend fun previewDailySession(now: Long): StudySessionPreview {
        val settings = settingsRepository.load()
        return previewDailySession(settings.newWordsPerDay, settings.dailyReviewCap, now)
    }

    suspend fun previewDailySession(
        newWordLimit: Int,
        reviewLimit: Int,
        now: Long,
    ): StudySessionPreview {
        require(newWordLimit >= 0) { "Daily new-word limit must not be negative" }
        require(reviewLimit >= 0) { "Daily review limit must not be negative" }
        return database.withTransaction {
            StudySessionPreview(
                dueReviews = loadDueReviews(reviewLimit, now),
                newWords = availableDailyNewWords(newWordLimit, now),
            )
        }
    }

    private suspend fun selectDailyNewWords(dailyLimit: Int, now: Long): List<WordEntity> {
        val words = availableDailyNewWords(dailyLimit, now)
        words.forEach { word ->
            cardRepository.introduceWord(word.id, now)
        }
        return words
    }

    private suspend fun availableDailyNewWords(dailyLimit: Int, now: Long): List<WordEntity> {
        val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nextDayStart = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val introducedToday = database.cardDao().countIntroducedBetween(dayStart, nextDayStart)
        val remainingSlots = (dailyLimit - introducedToday).coerceAtLeast(0)
        return database.wordDao().findNotIntroduced(remainingSlots)
    }

    private suspend fun selectAndIntroduce(limit: Int, introducedAt: Long): List<WordEntity> {
        val words = database.wordDao().findNotIntroduced(limit)
        words.forEach { word ->
            cardRepository.introduceWord(word.id, introducedAt)
        }
        return words
    }

    private suspend fun loadDueReviews(limit: Int, now: Long): List<StudySessionCard> =
        database.cardDao()
            .findDue(currentTime = now, limit = limit)
            .map { card ->
                StudySessionCard(
                    word = requireNotNull(database.wordDao().findById(card.wordId)) {
                        "Card ${card.id} refers to missing word ${card.wordId}"
                    },
                    card = card,
                    kind = StudyCardKind.DUE_REVIEW,
                )
            }

    private suspend fun loadNewWordCards(words: List<WordEntity>): List<StudySessionCard> =
        words.flatMap { word ->
            database.cardDao().findForWord(word.id).map { card ->
                StudySessionCard(word, card, StudyCardKind.NEW_WORD)
            }
        }

    private fun interleave(
        reviews: List<StudySessionCard>,
        newCards: List<StudySessionCard>,
    ): List<StudySessionCard> = buildList {
        var reviewIndex = 0
        var newCardIndex = 0
        while (reviewIndex < reviews.size || newCardIndex < newCards.size) {
            if (reviewIndex < reviews.size) add(reviews[reviewIndex++])
            if (newCardIndex < newCards.size) add(newCards[newCardIndex++])
        }
    }
}

enum class StudyCardKind {
    DUE_REVIEW,
    NEW_WORD,
}

data class StudySessionCard(
    val word: WordEntity,
    val card: CardEntity,
    val kind: StudyCardKind,
)

data class StudySessionPreview(
    val dueReviews: List<StudySessionCard>,
    val newWords: List<WordEntity>,
) {
    val totalCardCount: Int
        get() = dueReviews.size + newWords.size * CardDirection.entries.size
}
