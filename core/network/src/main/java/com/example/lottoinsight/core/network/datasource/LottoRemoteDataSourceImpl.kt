package com.example.lottoinsight.core.network.datasource

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.network.api.LottoApiService
import com.example.lottoinsight.core.network.model.LegacyRemoteDrawResponseValidator
import com.example.lottoinsight.core.network.model.NewRemoteDrawDto
import com.example.lottoinsight.core.network.model.NewRemoteDrawResponseValidator
import com.example.lottoinsight.core.network.model.RemoteDrawDto
import com.example.lottoinsight.core.network.model.toDomain
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.Response

class LottoRemoteDataSourceImpl(
    private val apiService: LottoApiService
) : LottoRemoteDataSource {

    override suspend fun fetchDraw(drawNo: Int): AppResult<Draw> {
        if (drawNo <= 0) {
            return AppResult.Error(AppError.InvalidDraw(drawNo, "Invalid drawNo parameter: $drawNo"))
        }

        val newApiResult = fetchFromNewApi(drawNo)
        if (newApiResult is AppResult.Success) {
            return newApiResult
        }

        val legacyResult = fetchFromLegacyApi(drawNo)
        return if (legacyResult is AppResult.Success) legacyResult else newApiResult
    }

    override suspend fun fetchAllDraws(
        existingMaxDrawNo: Int?,
        onProgress: ((completed: Int, total: Int) -> Unit)?
    ): AppResult<DrawFetchReport> {
        val primaryResult = try {
            val response = apiService.getDraws(
                drawQuery = "all",
                cacheBuster = System.currentTimeMillis()
            )
            if (!response.isSuccessful) {
                AppResult.Error(AppError.HttpError(response.code()))
            } else {
                val items = response.body()?.data?.list.orEmpty()
                mapNewDraws(items, onProgress)
            }
        } catch (e: IOException) {
            AppResult.Error(AppError.NetworkUnavailable)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            AppResult.Error(AppError.ParseError("Unexpected response while fetching all draws"))
        }

        if (primaryResult is AppResult.Success) return primaryResult

        val latestResult = fetchLatestDrawNo(existingMaxDrawNo)
        val latest = (latestResult as? AppResult.Success)?.data
            ?: return primaryResult
        val start = (existingMaxDrawNo ?: 0) + 1
        if (start > latest) {
            return AppResult.Success(DrawFetchReport(emptyList(), emptyList()))
        }
        return fetchDrawRange(start, latest, onProgress)
    }

    override suspend fun fetchDrawRange(
        startDrawNo: Int,
        endDrawNo: Int,
        onProgress: ((completed: Int, total: Int) -> Unit)?
    ): AppResult<DrawFetchReport> {
        if (startDrawNo <= 0 || endDrawNo < startDrawNo) {
            return AppResult.Success(DrawFetchReport(emptyList(), emptyList()))
        }
        return fetchDraws((startDrawNo..endDrawNo).toList(), onProgress)
    }

    override suspend fun fetchDraws(
        drawNos: List<Int>,
        onProgress: ((completed: Int, total: Int) -> Unit)?
    ): AppResult<DrawFetchReport> {
        val targets = drawNos.filter { it > 0 }.distinct().sorted()
        if (targets.isEmpty()) {
            return AppResult.Success(DrawFetchReport(emptyList(), emptyList()))
        }

        val total = targets.size
        val completed = AtomicInteger(0)
        val semaphore = Semaphore(MAX_CONCURRENT_FALLBACK_REQUESTS)

        val results = coroutineScope {
            targets.map { drawNo ->
                async {
                    val result = semaphore.withPermit { fetchDraw(drawNo) }
                    val done = completed.incrementAndGet()
                    runCatching { onProgress?.invoke(done, total) }
                    drawNo to result
                }
            }.awaitAll()
        }.sortedBy { it.first }

        val successful = results.mapNotNull { (_, result) ->
            (result as? AppResult.Success)?.data
        }.distinctBy { it.drawNo }.sortedBy { it.drawNo }

        val failedDrawNos = results.mapNotNull { (drawNo, result) ->
            if (result is AppResult.Success) null else drawNo
        }

        return AppResult.Success(
            DrawFetchReport(
                successful = successful,
                failedDrawNos = failedDrawNos
            )
        )
    }

    override suspend fun fetchLatestDrawNo(existingMaxDrawNo: Int?): AppResult<Int> {
        val officialLatest = try {
            val response = apiService.getDraws(
                drawQuery = "all",
                cacheBuster = System.currentTimeMillis()
            )
            if (response.isSuccessful) {
                response.body()?.data?.list.orEmpty()
                    .mapNotNull { it.ltEpsd }
                    .filter { isOfficialLatestPlausible(it, existingMaxDrawNo) }
                    .maxOrNull()
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        if (officialLatest != null) {
            return AppResult.Success(officialLatest)
        }

        return try {
            val response = apiService.getResultPage()
            if (!response.isSuccessful) return AppResult.Error(AppError.HttpError(response.code()))
            val html = response.body().orEmpty()
            val candidates = listOf(
                Regex("""opt_val[^0-9]{0,50}([0-9]{1,5})"""),
                Regex("""d-trigger_txt.{0,80}?([0-9]{1,5})""")
            ).flatMap { pattern ->
                pattern.findAll(html)
                    .mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
                    .toList()
            }
            val latest = candidates
                .filter { isHtmlLatestPlausible(it, existingMaxDrawNo) }
                .maxOrNull()
                ?: return AppResult.Error(AppError.ParseError("Latest draw number was not found in a plausible range"))
            AppResult.Success(latest)
        } catch (e: IOException) {
            AppResult.Error(AppError.NetworkUnavailable)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            AppResult.Error(AppError.ParseError("Unexpected response while finding latest draw"))
        }
    }

    private fun isOfficialLatestPlausible(candidate: Int, existingMaxDrawNo: Int?): Boolean {
        if (candidate !in 1..MAX_PLAUSIBLE_DRAW_NO) return false
        val existing = existingMaxDrawNo?.takeIf { it > 0 } ?: return true
        return candidate >= existing
    }

    private fun isHtmlLatestPlausible(candidate: Int, existingMaxDrawNo: Int?): Boolean {
        if (!isOfficialLatestPlausible(candidate, existingMaxDrawNo)) return false
        val existing = existingMaxDrawNo?.takeIf { it > 0 } ?: return true
        return candidate - existing <= MAX_REASONABLE_HTML_SYNC_GAP
    }

    private suspend fun fetchFromNewApi(drawNo: Int): AppResult<Draw> {
        return try {
            val response = apiService.getDraws(
                drawQuery = drawNo.toString(),
                cacheBuster = System.currentTimeMillis()
            )
            if (!response.isSuccessful) {
                return AppResult.Error(AppError.HttpError(response.code()))
            }

            val items = response.body()?.data?.list.orEmpty()
            val matchingItem = items.firstOrNull { it.ltEpsd == drawNo }
                ?: return AppResult.Error(AppError.InvalidDraw(drawNo, "Requested draw was not returned"))
            mapNewDraw(matchingItem, drawNo)
        } catch (e: IOException) {
            AppResult.Error(AppError.NetworkUnavailable)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            AppResult.Error(AppError.ParseError("Unexpected response while fetching draw $drawNo"))
        }
    }

    private suspend fun fetchFromLegacyApi(drawNo: Int): AppResult<Draw> {
        return try {
            val response = apiService.getDraw(drawNo)
            handleHttpResponse(response, drawNo)
        } catch (e: IOException) {
            AppResult.Error(AppError.NetworkUnavailable)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            AppResult.Error(AppError.ParseError("Unexpected legacy response while fetching draw $drawNo"))
        }
    }

    private fun mapNewDraws(
        items: List<NewRemoteDrawDto>,
        onProgress: ((completed: Int, total: Int) -> Unit)?
    ): AppResult<DrawFetchReport> {
        if (items.isEmpty()) {
            return AppResult.Error(AppError.ParseError("No draw data was returned by the new API"))
        }

        val drawsByNumber = linkedMapOf<Int, Draw>()
        val total = items.size
        for ((index, item) in items.withIndex()) {
            val drawNo = item.ltEpsd ?: 0
            val validationError = NewRemoteDrawResponseValidator.validate(item)
            if (validationError != null) {
                return AppResult.Error(validationError)
            }
            try {
                val draw = item.toDomain()
                drawsByNumber[draw.drawNo] = draw
            } catch (_: IllegalArgumentException) {
                return AppResult.Error(AppError.ParseError("Invalid draw payload for draw $drawNo"))
            }
            runCatching { onProgress?.invoke(index + 1, total) }
        }

        return AppResult.Success(
            DrawFetchReport(
                successful = drawsByNumber.values.sortedBy { it.drawNo },
                failedDrawNos = emptyList()
            )
        )
    }

    private fun mapNewDraw(item: NewRemoteDrawDto, expectedDrawNo: Int): AppResult<Draw> {
        val validationError = NewRemoteDrawResponseValidator.validate(item, expectedDrawNo)
        if (validationError != null) {
            return AppResult.Error(validationError)
        }
        return try {
            AppResult.Success(item.toDomain())
        } catch (_: IllegalArgumentException) {
            AppResult.Error(AppError.ParseError("Invalid draw payload for draw $expectedDrawNo"))
        }
    }

    private fun handleHttpResponse(response: Response<RemoteDrawDto>, drawNo: Int): AppResult<Draw> {
        if (!response.isSuccessful) {
            return AppResult.Error(AppError.HttpError(response.code()))
        }

        val dto = response.body()
            ?: return AppResult.Error(AppError.ParseError("Null response body received for draw $drawNo"))

        return validateAndMapDto(dto, drawNo)
    }

    private fun validateAndMapDto(dto: RemoteDrawDto, expectedDrawNo: Int): AppResult<Draw> {
        val validationError = LegacyRemoteDrawResponseValidator.validate(dto, expectedDrawNo)
        if (validationError != null) {
            return AppResult.Error(validationError)
        }

        return AppResult.Success(dto.toDomain())
    }

    private companion object {
        const val MAX_CONCURRENT_FALLBACK_REQUESTS = 6
        const val MAX_REASONABLE_HTML_SYNC_GAP = 200
        const val MAX_PLAUSIBLE_DRAW_NO = 5000
    }
}
