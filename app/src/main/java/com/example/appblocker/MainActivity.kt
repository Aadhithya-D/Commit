package com.example.appblocker

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.appblocker.ui.theme.AppBlockerTheme

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
    val icon: Drawable
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val installedApps = getInstalledApps()

        setContent {
            AppBlockerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppList(
                        apps = installedApps,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        // GET_META_DATA is a flag required for certain app details.
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return packages
            .filter {
                // Keep user-installed apps OR whitelisted Google apps
                (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName in googleAppsWhitelist
            }
            .map {
            AppInfo(
                name = it.loadLabel(pm).toString(),
                packageName = it.packageName,
                icon = it.loadIcon(pm)
            )
        }
        .filter { !it.name.contains('.') } // Filter out apps with '.' in their name
        .sortedBy { it.name } // Sort apps alphabetically
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = null, // Decorative image
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.name,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppRowPreview() {
    val context = LocalContext.current
    // Get this app's own icon for the preview
    val sampleApp = AppInfo(
        "Sample App",
        "com.sample.app",
        context.packageManager.getApplicationIcon(context.applicationInfo)
    )
    AppBlockerTheme {
        AppRow(app = sampleApp)
    }
}