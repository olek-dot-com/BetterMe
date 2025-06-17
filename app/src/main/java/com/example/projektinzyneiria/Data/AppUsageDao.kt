package com.example.projektinzyneiria.Data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: AppUsage)

    @Query("SELECT * FROM app_usage WHERE date = :day")
    suspend fun getByDate(day: LocalDate): List<AppUsage>

    @Query("SELECT * FROM app_usage ORDER BY date DESC")
    fun getAll(): Flow<List<AppUsage>>

    @Query("DELETE FROM app_usage WHERE date < :minDate")
    suspend fun deleteOlderThan(minDate: LocalDate)

    @Query("DELETE FROM app_usage")
    suspend fun deleteAll()

    @Query("SELECT EXISTS(SELECT 1 FROM app_usage WHERE package_name = :pkg)")
    suspend fun exists(pkg: String): Boolean
    /**
     * Usuwa wpisy, których packageName jest równy podanemu
     * @param packageName nazwa paczki do usunięcia
     */
    @Query("DELETE FROM app_usage WHERE package_name = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT * FROM app_usage")
    suspend fun getAllOnce(): List<AppUsage>
}
