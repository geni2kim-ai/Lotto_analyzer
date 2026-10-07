package com.example.lottoinsight

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.lottoinsight.app.ui.MainAppScreen
import com.example.lottoinsight.core.data.repository.AnalysisRepositoryImpl
import com.example.lottoinsight.core.data.repository.ExpectedValueRepositoryImpl
import com.example.lottoinsight.core.data.repository.LottoRepositoryImpl
import com.example.lottoinsight.core.database.AppDatabase
import com.example.lottoinsight.core.engine.AnalysisEngineImpl
import com.example.lottoinsight.core.network.api.LottoApiService
import com.example.lottoinsight.core.network.datasource.LottoRemoteDataSourceImpl
import com.example.lottoinsight.feature.analysis.AnalysisViewModel
import com.example.lottoinsight.feature.history.HistoryViewModel
import com.example.lottoinsight.feature.statistics.DatabaseViewModel
import com.example.lottoinsight.feature.statistics.StatisticsViewModel
import com.example.lottoinsight.feature.ui.theme.LottoInsightTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val provider = ViewModelProvider(
            this,
            LottoViewModelFactory(applicationContext)
        )
        val analysisViewModel = provider[AnalysisViewModel::class.java]
        val databaseViewModel = provider[DatabaseViewModel::class.java]
        val historyViewModel = provider[HistoryViewModel::class.java]
        val statisticsViewModel = provider[StatisticsViewModel::class.java]

        setContent {
            LottoInsightTheme {
                MainAppScreen(
                    analysisViewModel = analysisViewModel,
                    databaseViewModel = databaseViewModel,
                    historyViewModel = historyViewModel,
                    statisticsViewModel = statisticsViewModel
                )
            }
        }
    }
}

private class LottoViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    private val db by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    private val apiService by lazy {
        Retrofit.Builder()
            .baseUrl(LottoApiService.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LottoApiService::class.java)
    }

    private val remoteDataSource by lazy { LottoRemoteDataSourceImpl(apiService) }
    private val lottoRepository by lazy { LottoRepositoryImpl(db.drawDao(), remoteDataSource) }
    private val analysisRepository by lazy { AnalysisRepositoryImpl(db.analysisDao()) }
    private val expectedValueRepository by lazy { ExpectedValueRepositoryImpl(db.expectedValueDao()) }
    private val analysisEngine by lazy { AnalysisEngineImpl() }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AnalysisViewModel::class.java) ->
                AnalysisViewModel(lottoRepository, analysisRepository, analysisEngine) as T
            modelClass.isAssignableFrom(DatabaseViewModel::class.java) ->
                DatabaseViewModel(lottoRepository) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(analysisRepository) as T
            modelClass.isAssignableFrom(StatisticsViewModel::class.java) ->
                StatisticsViewModel(lottoRepository, expectedValueRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
