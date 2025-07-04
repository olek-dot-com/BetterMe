package com.example.projektinzyneiria.Data

import android.content.Context
import android.util.Log
import kotlinx.datetime.LocalDate


// 1. Repozytorium z fabryką (singleton)
class AppUsageRepository private constructor(
    private val dao: AppUsageDao
) {

    suspend fun upsertUsage(usage: AppUsage) {
        //logging for debugging
        Log.d("Upserting usage: $usage", "AppUsageRepository")

        dao.upsert(usage)
    }

    fun getAllUsages() = dao.getAll()

    suspend fun getUsagesByDate(date: LocalDate) = dao.getByDate(date)

    suspend fun clearAllUsages() = dao.deleteAll()

    suspend fun deleteUsagesForPackage(packageName: String) =
        dao.deleteByPackageName(packageName)

    suspend fun deleteOlderThan(minDate: LocalDate) =
        dao.deleteOlderThan(minDate)

    suspend fun isAppMonitored(pkg: String): Boolean =
        dao.exists(pkg)
    suspend fun getAllUsagesOnce(): List<AppUsage> =
        dao.getAllOnce()

    companion object {
        @Volatile
        private var INSTANCE: AppUsageRepository? = null

        /**
         * Zwraca singleton repozytorium.
         * Używaj AppUsageRepository.getInstance(context)
         */
        fun getInstance(context: Context): AppUsageRepository =
            INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                AppUsageRepository(db.appUsageDao()).also { INSTANCE = it }
            }
    }
}
