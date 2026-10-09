package com.avery.vocabapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CardDao {
    @Insert
    suspend fun insertAll(cards: List<CardEntity>)

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun findById(cardId: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE word_id = :wordId ORDER BY id")
    suspend fun findForWord(wordId: Long): List<CardEntity>

    @Query("SELECT * FROM cards WHERE due_date <= :currentTime ORDER BY due_date, id LIMIT :limit")
    suspend fun findDue(currentTime: Long, limit: Int): List<CardEntity>

    @Query(
        """
        SELECT COUNT(DISTINCT word_id) FROM cards
        WHERE introduced_date >= :dayStart AND introduced_date < :nextDayStart
        """,
    )
    suspend fun countIntroducedBetween(dayStart: Long, nextDayStart: Long): Int

    @Update
    suspend fun update(card: CardEntity)
}
