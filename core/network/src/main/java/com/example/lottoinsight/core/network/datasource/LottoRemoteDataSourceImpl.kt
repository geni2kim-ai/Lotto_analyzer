package com.example.lottoinsight.core.network.datasource

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.network.api.LottoApiService
import com.example.lottoinsight.core.network.model.NewRemoteDrawDto
import com.example.lottoinsight.core.network.model.NewRemoteDrawResponseValidator
import com.example.lottoinsight.core.network.model.RemoteDrawDto
import com.example.lottoinsight.core.network.model.RemoteDrawResponseValidator
import com.example.lottoinsight.core.network.model.toDomain
import java.util.concurrent.CancellationException
import java.io.IOException
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

    override suspend fun fetchAllDraws(existingMaxDrawNo: Int?): AppResult<List<Draw>> {
        val primaryResult = try {
            val response = apiService.getDraws(
                drawQuery = "all",
                cacheBuster = System.currentTimeMillis()
            )
            if (!response.isSuccessful) {
                AppResult.Error(AppError.HttpError(response.code()))
            } else {
                val items = response.body()?.data?.list.orEmpty()
                mapNewDraws(items)
            }
        } catch (e: IOException) {
            AppResult.Error(AppError.NetworkUnavailable)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(AppError.ParseError("Unexpected response while fetching all draws"))
        }

        if (primaryResult is AppResult.Success) return primaryResult

        val latestResult = fetchLatestDrawNo()
        val latest = (latestResult as? AppResult.Success)?.data
            ?: return primaryResult
        val start = (existingMaxDrawNo ?: 0) + 1
        if (start > latest) return AppResult.Success(emptyList())
        return fetchDrawRange(start, latest)
    }

    override suspend fun fetchDrawRange(startDrawNo: Int, endDrawNo: Int): AppResult<List<Draw>> {
        if (startDrawNo <= 0 || endDrawNo < startDrawNo) {
            return AppResult.Success(emptyList())
        }

        val draws = mutableListOf<Draw>()
        for (drawNo in startDrawNo..endDrawNo) {
            when (val result = fetchDraw(drawNo)) {
                is AppResult.Success -> draws += result.data
                is AppResult.Error -> return AppResult.Error(result.error)
                is AppResult.Loading -> return AppResult.Loading
            }
        }
        return AppResult.Success(draws.distinctBy { it.drawNo }.sortedBy { it.drawNo })
    }

    override suspend fun fetchLatestDrawNo(): AppResult<Int> {
        return try {
            val response = apiService.getResultPage()
            if (!response.isSuccessful) return AppResult.Error(AppError.HttpError(response.code()))
            val html = response.body().orEmpty()
            val candidates = listOf(
                Regex("""opt_val[^0-9]{0,50}([0-9]{1,5})"""),
                Regex("""d-trigger_txt"\)\.text\(\"(\\d{1,5})\"""")
            ).flatMap { pattern ->
                pattern.findAll(html).mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }.toList()
            }
            val latest = candidates.maxOrNull()
                ?: return AppResult.Error(AppError.ParseError("Latest draw number was not found"))
            if (latest <= 0) AppResult.Error(AppError.ParseError("Latest draw number was invalid"))
            else AppResult.Success(latest)
        } catch (e: IOException) {
            AppResult.Error(AppError.NetworkUnavailable)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            AppResult.Error(AppError.ParseError("Unexpected response while finding latest draw"))
        }
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            AppResult.Error(AppError.ParseError("Unexpected legacy response while fetching draw $drawNo"))
        }
    }

    private fun mapNewDraws(items: List<NewRemoteDrawDto>): AppResult<List<Draw>> {
        if (items.isEmpty()) {
            return AppResult.Error(AppError.ParseError("No draw data was returned by the new API"))
        }

        val drawsByNumber = linkedMapOf<Int, Draw>()
        for (item in items) {
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
        }

        return AppResult.Success(drawsByNumber.values.sortedBy { it.drawNo })
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
            return AppResult.Error(
                AppError.HttpError(response.code())
            )
        }

        val dto = response.body()
            ?: return AppResult.Error(AppError.ParseError("Null response body received for draw $drawNo"))

        return validateAndMapDto(dto, drawNo)
    }

    private fun validateAndMapDto(dto: RemoteDrawDto, expectedDrawNo: Int): AppResult<Draw> {
        val validationError = RemoteDrawResponseValidator.validate(dto, expectedDrawNo)
        if (validationError != null) {
            return AppResult.Error(validationError)
        }

        return AppResult.Success(dto.toDomain())
    }
}
