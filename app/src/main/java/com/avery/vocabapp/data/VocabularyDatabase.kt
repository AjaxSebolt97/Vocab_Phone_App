package com.avery.vocabapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

@Database(
    entities = [WordEntity::class, CardEntity::class, SettingsEntity::class],
    version = 4,
)
@TypeConverters(CardDirectionConverter::class)
abstract class VocabularyDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun cardDao(): CardDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val ASSET_PATH = "es_vocabulary.sqlite"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cards (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        word_id INTEGER NOT NULL,
                        direction TEXT NOT NULL,
                        ease_factor REAL NOT NULL,
                        interval_days INTEGER NOT NULL,
                        due_date INTEGER NOT NULL,
                        introduced_date INTEGER NOT NULL,
                        FOREIGN KEY(word_id) REFERENCES words(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cards_word_id ON cards(word_id)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_cards_word_id_direction " +
                        "ON cards(word_id, direction)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS settings (
                        id INTEGER NOT NULL PRIMARY KEY,
                        new_words_per_day INTEGER NOT NULL,
                        daily_review_cap INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE cards ADD COLUMN repetitions INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        internal fun migration3To4(context: Context): Migration =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    val assetFile = File.createTempFile(
                        "vocab-gloss-migration-",
                        ".sqlite",
                        context.cacheDir,
                    )
                    try {
                        context.assets.open(ASSET_PATH).use { input ->
                            assetFile.outputStream().use { output -> input.copyTo(output) }
                        }

                        val assetDatabase = SQLiteDatabase.openDatabase(
                            assetFile.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READONLY,
                        )
                        try {
                            val expectedWordCount = db.query("SELECT COUNT(*) FROM words").use { cursor ->
                                if (!cursor.moveToFirst()) {
                                    throw IllegalStateException("Could not count existing vocabulary words")
                                }
                                cursor.getInt(0)
                            }
                            var updatedWordCount = 0
                            val updateGloss = db.compileStatement(
                                "UPDATE words SET gloss = ? WHERE id = ?",
                            )
                            assetDatabase.rawQuery("SELECT id, gloss FROM words", null).use { cursor ->
                                while (cursor.moveToNext()) {
                                    updateGloss.clearBindings()
                                    updateGloss.bindString(1, cursor.getString(1))
                                    updateGloss.bindLong(2, cursor.getLong(0))
                                    if (updateGloss.executeUpdateDelete() == 1) {
                                        updatedWordCount += 1
                                    }
                                }
                            }
                            if (updatedWordCount != expectedWordCount) {
                                throw IllegalStateException(
                                    "Updated $updatedWordCount of $expectedWordCount existing vocabulary glosses",
                                )
                            }

                            db.execSQL("DELETE FROM dataset_sources")
                            val insertSource = db.compileStatement(
                                """
                                INSERT INTO dataset_sources (source_name, source_url, license, attribution)
                                VALUES (?, ?, ?, ?)
                                """.trimIndent(),
                            )
                            assetDatabase.rawQuery(
                                "SELECT source_name, source_url, license, attribution FROM dataset_sources",
                                null,
                            ).use { cursor ->
                                while (cursor.moveToNext()) {
                                    insertSource.clearBindings()
                                    insertSource.bindString(1, cursor.getString(0))
                                    insertSource.bindString(2, cursor.getString(1))
                                    insertSource.bindString(3, cursor.getString(2))
                                    insertSource.bindString(4, cursor.getString(3))
                                    if (insertSource.executeInsert() == -1L) {
                                        throw IllegalStateException(
                                            "Could not update vocabulary source attribution",
                                        )
                                    }
                                }
                            }
                        } finally {
                            assetDatabase.close()
                        }
                    } finally {
                        assetFile.delete()
                    }
                }
            }

        @Volatile
        private var instance: VocabularyDatabase? = null

        fun getInstance(context: Context): VocabularyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VocabularyDatabase::class.java,
                    "vocabulary.db",
                )
                    .createFromAsset(ASSET_PATH)
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        migration3To4(context.applicationContext),
                    )
                    .build()
                    .also { instance = it }
            }
    }
}
