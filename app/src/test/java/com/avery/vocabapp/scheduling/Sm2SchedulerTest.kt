package com.avery.vocabapp.scheduling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2SchedulerTest {
    @Test
    fun firstGoodReviewSchedulesOneDayLater() {
        val now = 1_000L
        val next = Sm2Scheduler.review(CardSchedule(), ReviewGrade.GOOD, now)

        assertEquals(1, next.repetitions)
        assertEquals(1, next.intervalDays)
        assertEquals(now + Sm2Scheduler.MILLIS_PER_DAY, next.dueDate)
        assertEquals(2.5, next.easeFactor, 0.0001)
    }

    @Test
    fun consecutiveGoodReviewsGrowTheInterval() {
        val first = Sm2Scheduler.review(CardSchedule(), ReviewGrade.GOOD, 0)
        val second = Sm2Scheduler.review(first, ReviewGrade.GOOD, first.dueDate)
        val third = Sm2Scheduler.review(second, ReviewGrade.GOOD, second.dueDate)

        assertEquals(1, first.intervalDays)
        assertEquals(6, second.intervalDays)
        assertTrue(third.intervalDays > second.intervalDays)
    }

    @Test
    fun againResetsRepetitionsAndSchedulesAOneDayRelearningInterval() {
        val now = 500L
        val current = CardSchedule(easeFactor = 2.5, repetitions = 4, intervalDays = 20)
        val next = Sm2Scheduler.review(current, ReviewGrade.AGAIN, now)

        assertEquals(0, next.repetitions)
        assertEquals(1, next.intervalDays)
        assertEquals(now + Sm2Scheduler.MILLIS_PER_DAY, next.dueDate)
        assertTrue(next.easeFactor < current.easeFactor)
        assertTrue(next.easeFactor >= Sm2Scheduler.MIN_EASE_FACTOR)
    }

    @Test
    fun easyProducesALongerIntervalThanGoodOnTheSameCard() {
        val current = CardSchedule(easeFactor = 2.5, repetitions = 2, intervalDays = 6)
        val good = Sm2Scheduler.review(current, ReviewGrade.GOOD, 0)
        val easy = Sm2Scheduler.review(current, ReviewGrade.EASY, 0)

        assertTrue(easy.intervalDays > good.intervalDays)
    }
}
