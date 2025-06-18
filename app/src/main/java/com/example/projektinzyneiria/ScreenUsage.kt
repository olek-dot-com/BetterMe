package com.example.projektinzyneiria

import android.app.AppOpsManager
import android.app.DatePickerDialog
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import com.example.projektinzyneiria.Data.AppUsage
import com.example.projektinzyneiria.Data.AppUsageRepository
import com.example.projektinzyneiria.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.example.projektinzyneiria.UsageLimitData.AppLimitRepository


@Composable
fun UsageScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = remember { AppUsageRepository.getInstance(context) }
    val repolimit = remember { AppLimitRepository.getInstance(context) }
    var selectedDate by remember {
        mutableStateOf(
            Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        )
    }
    val datePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate(year, month + 1, dayOfMonth)
            },
            selectedDate.year,
            selectedDate.monthNumber - 1,
            selectedDate.dayOfMonth
        )
    }

    // Subskrypcja monitorowanych aplikacji z bazy
    val dbUsages by repo.getAllUsages().collectAsState(initial = emptyList())
    val monitoredPackages = remember(dbUsages) { dbUsages.map { it.packageName }.toSet() }
    val dayUsages = remember(dbUsages, selectedDate) {
        dbUsages.filter { it.date == selectedDate }
    }

    val displayData = remember(dayUsages) {
        dayUsages
            .sortedByDescending { it.usageTimeMs }
            .map { it.packageName to it.usageTimeMs }
    }

    // Uprawnienia i UI state
    var hasUsagePermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var hasNotificationPermission by remember { mutableStateOf(hasNotificationPermission(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showAppSelection by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Launcher do ustawień użycia
    val usageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasUsagePermission = hasUsageStatsPermission(context)
    }

    // Launcher do uprawnień powiadomień
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, hasUsagePermission, selectedDate, monitoredPackages) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasUsagePermission) {
                Log.d("UsageScreen", "Refreshing app usage data for date: $selectedDate")
                // Za każdym razie wywołaj refresh
                coroutineScope.launch {
                    val usage7d = getAppUsageLast7Days(context)
                    monitoredPackages.forEach { pkg ->
                        usage7d[pkg]?.get(selectedDate)?.let { ms ->
                            repo.upsertUsage(
                                AppUsage(
                                    packageName = pkg,
                                    date        = selectedDate,
                                    usageTimeMs = ms
                                )
                            )
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Lista zainstalowanych pakietów
    val installedPackages = remember {
        context.packageManager.getInstalledApplications(0).map { it.packageName }
    }

    Column(
        modifier = modifier.padding(16.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sprawdzenie uprawnień
        when {
            !hasUsagePermission -> {
                // UI dla braku uprawnień do użycia
                Text(
                    "Usage Stats Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Grant usage access to monitor app usage.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    showPermissionDialog = true
                }) {
                    Text("Grant Usage Permission")
                }
                if (showPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showPermissionDialog = false },
                        title = { Text("Grant Usage Access") },
                        text = {
                            Text(
                                "Allow the app to access usage data in Settings."
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                showPermissionDialog = false
                                usageLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }) { Text("Open Settings") }
                        },
                        dismissButton = null
                    )
                }
            }
            !hasNotificationPermission -> {
                // UI dla braku uprawnień do powiadomień
                Text(
                    "Notification Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Grant notification permission to receive usage alerts.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch("android.permission.POST_NOTIFICATIONS")
                    } else {
                        // For older versions, notifications are granted by default
                        hasNotificationPermission = true
                    }
                }) {
                    Text("Grant Notification Permission")
                }
            }
            else -> {
                // Główna lista monitorowanych aplikacji (gdy oba uprawnienia są nadane)
                Text(
                    "Monitored Apps Usage",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { showAppSelection = true }) {
                    Text("Select Apps to Display")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Usage on: $selectedDate",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { datePicker.show() }) {
                    Text("Select Date")
                }
                Spacer(Modifier.height(16.dp))

                if (displayData.isEmpty()) {
                    Text("Brak monitorowanych aplikacji. Dodaj je poniżej.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(displayData) { (pkg, timeSpent) ->
                            val (appName, appIcon, _) = getAppInfo(
                                context, pkg, iconCache = mutableMapOf(), nameCache = mutableMapOf()
                            )
                            val days = TimeUnit.MILLISECONDS.toDays(timeSpent)
                            val hours = TimeUnit.MILLISECONDS.toHours(timeSpent) % 24
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeSpent) % 60

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (appIcon != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = appIcon),
                                        contentDescription = "$appName icon",
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = android.R.drawable.sym_def_app_icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    buildString {
                                        append("$appName: ")
                                        if (days > 0) append("$days dni ")
                                        if (hours > 0) append("$hours godz ")
                                        append("$minutes min")
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Dialog wyboru aplikacji
    if (showAppSelection) {
        AlertDialog(
            onDismissRequest = { showAppSelection = false },
            title = { Text("Select Apps to Display") },
            text = {
                Column {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search…") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .height(300.dp)
                            .fillMaxWidth()
                    ) {
                        val sortedApps = installedPackages
                            .map { pkg ->
                                val label = try {
                                    context.packageManager.getApplicationLabel(
                                        context.packageManager.getApplicationInfo(pkg, 0)
                                    ).toString()
                                } catch (_: Exception) {
                                    pkg
                                }
                                label to pkg
                            }
                            .filter { (label, _) -> label.contains(searchQuery, ignoreCase = true) }
                            .sortedWith(compareByDescending<Pair<String, String>> { it.second in monitoredPackages }
                                .thenBy { it.first.lowercase() })

                        items(sortedApps) { (label, pkg) ->
                            val checked = pkg in monitoredPackages
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = pkg in monitoredPackages,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            if (checked) {
                                                val usage7d = getAppUsageLast7Days(context)
                                                // dla każdej pary (data → czas) w usage7d[pkg]:
                                                usage7d[pkg]?.forEach { (day, ms) ->
                                                    repo.upsertUsage(
                                                        AppUsage(
                                                            packageName = pkg,
                                                            date        = day,
                                                            usageTimeMs = ms
                                                        )
                                                    )
                                                }
                                            } else {
                                                // usuwamy wszystkie dni tej paczki
                                                repo.deleteUsagesForPackage(pkg)
                                                repolimit.clearLimit(pkg)
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAppSelection = false }) {
                    Text("Close")
                }
            },
            dismissButton = null
        )
    }
}

// Helper: czy uprawnienie do usage stats jest nadane
@Suppress("InlinedApi")
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

// Helper: czy uprawnienie do powiadomień jest nadane
fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            "android.permission.POST_NOTIFICATIONS"
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        // W starszych wersjach Androida powiadomienia są domyślnie dostępne
        true
    }
}

// Pobiera czas użycia aplikacji w ostatnich 24h
fun getAppUsageLast24h(context: Context): Map<String, Long> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val endTime = System.currentTimeMillis()
    val startTime = endTime - TimeUnit.DAYS.toMillis(1)

    val resumeTimes = mutableMapOf<String, Long>()
    val usageDurations = mutableMapOf<String, Long>()
    val events = usageStatsManager.queryEvents(startTime, endTime)
    val event = UsageEvents.Event()

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED ->
                resumeTimes[event.packageName] = event.timeStamp
            UsageEvents.Event.ACTIVITY_PAUSED -> {
                resumeTimes[event.packageName]?.let { resume ->
                    val delta = event.timeStamp - resume
                    usageDurations[event.packageName] = (usageDurations[event.packageName] ?: 0L) + delta
                }
            }
        }
    }
    return usageDurations
}

// Pobiera nazwę i ikonę aplikacji
fun getAppInfo(
    context: Context,
    packageName: String,
    iconCache: MutableMap<String, Drawable?>,
    nameCache: MutableMap<String, String>
): Triple<String, Drawable?, Nothing?> {
    return try {
        val pm = context.packageManager
        val ai = pm.getApplicationInfo(packageName, 0)
        val label = nameCache.getOrPut(packageName) { pm.getApplicationLabel(ai).toString() }
        val icon = iconCache.getOrPut(packageName) { pm.getApplicationIcon(packageName) }
        Triple(label, icon, null)
    } catch (e: PackageManager.NameNotFoundException) {
        Triple(packageName, null, null)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        UsageScreen()
    }
}

fun getAppUsageLast7Days(context: Context): Map<String, Map<LocalDate, Long>> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val zone = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(zone).date

    val result = mutableMapOf<String, MutableMap<LocalDate, Long>>()

    for (i in 0 until 7) {
        val day = today.minus(DatePeriod(days = i))
        val start = day.atStartOfDayIn(zone).toEpochMilliseconds()
        val end   = day.plus(DatePeriod(days = 1)).atStartOfDayIn(zone).toEpochMilliseconds()

        val statsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        statsList.forEach { stat ->
            val pkg = stat.packageName
            val t   = stat.totalTimeInForeground
            result.getOrPut(pkg) { mutableMapOf() }[day] = t
        }
    }
    return result
}