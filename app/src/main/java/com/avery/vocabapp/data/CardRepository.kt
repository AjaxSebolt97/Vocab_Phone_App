package com.avery.vocabapp.data

import androidx.room.withTransaction
import com.avery.vocabapp.scheduling.CardSchedule
import com.avery.vocabapp.scheduling.ReviewGrade
import com.avery.vocabapp.scheduling.Sm2Scheduler

class CardRepository(private val database: VocabularyDatabase) {
    suspend fun introduceWord(wordId: Long, introducedAt: Long) {
        database.withTransaction {
            val existingDirections = database.cardDao()
                .findForWord(wordId)
                .mapTo(mutableSetOf()) { it.direction }
            val missingDirections = CardDirection.entries.filterNot(existingDirections::contains)

            if (missingDirections.isNotEmpty()) {
                database.cardDao().insertAll(
                    missingDirections.map { direction ->
                        CardEntity(
                            wordId = wordId,
                            direction = direction,
                            easeFactor = INITIAL_EASE_FACTOR,
                            intervalDays = 0,
                            dueDate = introducedAt,
                            introducedDate = introducedAt,
                        )
                    },
                )
            }
        }
    }

    suspend fun gradeCard(
        cardId: Long,
        grade: ReviewGrade,
        reviewedAt: Long,
    ): CardEntity = database.withTransaction {
        val card = requireNotNull(database.cardDao().findById(cardId)) {
            "Cannot grade missing card id=$cardId"
        }
        val nextSchedule = Sm2Scheduler.review(
            current = CardSchedule(
                easeFactor = card.easeFactor,
                repetitions = card.repetitions,
                intervalDays = card.intervalDays,
                dueDate = card.dueDate,
            ),
            grade = grade,
            reviewedAt = reviewedAt,
        )
        val updatedCard = card.copy(
            easeFactor = nextSchedule.easeFactor,
            repetitions = nextSchedule.repetitions,
            intervalDays = nextSchedule.intervalDays,
            dueDate = nextSchedule.dueDate,
        )
        database.cardDao().update(updatedCard)
        updatedCard
    }

    private companion object {
        const val INITIAL_EASE_FACTOR = 2.5
    }
}
