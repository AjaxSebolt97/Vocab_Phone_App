package com.avery.vocabapp.data

import androidx.room.TypeConverter

class CardDirectionConverter {
    @TypeConverter
    fun toStoredValue(direction: CardDirection): String = direction.name

    @TypeConverter
    fun fromStoredValue(value: String): CardDirection = CardDirection.valueOf(value)
}
