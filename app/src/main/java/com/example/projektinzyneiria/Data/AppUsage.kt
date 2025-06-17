package com.example.projektinzyneiria.Data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "app_usage")
data class AppUsage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    // przykładowo: "2025-06-17"
    @ColumnInfo(name = "date")
    val date: LocalDate,

    // czas w milisekundach
    @ColumnInfo(name = "usage_time_ms")
    val usageTimeMs: Long
)