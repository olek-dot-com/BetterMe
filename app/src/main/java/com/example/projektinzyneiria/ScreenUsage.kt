package com.example.projektinzyneiria

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.projektinzyneiria.ui.theme.ProjektInzyneiriaTheme

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.div

@Composable
fun UsageScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var usageData by remember { mutableStateOf(listOf<Pair<String, Long>>()) }
    var hasPermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var dataLoaded by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var watchedApps by remember { mutableStateOf(mutableListOf<String>()) }
    var allApps by remember { mutableStateOf(listOf<String>()) }
    var showAllAppsDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = hasUsageStatsPermission(context)
        if (hasPermission) {
            usageData = getAppUsage(context)
            dataLoaded = true
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            usageData = getAppUsage(context)
            dataLoaded = true
        }
        while (true) {
            if (hasPermission) {
                usageData = getAppUsage(context)
            }
            delay(TimeUnit.MINUTES.toMillis(15))
        }
    }

    LaunchedEffect(Unit) {
        allApps = getAllApps(context)
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasPermission) {
            if (dataLoaded) {
                Text(
                    "App Usage (Last 24 hours)",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (usageData.isEmpty()) {
                    Text("No usage data found.")
                } else {
                    usageData.take(10).forEach { (packageName, timeSpent) ->
                        val timeInMinutes = timeSpent / 1000 / 60
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$packageName: $timeInMinutes minutes", modifier = Modifier.weight(1f))
                            Button(onClick = {
                                if (!watchedApps.contains(packageName)) {
                                    watchedApps.add(packageName)
                                }
                            }) {
                                Text("Watch")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showAllAppsDialog = true }) {
                    Text("Show All Apps")
                }
                if (showAllAppsDialog) {
                    AlertDialog(
                        onDismissRequest = { showAllAppsDialog = false },
                        title = { Text("All Apps") },
                        text = {
                            Column {
                                allApps.forEach { app ->
                                    TextButton(onClick = {
                                        if (!watchedApps.contains(app)) {
                                            watchedApps.add(app)
                                        }
                                        showAllAppsDialog = false
                                    }) {
                                        Text(app)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = { showAllAppsDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            } else {
                Text("Loading usage data...")
            }
        } else {
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
}

fun getAllApps(context: Context): List<String> {
    val pm = context.packageManager
    val packages = pm.getInstalledApplications(0)
    return packages.map { it.packageName }
}

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
    val startTime = endTime - TimeUnit.DAYS.toMillis(1)

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
        .sortedByDescending { it.value }
        .map { it.key to it.value }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ProjektInzyneiriaTheme {
        UsageScreen()
    }
}