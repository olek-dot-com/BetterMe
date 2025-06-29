package com.example.projektinzyneiria.Data

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate


class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)
}
