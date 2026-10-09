package com.avery.vocabapp.scheduling

import kotlin.math.roundToInt

enum class ReviewGrade(val quality: Int) {
    AGAIN(0),
    HARD(3),
    GOOD(4),
    EASY(5),
}

data class CardSchedule(
    val easeFactor: Double = 2.5,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val dueDate: Long = 0,
)

object Sm2Scheduler {
    const val MIN_EASE_FACTOR = 1.3
    const val MILLIS_PER_DAY = 86_400_000L

    fun review(
        current: CardSchedule,
        grade: ReviewGrade,
        reviewedAt: Long,
    ): CardSchedule {
        require(current.easeFactor.isFinite() && current.easeFactor >= MIN_EASE_FACTOR)
        require(current.repetitions >= 0)
        require(current.intervalDays >= 0)

        val qualityDifference = 5 - grade.quality
        val easeAdjustment =
            0.1 - qualityDifference * (0.08 + qualityDifference * 0.02)
        val nextEaseFactor = (current.easeFactor + easeAdjustment).coerceAtLeast(MIN_EASE_FACTOR)

        val nextRepetitions: Int
        val nextIntervalDays: Int
        if (grade.quality < 3) {
            nextRepetitions = 0
            nextIntervalDays = 1
        } else {
            nextRepetitions = current.repetitions + 1
            nextIntervalDays = when (current.repetitions) {
                0 -> 1
                1 -> 6
                else -> (current.intervalDays * nextEaseFactor).roundToInt().coerceAtLeast(1)
            }
        }

        val nextDueDate = Math.addExact(
            reviewedAt,
            nextIntervalDays.toLong() * MILLIS_PER_DAY,
        )
        return CardSchedule(
            easeFactor = nextEaseFactor,
            repetitions = nextRepetitions,
            intervalDays = nextIntervalDays,
            dueDate = nextDueDate,
        )
    }
}
