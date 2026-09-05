package com.example.lottoinsight.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_runs")
data class AnalysisRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long,
    val latestDrawNo: Int,
    val recentN: Int,
    val gameCount: Int,
    val weightFrequency: Int,
    val weightConsecutive: Int,
    val weightParity: Int,
    val usePrizeIndex: Boolean,
    val randomSeed: Long,
    val algorithmVersion: String,
    val dataStartDrawNo: Int,
    val dataEndDrawNo: Int
)
