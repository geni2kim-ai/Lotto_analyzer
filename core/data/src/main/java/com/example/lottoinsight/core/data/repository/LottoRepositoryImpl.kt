package com.example.lottoinsight.core.data.repository

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.database.dao.DrawDao
import com.example.lottoinsight.core.database.entity.DrawEntity
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.network.datasource.LottoRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LottoRepositoryImpl(
    private val drawDao: DrawDao,
    private val remoteDataSource: LottoRemoteDataSource
) : LottoRepository {

    override fun observeAllDraws(): Flow<List<Draw>> {
        return drawDao.observeAllDraws().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeLatestDraw(): Flow<Draw?> {
        return drawDao.observeLatestDraw().map { it?.toDomain() }
    }

    override suspend fun getDrawByNo(drawNo: Int): AppResult<Draw> {
        if (drawNo <= 0) {
            return AppResult.Error(AppError.InvalidDraw(drawNo, "Invalid drawNo: $drawNo"))
        }

        val cachedEntity = drawDao.getDrawByNo(drawNo)
        if (cachedEntity != null) {
            return AppResult.Success(cachedEntity.toDomain())
        }

        return when (val remoteResult = remoteDataSource.fetchDraw(drawNo)) {
            is AppResult.Success -> {
                val entity = DrawEntity.fromDomain(remoteResult.data)
                drawDao.upsertDraws(listOf(entity))
                AppResult.Success(remoteResult.data)
            }
            is AppResult.Error -> remoteResult
            is AppResult.Loading -> AppResult.Loading
        }
    }

    override suspend fun fetchAndSaveLatestDraws(fetchCount: Int): AppResult<Int> {
        if (fetchCount <= 0) {
            return AppResult.Error(AppError.InsufficientData)
        }

        val latestLocalDrawNo = drawDao.getLatestDrawNo() ?: 0
        var savedCount = 0
        var firstError: AppError? = null

        for (i in 0 until fetchCount) {
            val targetDrawNo = latestLocalDrawNo + 1 + i
            when (val result = fetchAndPersistSingleDraw(targetDrawNo)) {
                is AppResult.Success -> savedCount++
                is AppResult.Error -> {
                    firstError = result.error
                    break
                }
                is AppResult.Loading -> return AppResult.Loading
            }
        }

        return if (savedCount == 0 && firstError != null) {
            AppResult.Error(firstError)
        } else {
            AppResult.Success(savedCount)
        }
    }

    private suspend fun fetchAndPersistSingleDraw(targetDrawNo: Int): AppResult<Unit> {
        return when (val result = remoteDataSource.fetchDraw(targetDrawNo)) {
            is AppResult.Success -> {
                val entity = DrawEntity.fromDomain(result.data)
                drawDao.upsertDraws(listOf(entity))
                AppResult.Success(Unit)
            }
            is AppResult.Error -> result
            is AppResult.Loading -> AppResult.Loading
        }
    }

    override suspend fun syncDraws(
        onProgress: ((completed: Int, total: Int) -> Unit)?
    ): AppResult<Unit> {
        val existingCount = drawDao.getDrawCount()
        return when (
            val fetchResult = remoteDataSource.fetchAllDraws(
                existingMaxDrawNo = drawDao.getLatestDrawNo(),
                onProgress = onProgress
            )
        ) {
            is AppResult.Success -> {
                if (fetchResult.data.isEmpty()) {
                    if (existingCount > 0) AppResult.Success(Unit) else AppResult.Error(AppError.InsufficientData)
                } else {
                    drawDao.upsertDraws(fetchResult.data.map(DrawEntity::fromDomain))
                    AppResult.Success(Unit)
                }
            }
            is AppResult.Error -> AppResult.Error(fetchResult.error)
            is AppResult.Loading -> AppResult.Loading
        }
    }
}
