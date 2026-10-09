package com.avery.vocabapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "new_words_per_day") val newWordsPerDay: Int,
    @ColumnInfo(name = "daily_review_cap") val dailyReviewCap: Int,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
