package com.personal.fittrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

    @Insert
    suspend fun insert(entry: FoodLogEntity): Long

    @Query("SELECT * FROM food_log WHERE epochDay = :epochDay ORDER BY epochMillis DESC")
    fun observeForDay(epochDay: Long): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_log WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY epochMillis DESC")
    fun observeForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_log ORDER BY epochMillis DESC")
    fun observeAll(): Flow<List<FoodLogEntity>>

    @Query("DELETE FROM food_log WHERE id = :id")
    suspend fun delete(id: Long)
}
