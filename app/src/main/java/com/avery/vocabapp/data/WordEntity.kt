package com.avery.vocabapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [
        Index(value = ["rank"], unique = true, name = "index_words_rank"),
        Index(value = ["normalized_word"], unique = true, name = "index_words_normalized_word"),
    ],
)
data class WordEntity(
    @PrimaryKey val id: Long,
    val rank: Int,
    @ColumnInfo(name = "spanish_text") val spanishText: String,
    @ColumnInfo(name = "normalized_word") val normalizedWord: String,
    val frequency: Int,
    @ColumnInfo(name = "part_of_speech") val partOfSpeech: String,
    val gloss: String,
    @ColumnInfo(name = "example_sentence") val exampleSentence: String?,
    @ColumnInfo(name = "grammatical_gender") val grammaticalGender: String?,
)
