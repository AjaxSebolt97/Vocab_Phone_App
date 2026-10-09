package com.avery.vocabapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WordDao {
    @Insert
    suspend fun insert(word: WordEntity)

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun findById(id: Long): WordEntity?

    @Query(
        """
        SELECT * FROM words
        WHERE NOT EXISTS (
            SELECT 1 FROM cards WHERE cards.word_id = words.id
        )
        ORDER BY rank ASC
        LIMIT :limit
        """,
    )
    suspend fun findNotIntroduced(limit: Int): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int
}
