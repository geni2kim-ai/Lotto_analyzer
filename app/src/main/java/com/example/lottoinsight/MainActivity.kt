package com.example.lottoinsight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()

        val retrofit = Retrofit.Builder()
            .baseUrl(LottoApiService.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(LottoApiService::class.java)
        val remoteDataSource = LottoRemoteDataSourceImpl(apiService)

        val lottoRepository = LottoRepositoryImpl(db.drawDao(), remoteDataSource)
        val analysisRepository = AnalysisRepositoryImpl(db.analysisDao())
        val expectedValueRepository = ExpectedValueRepositoryImpl(db.expectedValueDao())

        val analysisEngine = AnalysisEngineImpl()

        val analysisViewModel = AnalysisViewModel(lottoRepository, analysisRepository, analysisEngine)
        val databaseViewModel = DatabaseViewModel(lottoRepository)
        val historyViewModel = HistoryViewModel(analysisRepository)
        val statisticsViewModel = StatisticsViewModel(lottoRepository, expectedValueRepository)

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
