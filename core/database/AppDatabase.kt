package com.example.lottoinsight.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lottoinsight.core.database.converter.ListIntConverter
import com.example.lottoinsight.core.database.dao.AnalysisDao
import com.example.lottoinsight.core.database.dao.DrawDao
import com.example.lottoinsight.core.database.dao.ExpectedValueDao
import com.example.lottoinsight.core.database.entity.AnalysisGameEntity
import com.example.lottoinsight.core.database.entity.AnalysisRunEntity
import com.example.lottoinsight.core.database.entity.DrawEntity
import com.example.lottoinsight.core.database.entity.ExpectedValueNumberEntity
import com.example.lottoinsight.core.database.entity.ExpectedValueRunEntity

@Database(
    entities = [
        DrawEntity::class,
        AnalysisRunEntity::class,
        AnalysisGameEntity::class,
        ExpectedValueRunEntity::class,
        ExpectedValueNumberEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(ListIntConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun drawDao(): DrawDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun expectedValueDao(): ExpectedValueDao

    companion object {
        const val DATABASE_NAME = "lotto_insight.db"
    }
}
