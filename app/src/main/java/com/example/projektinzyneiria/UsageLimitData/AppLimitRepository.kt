package com.example.projektinzyneiria.UsageLimitData

import android.content.Context
import com.example.projektinzyneiria.Data.AppDatabase
import kotlinx.coroutines.flow.Flow

class AppLimitRepository private constructor(
    private val dao: AppLimitDao
) {
    /** Obserwuje wszystkie limity */
    fun getAllLimits(): Flow<List<AppLimit>> =
        dao.getAllLimits()

    /** Pobiera limit dla danej aplikacji (lub null) */
    suspend fun getLimit(pkg: String): AppLimit? =
        dao.getLimitForPackage(pkg)

    /** Ustawia (lub nadpisuje) dzienny limit */
    suspend fun setLimit(pkg: String, minutes: Int) {
        dao.upsertLimit(AppLimit(packageName = pkg, dailyLimitMin = minutes))
    }

    /** Usuwa limit dla danej aplikacji */
    suspend fun clearLimit(pkg: String) =
        dao.deleteLimit(pkg)

    companion object {
        @Volatile private var INSTANCE: AppLimitRepository? = null

        fun getInstance(context: Context): AppLimitRepository =
            INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                AppLimitRepository(db.appLimitDao()).also { INSTANCE = it }
            }
    }
}