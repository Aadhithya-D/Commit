package com.example.appblocker

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.appblocker.ui.theme.AppBlockerTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val googleAppsWhitelist = setOf(
    "com.android.chrome",
    "com.google.android.youtube",
    "com.google.android.gm",
    "com.google.android.apps.maps",
    "com.google.android.apps.photos",
    "com.google.android.apps.docs",
    "com.google.android.apps.drive"
)

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val totalTimeInForeground: Long = 0,
    val lastTimeUsed: Long = 0,
    val launchCount: Long = 0
)

data class UsageData(
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val launchCount: Long
)

data class OverallUsageStats(
    val todayTotalTime: Long,
    val todayOpenCount: Long,
    val weekTotalTime: Long,
    val weekOpenCount: Long
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppBlockerTheme {
                var hasUsagePermission by remember { mutableStateOf(checkUsageStatsPermission()) }
                var installedApps by remember { mutableStateOf(emptyList<AppInfo>()) }
                var overallStats by remember { mutableStateOf<OverallUsageStats?>(null) }

                LaunchedEffect(hasUsagePermission) {
                    if (hasUsagePermission) {
                        installedApps = getInstalledAppsWithUsage()
                        overallStats = getOverallUsageStats()
                    } else {
                        installedApps = getInstalledApps()
                        overallStats = null
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (!hasUsagePermission) {
                        PermissionRequestScreen(
                            onRequestPermission = {
                                requestUsageStatsPermission()
                            },
                            onRefresh = {
                                hasUsagePermission = checkUsageStatsPermission()
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        Column(modifier = Modifier.padding(innerPadding)) {
                            overallStats?.let { stats ->
                                OverallUsageDisplay(
                                    stats = stats,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            AppList(
                                apps = installedApps,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return packages
            .filter {
                (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName in googleAppsWhitelist
            }
            .map {
                AppInfo(
                    name = it.loadLabel(pm).toString(),
                    packageName = it.packageName,
                    icon = it.loadIcon(pm)
                )
            }
            .filter { !it.name.contains('.') }
            .sortedBy { it.name }
    }

    private fun getInstalledAppsWithUsage(): List<AppInfo> {
        val pm = packageManager
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        // Get usage stats for the last 7 days using INTERVAL_BEST
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis
        
        // Debug: Log the time range we're querying
        android.util.Log.d("AppBlocker", "Querying usage stats from ${Date(startTime)} to ${Date(endTime)}")
        
        // Use INTERVAL_BEST to get the most detailed data available
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        )
        
        // Debug: Log how many usage stats we got
        android.util.Log.d("AppBlocker", "Found ${usageStats.size} usage stats entries")
        
        // Group by package name and sum up the usage data
        val usageMap = usageStats.groupBy { it.packageName }
            .mapValues { (packageName, stats) ->
                val totalTime = stats.sumOf { it.totalTimeInForeground }
                val lastUsed = stats.maxOfOrNull { it.lastTimeUsed } ?: 0
                
                // Debug: Log usage for apps with significant usage
                if (totalTime > 0) {
                    android.util.Log.d("AppBlocker", "$packageName: ${formatDuration(totalTime)}")
                }
                
                UsageData(
                    totalTimeInForeground = totalTime,
                    lastTimeUsed = lastUsed,
                    launchCount = stats.sumOf { 
                        // Better launch count estimation
                        if (it.totalTimeInForeground > 0) {
                            maxOf(1, it.totalTimeInForeground / (1000 * 60 * 3)) // Assume avg 3 min per session
                        } else 0
                    }
                )
            }
        
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return packages
            .filter {
                (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName in googleAppsWhitelist
            }
            .map { appInfo ->
                val usage = usageMap[appInfo.packageName]
                AppInfo(
                    name = appInfo.loadLabel(pm).toString(),
                    packageName = appInfo.packageName,
                    icon = appInfo.loadIcon(pm),
                    totalTimeInForeground = usage?.totalTimeInForeground ?: 0,
                    lastTimeUsed = usage?.lastTimeUsed ?: 0,
                    launchCount = usage?.launchCount ?: 0
                )
            }
            .filter { !it.name.contains('.') }
            .sortedByDescending { it.totalTimeInForeground } // Sort by most used
    }
    
    private fun getOverallUsageStats(): OverallUsageStats {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        // Get today's stats
        val todayCalendar = Calendar.getInstance()
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        todayCalendar.set(Calendar.MINUTE, 0)
        todayCalendar.set(Calendar.SECOND, 0)
        todayCalendar.set(Calendar.MILLISECOND, 0)
        val todayStart = todayCalendar.timeInMillis
        val todayEnd = System.currentTimeMillis()
        
        val todayStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            todayStart,
            todayEnd
        )
        
        // Get this week's stats
        val weekCalendar = Calendar.getInstance()
        weekCalendar.add(Calendar.DAY_OF_YEAR, -7)
        weekCalendar.set(Calendar.HOUR_OF_DAY, 0)
        weekCalendar.set(Calendar.MINUTE, 0)
        weekCalendar.set(Calendar.SECOND, 0)
        weekCalendar.set(Calendar.MILLISECOND, 0)
        val weekStart = weekCalendar.timeInMillis
        val weekEnd = System.currentTimeMillis()
        
        val weekStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            weekStart,
            weekEnd
        )
        
        // Calculate today's totals
        val todayTotalTime = todayStats.sumOf { it.totalTimeInForeground }
        val todayOpenCount = todayStats.sumOf { 
            if (it.totalTimeInForeground > 0) {
                maxOf(1, it.totalTimeInForeground / (1000 * 60 * 3)) // Assume avg 3 min per session
            } else 0
        }
        
        // Calculate week's totals
        val weekTotalTime = weekStats.sumOf { it.totalTimeInForeground }
        val weekOpenCount = weekStats.sumOf { 
            if (it.totalTimeInForeground > 0) {
                maxOf(1, it.totalTimeInForeground / (1000 * 60 * 3)) // Assume avg 3 min per session
            } else 0
        }
        
        android.util.Log.d("AppBlocker", "Today: ${formatDuration(todayTotalTime)}, Opens: $todayOpenCount")
        android.util.Log.d("AppBlocker", "Week: ${formatDuration(weekTotalTime)}, Opens: $weekOpenCount")
        
        return OverallUsageStats(
            todayTotalTime = todayTotalTime,
            todayOpenCount = todayOpenCount,
            weekTotalTime = weekTotalTime,
            weekOpenCount = weekOpenCount
        )
    }
}

@Composable
fun OverallUsageDisplay(
    stats: OverallUsageStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Today's stats
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "TODAY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatDuration(stats.todayTotalTime),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stats.todayOpenCount} opens",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Divider
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "|",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                // This week's stats
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "THIS WEEK",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatDuration(stats.weekTotalTime),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stats.weekOpenCount} opens",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Daily average
            if (stats.weekTotalTime > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Daily avg: ${formatDuration(stats.weekTotalTime / 7)} • ${stats.weekOpenCount / 7} opens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Usage Access Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "To display app usage statistics, this app needs access to usage data. Please grant permission in the settings.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRequestPermission) {
            Text("Open Settings")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRefresh) {
            Text("Refresh")
        }
    }
}

@Composable
fun AppList(apps: List<AppInfo>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(apps) { app ->
            AppRow(app = app)
        }
    }
}

@Composable
fun AppRow(app: AppInfo, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { /* TODO: Handle app selection */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = app.icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                if (app.totalTimeInForeground > 0) {
                    Text(
                        text = "Weekly: ${formatDuration(app.totalTimeInForeground)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Daily avg: ${formatDuration(app.totalTimeInForeground / 7)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (app.launchCount > 0) {
                        Text(
                            text = "Launches: ${app.launchCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun formatDuration(milliseconds: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}

fun formatLastUsed(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    
    return when {
        days > 0 -> {
            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}

@Preview(showBackground = true)
@Composable
fun AppRowPreview() {
    val context = LocalContext.current
    val sampleApp = AppInfo(
        "App Blocker",
        "com.example.appblocker",
        context.packageManager.getApplicationIcon(context.applicationInfo),
        totalTimeInForeground = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30),
        lastTimeUsed = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3),
        launchCount = 15
    )
    AppBlockerTheme {
        AppRow(app = sampleApp)
    }
}