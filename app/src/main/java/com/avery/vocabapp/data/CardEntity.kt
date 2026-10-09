package com.avery.vocabapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CardDirection {
    ES_TO_EN,
    EN_TO_ES,
}

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["word_id"]),
        Index(value = ["word_id", "direction"], unique = true),
    ],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long,
    val direction: CardDirection,
    @ColumnInfo(name = "ease_factor") val easeFactor: Double,
    @ColumnInfo(name = "repetitions", defaultValue = "0") val repetitions: Int = 0,
    @ColumnInfo(name = "interval_days") val intervalDays: Int,
    @ColumnInfo(name = "due_date") val dueDate: Long,
    @ColumnInfo(name = "introduced_date") val introducedDate: Long,
)
