package com.example.projektinzyneiria.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.projektinzyneiria.R
import com.example.projektinzyneiria.UsageLimitData.AppLimitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import java.util.concurrent.TimeUnit

class LimitCheckWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        private const val TAG = "LimitCheckWorker"
        private const val CHANNEL_ID = "limit_exceeded"
        private const val PREFS_NAME = "limit_notifications"

        fun enqueueFirst(ctx: Context) = enqueueNext(ctx)
        fun enqueueNext(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<LimitCheckWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "limit_checker", ExistingWorkPolicy.REPLACE, req
            )
        }
    }

    private val limitRepo = AppLimitRepository.getInstance(applicationContext)
    private val prefs: SharedPreferences =
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val ctx = applicationContext
        // 1. Oblicz dzisiejszą datę
        val zone  = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(zone).date.toString()

        // 2. Pobierz limity i przygotuj lookup
        val limits   = limitRepo.getAllLimitsOnce()               // pobranie z DB
        val limitMap = limits.associateBy { it.packageName }
        val limitPkgs = limitMap.keys

        // 3. Pobierz surowe użycie od północy
        val rawUsage = getUsageSinceMidnight(ctx)
        Log.d(TAG, "Raw usage since midnight: $rawUsage")
        // 3a. Odfiltruj tylko aplikacje z limitami
        val usage = rawUsage.filterKeys { it in limitPkgs }
        Log.d(TAG, "Filtered usage: $usage")

        // 4. Sprawdź każdy limit i wyślij notyfikację jednokrotnie na dobę
        usage.forEach { (pkg, ms) ->
            val usedMin = ms / 60_000
            val limit   = limitMap[pkg]!!.dailyLimitMin
            val key     = "${pkg}@${today}"

            Log.d(TAG, "Checking $pkg: used $usedMin min, limit = $limit min")
            if (usedMin >= limit && !prefs.contains(key)) {
                Log.d(TAG, "Limit exceeded for $pkg: used $usedMin min, limit = $limit min")
                sendNotification(pkg, usedMin, limit)
                prefs.edit().putBoolean(key, true).apply()
                Log.d(TAG, "Notification sent for $pkg")
            }
        }

        // 5. Zaplanuj kolejne sprawdzenie za 1 minutę
        enqueueNext(ctx)

        Result.success()
    }

    /** Zwraca mapę <packageName, ms> od północy do teraz */
    private fun getUsageSinceMidnight(ctx: Context): Map<String, Long> {
        val usm  = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val zone = TimeZone.currentSystemDefault()
        val now  = System.currentTimeMillis()
        val start= Clock.System
            .now().toLocalDateTime(zone).date
            .atStartOfDayIn(zone)
            .toEpochMilliseconds()

        return usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
            .associate { it.packageName to it.totalTimeInForeground }
    }

//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    private fun sendNotification(pkg: String, usedMin: Long, limitMin: Int) {
//        val nm = NotificationManagerCompat.from(applicationContext)
//        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
//            val ch = NotificationChannel(
//                CHANNEL_ID,
//                "Limit exceeded",
//                NotificationManager.IMPORTANCE_HIGH
//            )
//            nm.createNotificationChannel(ch)
//        }
//        val text = "Użyto ${format(usedMin)} / dozwolone ${format(limitMin.toLong())}"
//        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle("Przekroczono limit!")
//            .setContentText(text)
//            .setAutoCancel(true)
//            .build()
//        nm.notify(pkg.hashCode(), notif)
//    }

    private fun format(min: Long): String = "${min / 60} h ${min % 60} m"

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun sendNotification(pkg: String, usedMin: Long, limitMin: Int) {
        val ctx = applicationContext
        // 1) Sprawdź uprawnienie (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Checking notification permission for Android 13+")
            if (ContextCompat.checkSelfPermission(

                    ctx, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
            Log.d(TAG, "Notification permission granted")
        }
        Log.d("LimitCheckWorker", "Sending notification for package: $pkg")
        // 2) Pobierz NotificationManager
        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // 3) Zbuduj powiadomienie
        val appName = getAppLabel(ctx, pkg)
        val text = "Użyto ${format(usedMin)} / dozwolone ${format(limitMin.toLong())}"
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)    // <–– must be white-on-transparent icon
            .setContentTitle("Przekroczono limit dla $appName!")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        Log.d(TAG, "Notification built: $notif")
        // 4) Wyślij
        notificationManager.notify(pkg.hashCode(), notif)
    }
}
fun getAppLabel(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        val ai = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(ai).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName // Zwraca packageName, jeśli nie znaleziono aplikacji
    }
}