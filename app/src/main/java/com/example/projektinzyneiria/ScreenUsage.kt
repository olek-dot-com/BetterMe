package com.example.projektinzyneiria

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.projektinzyneiria.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun UsageScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var allUsageData by remember { mutableStateOf(listOf<Pair<String, Long>>()) }
    var filteredUsageData by remember { mutableStateOf(listOf<Pair<String, Long>>()) }
    var selectedApps by remember { mutableStateOf(setOf<String>()) }
    var showAppSelection by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var dataLoaded by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val installedPackages = remember {
        context.packageManager
            .getInstalledApplications(0)
            .map { it.packageName }
    }


    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = hasUsageStatsPermission(context)
        if (hasPermission) {
            allUsageData = getAppUsage(context)
            dataLoaded = true
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            allUsageData = getAppUsage(context)
            dataLoaded = true
        }
        while (true) {
            if (hasPermission) {
                allUsageData = getAppUsage(context)
                // Update filtered data based on current selection
                filteredUsageData = allUsageData.filter { (packageName, _) ->
                    selectedApps.contains(packageName)
                }
            }
            delay(TimeUnit.MINUTES.toMillis(5)) // Update every 5 minutes
        }
    }

    val iconCache = remember { mutableMapOf<String, Drawable?>() }
    val nameCache = remember { mutableMapOf<String, String>() }
    val timeCache = remember { mutableMapOf<String, Long>() }

    Column(
        modifier = modifier.padding(16.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasPermission) {
            if (dataLoaded) {
                Text(
                    "App Usage (Last 7 Days)",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { showAppSelection = true }) {
                    Text("Select Apps to Display")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredUsageData.isEmpty()) {
                    Text("No apps selected. Please select apps to display.")
                } else {
                    val displayData = remember(filteredUsageData, selectedApps) {
                        val baseList = if (selectedApps.isEmpty()) {
                            filteredUsageData                            // pokaż wszystko
                        } else {
                            filteredUsageData.filter { (pkg, _) ->       // pokaż tylko wybrane
                                selectedApps.contains(pkg)
                            }
                        }
                        baseList.sortedByDescending { it.second }   // <-- sortujemy po czasie
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(displayData) { (packageName, timeSpent) ->
                            timeCache[packageName] = timeSpent
                            val (appName, appIcon, appTime) = getAppInfo(
                                context,
                                packageName,
                                "MainActivity!",
                                iconCache,
                                nameCache,
                                timeCache
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
                                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                        contentDescription = "Default icon",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(buildString {
                                    append("$appName: ")
                                    if (days > 0) append("$days dni ")
                                    if (days > 0 || hours > 0) append("$hours godzin ")
                                    append("$minutes minut")
                                })
                            }
                        }
                    }
                }
            } else {
                Text("Loading usage data...")
            }
        } else {
            // Permission request UI remains the same
            Text(
                "Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This app needs permission to access usage data. Please grant the permission in settings.",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                showPermissionDialog = true
            }) {
                Text("Grant Permission")
            }
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { Text("Grant Usage Access") },
                    text = {
                        Text(
                            "To use this feature, you need to allow this app to access usage data. " +
                                    "On the next screen, find \"${context.packageManager.getApplicationLabel(context.applicationInfo)}\" and enable the permission."
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            showPermissionDialog = false
                            launcher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }) {
                            Text("Go to Settings")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showPermissionDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    // App selection dialog
    if (showAppSelection && dataLoaded) {
        AlertDialog(
            onDismissRequest = { showAppSelection = false },
            title = { Text("Select Apps to Display") },
            text = {
                // lista posortowana po nazwach aplikacji:
                val sortedApps = remember(installedPackages, searchQuery) {
                    installedPackages
                        .map { pkg ->
                            val label = try {
                                context.packageManager
                                    .getApplicationLabel(
                                        context.packageManager.getApplicationInfo(pkg, 0)
                                    ).toString()
                            } catch (_: Exception) {
                                pkg
                            }
                            label to pkg
                        }
                        .filter { (label, _) ->
                            label.contains(searchQuery, ignoreCase = true)
                        }
                        .sortedBy { it.first.lowercase() }
                }

                Column {
                    // ======== PASEK WYSZUKIWANIA ========
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Szukaj aplikacji…") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    // ======== LISTA APLIKACJI ========
                    LazyColumn(
                        modifier = Modifier
                            .height(400.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(sortedApps) { (appName, packageName) ->
                            // czas użycia:
                            val timeSpent = allUsageData.toMap()[packageName] ?: 0L
                            // ikonka:
                            val appIcon = iconCache.getOrPut(packageName) {
                                getIcon(context, packageName, "MainActivity")
                            }
                            // painter:
                            val painter = if (appIcon != null) {
                                rememberAsyncImagePainter(model = appIcon)
                            } else {
                                painterResource(id = R.drawable.ic_launcher_foreground)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedApps.contains(packageName),
                                    onCheckedChange = { checked ->
                                        selectedApps = if (checked) {
                                            selectedApps + packageName
                                        } else {
                                            selectedApps - packageName
                                        }
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Image(
                                    painter = painter,
                                    contentDescription = "$appName icon",
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(appName)
                            }
                        }
                    }
                }
            }
,
            confirmButton = {
                Button(onClick = {
                    showAppSelection = false
                    // Update filtered data based on selection
                    val usageMap = allUsageData.toMap()
                    filteredUsageData = selectedApps.map { pkg ->
                        pkg to (usageMap[pkg] ?: 0L)
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = { showAppSelection = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Rest of the code remains the same (hasUsageStatsPermission, getAppUsage, getAppInfo, getIcon)
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

fun getAppUsage(context: Context): List<Pair<String, Long>> {
    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val endTime = System.currentTimeMillis()
    val startTime = endTime - TimeUnit.DAYS.toMillis(7) // Last 7 days

    val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
    val resumeTimes = mutableMapOf<String, Long>()
    val usageDurations = mutableMapOf<String, Long>()

    val event = UsageEvents.Event()
    while (usageEvents.hasNextEvent()) {
        usageEvents.getNextEvent(event)

        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                resumeTimes[event.packageName] = event.timeStamp
            }

            UsageEvents.Event.ACTIVITY_PAUSED -> {
                val resumeTime = resumeTimes[event.packageName]
                if (resumeTime != null) {
                    val duration = event.timeStamp - resumeTime
                    usageDurations[event.packageName] =
                        (usageDurations[event.packageName] ?: 0L) + duration
                }
            }
        }
    }

    return usageDurations.entries
        .sortedByDescending { it.value } // Sort by usage duration
        .map { it.key to it.value }
}

fun getAppInfo(
    context: Context,
    packageName: String,
    className: String,
    iconCache: MutableMap<String, Drawable?>,
    nameCache: MutableMap<String, String>,
    timeCache: MutableMap<String, Long>
): Triple<String, Drawable?, Long?> {
    val packageManager = context.packageManager
    return try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        val appName = nameCache.getOrPut(packageName) { packageManager.getApplicationLabel(appInfo).toString() }
        val appIcon = iconCache.getOrPut(packageName) { getIcon(context, packageName, className) }
        val appTime = timeCache[packageName]
        Triple(appName, appIcon, appTime)
    } catch (e: PackageManager.NameNotFoundException) {
        Triple(packageName, null, null)
    }
}

fun getIcon(context: Context, packageName: String, className: String): Drawable? {
    var drawable: Drawable? = null
    try {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setClassName(packageName, className)
        drawable = context.packageManager.getActivityIcon(intent)
    } catch (e: Exception) {
        try {
            drawable = context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return drawable
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        UsageScreen()
    }
}