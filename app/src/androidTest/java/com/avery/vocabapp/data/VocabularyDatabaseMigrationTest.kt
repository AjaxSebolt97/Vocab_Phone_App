package com.avery.vocabapp.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VocabularyDatabaseMigrationTest {
    @Test
    fun migration3To4UpdatesGlossesAndPreservesStudyProgress() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "vocabulary-migration-test.db"
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()
        context.deleteDatabase(databaseName)
        createVersion3Database(databaseFile)

        val database = Room.databaseBuilder(
            context,
            VocabularyDatabase::class.java,
            databaseName,
        )
            .addMigrations(VocabularyDatabase.migration3To4(context))
            .build()

        try {
            val word = database.wordDao().findById(DE_WORD_ID)
            assertNotNull(word)
            assertEquals("of; 's; used after the thing owned and before the owner", word?.gloss)
            assertNotEquals("Indica pertenencia.", word?.gloss)
            assertEquals(
                CardEntity(
                    id = 1,
                    wordId = DE_WORD_ID,
                    direction = CardDirection.ES_TO_EN,
                    easeFactor = 2.3,
                    repetitions = 4,
                    intervalDays = 12,
                    dueDate = 1_700_000_000_000,
                    introducedDate = 1_699_000_000_000,
                ),
                database.cardDao().findById(1),
            )
            assertEquals(
                SettingsEntity(newWordsPerDay = 7, dailyReviewCap = 35),
                database.settingsDao().findById(),
            )
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun createVersion3Database(databaseFile: java.io.File) {
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE words (
                    id INTEGER NOT NULL PRIMARY KEY,
                    rank INTEGER NOT NULL,
                    spanish_text TEXT NOT NULL,
                    normalized_word TEXT NOT NULL,
                    frequency INTEGER NOT NULL,
                    part_of_speech TEXT NOT NULL,
                    gloss TEXT NOT NULL,
                    example_sentence TEXT,
                    grammatical_gender TEXT
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE UNIQUE INDEX index_words_rank ON words(rank)")
            database.execSQL(
                "CREATE UNIQUE INDEX index_words_normalized_word ON words(normalized_word)",
            )
            database.execSQL(
                """
                CREATE TABLE cards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    word_id INTEGER NOT NULL,
                    direction TEXT NOT NULL,
                    ease_factor REAL NOT NULL,
                    repetitions INTEGER NOT NULL DEFAULT 0,
                    interval_days INTEGER NOT NULL,
                    due_date INTEGER NOT NULL,
                    introduced_date INTEGER NOT NULL,
                    FOREIGN KEY(word_id) REFERENCES words(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX index_cards_word_id ON cards(word_id)")
            database.execSQL(
                "CREATE UNIQUE INDEX index_cards_word_id_direction " +
                    "ON cards(word_id, direction)",
            )
            database.execSQL(
                """
                CREATE TABLE settings (
                    id INTEGER NOT NULL PRIMARY KEY,
                    new_words_per_day INTEGER NOT NULL,
                    daily_review_cap INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE dataset_sources (
                    source_name TEXT NOT NULL,
                    source_url TEXT NOT NULL,
                    license TEXT NOT NULL,
                    attribution TEXT NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO words (
                    id, rank, spanish_text, normalized_word, frequency,
                    part_of_speech, gloss, example_sentence, grammatical_gender
                ) VALUES (?, 1, 'de', 'de', 1000, 'preposition',
                    'Indica pertenencia.', NULL, NULL)
                """.trimIndent(),
                arrayOf(DE_WORD_ID),
            )
            database.execSQL(
                """
                INSERT INTO cards (
                    id, word_id, direction, ease_factor, repetitions, interval_days,
                    due_date, introduced_date
                ) VALUES (1, ?, 'ES_TO_EN', 2.3, 4, 12, 1700000000000, 1699000000000)
                """.trimIndent(),
                arrayOf(DE_WORD_ID),
            )
            database.execSQL(
                "INSERT INTO settings (id, new_words_per_day, daily_review_cap) VALUES (1, 7, 35)",
            )
            database.execSQL(
                "INSERT INTO dataset_sources VALUES ('old', 'old', 'old', 'old')",
            )
            database.execSQL(
                "CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            database.execSQL(
                "INSERT INTO room_master_table (id, identity_hash) VALUES (42, ?)",
                arrayOf(ROOM_IDENTITY_HASH),
            )
            database.version = 3
        }
    }

    private companion object {
        const val DE_WORD_ID = 1556633399361261400L
        const val ROOM_IDENTITY_HASH = "569f3e07b5029aec7e0e1964e8fd19b2"
    }
}
