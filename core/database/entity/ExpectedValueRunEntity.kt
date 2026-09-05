package com.example.lottoinsight.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expected_value_runs")
data class ExpectedValueRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long,
    val latestDrawNo: Int,
    val recentN: Int,
    val algorithmVersion: String
)
