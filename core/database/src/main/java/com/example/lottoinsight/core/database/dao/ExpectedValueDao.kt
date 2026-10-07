package com.example.lottoinsight.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.lottoinsight.core.database.entity.ExpectedValueNumberEntity
import com.example.lottoinsight.core.database.entity.ExpectedValueRunEntity

@Dao
interface ExpectedValueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: ExpectedValueRunEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNumbers(numbers: List<ExpectedValueNumberEntity>)

    @Transaction
    suspend fun saveExpectedValueRun(run: ExpectedValueRunEntity, numbers: List<ExpectedValueNumberEntity>): Long {
        val runId = insertRun(run)
        val numbersWithRunId = numbers.map { it.copy(runId = runId) }
        insertNumbers(numbersWithRunId)
        return runId
    }

    @Query("SELECT * FROM expected_value_runs ORDER BY id DESC LIMIT 1")
    suspend fun getLatestRun(): ExpectedValueRunEntity?

    @Query("SELECT * FROM expected_value_runs WHERE recentN = :recentN ORDER BY id DESC LIMIT 1")
    suspend fun getLatestRunForRecentN(recentN: Int): ExpectedValueRunEntity?

    @Query("SELECT * FROM expected_value_numbers WHERE runId = :runId ORDER BY number ASC")
    suspend fun getNumbersForRun(runId: Long): List<ExpectedValueNumberEntity>

    @Query("DELETE FROM expected_value_runs WHERE id = :runId")
    suspend fun deleteRun(runId: Long)
}
