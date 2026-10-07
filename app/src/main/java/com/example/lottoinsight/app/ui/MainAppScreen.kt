package com.example.lottoinsight.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.lottoinsight.feature.analysis.AnalysisViewModel
import com.example.lottoinsight.feature.history.HistoryViewModel
import com.example.lottoinsight.feature.statistics.DatabaseViewModel
import com.example.lottoinsight.feature.statistics.StatisticsViewModel
import com.example.lottoinsight.feature.ui.analysis.AnalysisScreen
import com.example.lottoinsight.feature.ui.database.DatabaseScreen
import com.example.lottoinsight.feature.ui.history.HistoryScreen
import com.example.lottoinsight.feature.ui.statistics.StatisticsScreen

private enum class AppTab(val title: String) {
    ANALYSIS("번호 분석"),
    DATABASE("당첨번호 DB"),
    STATISTICS("기대값·통계"),
    HISTORY("분석 기록")
}

@Composable
fun MainAppScreen(
    analysisViewModel: AnalysisViewModel,
    databaseViewModel: DatabaseViewModel,
    historyViewModel: HistoryViewModel,
    statisticsViewModel: StatisticsViewModel
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.ANALYSIS) }

    Scaffold(
        bottomBar = {
            MainBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                AppTab.ANALYSIS -> AnalysisScreen(viewModel = analysisViewModel)
                AppTab.DATABASE -> DatabaseScreen(viewModel = databaseViewModel)
                AppTab.STATISTICS -> StatisticsScreen(viewModel = statisticsViewModel)
                AppTab.HISTORY -> HistoryScreen(viewModel = historyViewModel)
            }
        }
    }
}

@Composable
private fun MainBottomNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == AppTab.ANALYSIS,
            onClick = { onTabSelected(AppTab.ANALYSIS) },
            icon = { Icon(Icons.Default.Analytics, contentDescription = "Analysis") },
            label = { Text(AppTab.ANALYSIS.title) }
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.DATABASE,
            onClick = { onTabSelected(AppTab.DATABASE) },
            icon = { Icon(Icons.Default.Storage, contentDescription = "Database") },
            label = { Text(AppTab.DATABASE.title) }
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.STATISTICS,
            onClick = { onTabSelected(AppTab.STATISTICS) },
            icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Statistics") },
            label = { Text(AppTab.STATISTICS.title) }
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.HISTORY,
            onClick = { onTabSelected(AppTab.HISTORY) },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text(AppTab.HISTORY.title) }
        )
    }
}
