package com.example.projektinzyneiria.UsageLimitData

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limit")
data class AppLimit(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    /** Dzienny limit w minutach */
    @ColumnInfo(name = "daily_limit_min")
    val dailyLimitMin: Int
)
