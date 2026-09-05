package com.example.lottoinsight.core.database.converter

import androidx.room.TypeConverter

class ListIntConverter {
    @TypeConverter
    fun fromListInt(list: List<Int>?): String {
        return list?.joinToString(separator = ",") ?: ""
    }

    @TypeConverter
    fun toListInt(data: String?): List<Int> {
        if (data.isNullOrEmpty()) return emptyList()
        return data.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }
}
