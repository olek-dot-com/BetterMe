package com.example.projektinzyneiria.UsageLimitData

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    /** Wstawia lub nadpisuje limit dla danej aplikacji */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimit(limit: AppLimit)

    /** Pobiera limit dla konkretnej paczki (lub null, jeśli nie ma wpisu) */
    @Query("SELECT * FROM app_limit WHERE package_name = :pkg")
    suspend fun getLimitForPackage(pkg: String): AppLimit?

    /** Wszystkie limity – do wyświetlenia na liście ekranowej */
    @Query("SELECT * FROM app_limit")
    fun getAllLimits(): Flow<List<AppLimit>>

    /** Usuwa limit dla danej aplikacji */
    @Query("DELETE FROM app_limit WHERE package_name = :pkg")
    suspend fun deleteLimit(pkg: String)
}
