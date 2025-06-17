package com.example.projektinzyneiria.Data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate


@Entity(tableName = "app_usage", primaryKeys = ["package_name", "date"])
data class AppUsage(
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "date")
    val date: LocalDate,

    // czas w milisekundach
    @ColumnInfo(name = "usage_time_ms")
    val usageTimeMs: Long
)