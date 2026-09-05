package com.example.lottoinsight.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.lottoinsight.core.database.entity.AnalysisGameEntity
import com.example.lottoinsight.core.database.entity.AnalysisRunEntity
import kotlinx.coroutines.flow.Flow

data class AnalysisRunWithGames(
    val run: AnalysisRunEntity,
    val games: List<AnalysisGameEntity>
)

@Dao
interface AnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysisRun(run: AnalysisRunEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysisGames(games: List<AnalysisGameEntity>)

    @Transaction
    suspend fun saveAnalysisRun(run: AnalysisRunEntity, games: List<AnalysisGameEntity>): Long {
        val runId = insertAnalysisRun(run)
        val gamesWithRunId = games.map { it.copy(analysisRunId = runId) }
        insertAnalysisGames(gamesWithRunId)
        return runId
    }

    @Query("SELECT * FROM analysis_runs ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentAnalysisRuns(limit: Int = 50): List<AnalysisRunEntity>

    @Query("SELECT * FROM analysis_games WHERE analysisRunId = :runId ORDER BY gameIndex ASC")
    suspend fun getGamesForRun(runId: Long): List<AnalysisGameEntity>

    @Query("SELECT * FROM analysis_runs ORDER BY id DESC")
    fun observeAnalysisRuns(): Flow<List<AnalysisRunEntity>>
}
