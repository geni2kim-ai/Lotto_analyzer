package com.example.lottoinsight.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lottoinsight.core.database.entity.DrawEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraws(draws: List<DrawEntity>)

    @Query("SELECT COUNT(*) FROM draws")
    suspend fun getDrawCount(): Int

    @Query("SELECT MAX(drawNo) FROM draws")
    suspend fun getLatestDrawNo(): Int?

    @Query("SELECT * FROM draws ORDER BY drawNo DESC LIMIT 1")
    fun observeLatestDraw(): Flow<DrawEntity?>

    @Query("SELECT * FROM draws ORDER BY drawNo ASC")
    fun observeAllDraws(): Flow<List<DrawEntity>>

    @Query("SELECT * FROM draws ORDER BY drawNo DESC LIMIT :recentN")
    suspend fun getRecentDraws(recentN: Int): List<DrawEntity>

    @Query("SELECT * FROM draws WHERE drawNo = :drawNo")
    suspend fun getDrawByNo(drawNo: Int): DrawEntity?
}
