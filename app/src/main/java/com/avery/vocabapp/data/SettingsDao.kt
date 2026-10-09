package com.avery.vocabapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = :id")
    suspend fun findById(id: Int = SettingsEntity.SINGLETON_ID): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: SettingsEntity)
}
