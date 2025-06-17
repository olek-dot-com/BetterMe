package com.example.projektinzyneiria

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.NumberPicker
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.example.projektinzyneiria.Data.AppUsageRepository
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.style.TextAlign
import com.example.projektinzyneiria.UsageLimitData.AppLimitRepository

@Composable
fun LimitScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val usageRepo = remember { AppUsageRepository.getInstance(context) }
    val limitRepo = remember { AppLimitRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    // 1. Pobierz listę monitorowanych aplikacji
    val usages by usageRepo.getAllUsages().collectAsState(initial = emptyList())
    val monitoredPackages = remember(usages) {
        usages.map { it.packageName }.distinct().sorted()
    }

    // 2. Pobierz aktualne limity
    val limits by limitRepo.getAllLimits().collectAsState(initial = emptyList())
    val limitMap = remember(limits) { limits.associateBy { it.packageName } }

    // 3. Stany dialogów i tymczasowe wartości
    val showDialog = remember { mutableStateMapOf<String, Boolean>() }
    val tempLimits = remember { mutableStateMapOf<String, Int>() }
    val hourStates   = remember { mutableStateMapOf<String, Int>() }
    val minuteStates = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(monitoredPackages, limits) {
        monitoredPackages.forEach { pkg ->
            hourStates[pkg]   = (limitMap[pkg]?.dailyLimitMin ?: 0) / 60
            minuteStates[pkg] = (limitMap[pkg]?.dailyLimitMin ?: 0) % 60 / 5
            showDialog[pkg]   = false
        }

    }

    Column(modifier = modifier.padding(16.dp).fillMaxWidth()) {
        Text(
            text = "Daily Limits for Monitored Apps",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        if (monitoredPackages.isEmpty()) {
            Text(
                text = "No monitored applications. Please add apps in the Usage screen.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(monitoredPackages) { pkg ->
                    val currentLimit = limitMap[pkg]?.dailyLimitMin ?: 0
                    val dialogOpen = showDialog[pkg] == true
                    val hours = hourStates[pkg] ?: 0
                    val minutes = (minuteStates[pkg] ?: 0) * 5
                    val appInfo = getAppInfo(context, pkg)

                    // Dialog wyboru limitu
                    if (dialogOpen) {
                        AlertDialog(
                            onDismissRequest = { showDialog[pkg] = false },
                            title = { Text("Set daily limit for ${appInfo.first}") },
                            text = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${hours}h ${minutes}m",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        // Picker godzin
                                        AndroidView({ ctx ->
                                            NumberPicker(ctx).apply {
                                                minValue = 0; maxValue = 24; wrapSelectorWheel = false
                                                value = hourStates[pkg]!!
                                                setOnValueChangedListener { _, _, newH -> hourStates[pkg] = newH }
                                            }
                                        }, Modifier.size(100.dp,150.dp))
                                        // Picker minut (co 5)
                                        AndroidView({ ctx ->
                                            NumberPicker(ctx).apply {
                                                minValue = 0; maxValue = 11; wrapSelectorWheel = false
                                                displayedValues = Array(12){ i -> "${i*5}" }
                                                    value = minuteStates[pkg]!!
                                                    setOnValueChangedListener { _, _, newM -> minuteStates[pkg] = newM }
                                                }
                                            }, Modifier.size(100.dp,150.dp))
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val totalMinutes = (hourStates[pkg]!! * 60) + (minuteStates[pkg]!! * 5)
                                    coroutineScope.launch {
                                        limitRepo.setLimit(pkg, totalMinutes)
                                    }
                                    tempLimits[pkg] = totalMinutes       // aktualizujemy cache
                                    showDialog[pkg] = false
                                }) {
                                    Text("OK")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog[pkg] = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Wiersz aplikacji
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconPainter = appInfo.second?.let { rememberAsyncImagePainter(it) }
                            ?: painterResource(android.R.drawable.sym_def_app_icon)
                        Image(
                            painter = iconPainter,
                            contentDescription = appInfo.first,
                            modifier = Modifier.size(40.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = appInfo.first,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${currentLimit / 60}h ${currentLimit % 60}m",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { showDialog[pkg] = true }) {
                            Text("Set")
                        }
                    }
                }
            }
        }
    }
}

// Helper
fun getAppInfo(context: Context, packageName: String): Pair<String, Drawable?> {
    return try {
        val pm = context.packageManager
        val ai = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(ai).toString() to pm.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        packageName to null
    }
}

@Preview(showBackground = true)
@Composable
fun LimitScreenPreview() {
    com.example.projektinzyneiria.ui.theme.AppTheme {
        LimitScreen()
    }
}
