package com.example.lottoinsight.core.data.repository

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.database.dao.DrawDao
import com.example.lottoinsight.core.database.entity.DrawEntity
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.network.datasource.DrawFetchReport
import com.example.lottoinsight.core.network.datasource.LottoRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LottoRepositoryImpl(
    private val drawDao: DrawDao,
    private val remoteDataSource: LottoRemoteDataSource
) : LottoRepository {

    override fun observeAllDraws(): Flow<List<Draw>> =
        drawDao.observeAllDraws().map { list -> list.map { it.toDomain() } }

    override fun observeLatestDraw(): Flow<Draw?> =
        drawDao.observeLatestDraw().map { it?.toDomain() }

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
        val report = remoteDataSource.fetchDrawRange(
            startDrawNo = latestLocalDrawNo + 1,
            endDrawNo = latestLocalDrawNo + fetchCount
        )
        persistSuccessful(report)
        return AppResult.Success(report.successful.size)
    }

    override suspend fun syncDraws(
        onProgress: ((completed: Int, total: Int) -> Unit)?
    ): AppResult<DrawSyncReport> {
        val existingCount = drawDao.getDrawCount()
        return when (
            val fetchResult = remoteDataSource.fetchAllDraws(
                existingMaxDrawNo = drawDao.getLatestDrawNo(),
                onProgress = onProgress
            )
        ) {
            is AppResult.Success -> {
                val report = fetchResult.data
                persistSuccessful(report)

                if (report.successful.isEmpty() && report.failedDrawNos.isEmpty() && existingCount == 0) {
                    AppResult.Error(AppError.InsufficientData)
                } else {
                    AppResult.Success(report.toSyncReport())
                }
            }
            is AppResult.Error -> AppResult.Error(fetchResult.error)
            is AppResult.Loading -> AppResult.Loading
        }
    }

    override suspend fun retryDraws(
        drawNos: List<Int>,
        onProgress: ((completed: Int, total: Int) -> Unit)?
    ): AppResult<DrawSyncReport> {
        val targets = drawNos.filter { it > 0 }.distinct().sorted()
        if (targets.isEmpty()) {
            return AppResult.Success(DrawSyncReport(0, emptyList()))
        }

        val report = remoteDataSource.fetchDraws(targets, onProgress)
        persistSuccessful(report)
        return AppResult.Success(report.toSyncReport())
    }

    private suspend fun persistSuccessful(report: DrawFetchReport) {
        if (report.successful.isNotEmpty()) {
            drawDao.upsertDraws(report.successful.map(DrawEntity::fromDomain))
        }
    }

    private fun DrawFetchReport.toSyncReport(): DrawSyncReport =
        DrawSyncReport(
            successfulCount = successful.size,
            failedDrawNos = failedDrawNos
        )
}
