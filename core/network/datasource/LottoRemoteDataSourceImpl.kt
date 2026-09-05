package com.example.lottoinsight.core.network.datasource

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.network.api.LottoApiService
import com.example.lottoinsight.core.network.model.RemoteDrawDto
import com.example.lottoinsight.core.network.model.RemoteDrawResponseValidator
import retrofit2.Response
import java.io.IOException

class LottoRemoteDataSourceImpl(
    private val apiService: LottoApiService
) : LottoRemoteDataSource {

    override suspend fun fetchDraw(drawNo: Int): AppResult<Draw> {
        if (drawNo <= 0) {
            return AppResult.Error(AppError.InvalidDraw(drawNo, "Invalid drawNo parameter: $drawNo"))
        }

        return try {
            val response = apiService.getDraw(drawNo)
            handleHttpResponse(response, drawNo)
        } catch (e: IOException) {
            AppResult.Error(AppError.NetworkUnavailable("Network I/O failure fetching draw $drawNo: ${e.localizedMessage}"))
        } catch (e: Exception) {
            AppResult.Error(AppError.ParseError("Unexpected exception fetching draw $drawNo: ${e.localizedMessage}"))
        }
    }

    private fun handleHttpResponse(response: Response<RemoteDrawDto>, drawNo: Int): AppResult<Draw> {
        if (!response.isSuccessful) {
            return AppResult.Error(
                AppError.HttpError(response.code(), response.message())
            )
        }

        val dto = response.body()
            ?: return AppResult.Error(AppError.ParseError("Null response body received for draw $drawNo"))

        return validateAndMapDto(dto)
    }

    private fun validateAndMapDto(dto: RemoteDrawDto): AppResult<Draw> {
        val validationError = RemoteDrawResponseValidator.validate(dto)
        if (validationError != null) {
            return AppResult.Error(validationError)
        }

        return AppResult.Success(dto.toDomain())
    }
}
