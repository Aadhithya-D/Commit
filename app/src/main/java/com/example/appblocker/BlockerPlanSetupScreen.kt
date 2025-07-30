package com.example.appblocker

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockerPlanSetupScreen(
    apps: List<AppInfo>,
    onPlanCreated: (BlockerPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    var planName by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(17, 0)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppConfigs by remember { mutableStateOf<Map<String, AppTimerConfig>>(emptyMap()) }
    var showAppTimerDialog by remember { mutableStateOf<AppInfo?>(null) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val blockerPlanManager = remember { BlockerPlanManager(context) }
    
    // Filter apps based on search query
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                app.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Text(
                    text = "Create Your App Blocker Plan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp),
                    textAlign = TextAlign.Center
                )
        
        // Plan name input
        OutlinedTextField(
            value = planName,
            onValueChange = { planName = it },
            label = { Text("Plan Name") },
            placeholder = { Text("e.g., Work Focus, Study Time") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Time range selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Block Hours",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Selected apps will be blocked during these hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Start time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "From",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = formatTimeToAmPm(startTime),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    // End time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "To",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = formatTimeToAmPm(endTime),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
        
        // Search bar
        SearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Apps list
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(filteredApps) { app ->
                AppSelectionRow(
                    app = app,
                    isSelected = selectedAppConfigs.containsKey(app.packageName),
                    timerConfig = selectedAppConfigs[app.packageName],
                    onAppClick = { showAppTimerDialog = app },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }
        
        // Create plan button
        Button(
            onClick = {
                if (planName.isNotBlank()) {
                    val plan = BlockerPlan(
                        id = blockerPlanManager.createPlanId(),
                        name = planName,
                        timeRange = TimeRange(startTime, endTime),
                        appTimers = selectedAppConfigs
                    )
                    onPlanCreated(plan)
                }
            },
            enabled = planName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Create Blocker Plan")
        }
    }
    
    // Time pickers
    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime,
            onTimeSelected = { startTime = it },
            onDismiss = { showStartTimePicker = false }
        )
    }
    
    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime,
            onTimeSelected = { endTime = it },
            onDismiss = { showEndTimePicker = false }
        )
    }
    
    // App timer configuration dialog
    showAppTimerDialog?.let { app ->
        AppTimerConfigDialog(
            app = app,
            currentConfig = selectedAppConfigs[app.packageName],
            onConfigChanged = { config ->
                selectedAppConfigs = if (config != null) {
                    selectedAppConfigs + (app.packageName to config)
                } else {
                    selectedAppConfigs - app.packageName
                }
            },
            onDismiss = { showAppTimerDialog = null }
        )
    }
}

@Composable
fun AppSelectionRow(
    app: AppInfo,
    isSelected: Boolean,
    timerConfig: AppTimerConfig?,
    onAppClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAppClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
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
                
                if (isSelected && timerConfig != null) {
                    Text(
                        text = if (timerConfig.dailyLimitMinutes == 0) {
                            "Blocked during block hours only"
                        } else {
                            "Blocked during block hours + ${timerConfig.dailyLimitMinutes} min/day limit"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (app.totalTimeInForeground > 0) {
                    Text(
                        text = "Weekly: ${formatDuration(app.totalTimeInForeground)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add to plan",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AppTimerConfigDialog(
    app: AppInfo,
    currentConfig: AppTimerConfig?,
    onConfigChanged: (AppTimerConfig?) -> Unit,
    onDismiss: () -> Unit
) {
    var isBlocked by remember { mutableStateOf(currentConfig?.isBlocked ?: true) }
    var dailyLimitMinutes by remember { mutableStateOf(currentConfig?.dailyLimitMinutes ?: 0) }
    var isCompleteBlock by remember { mutableStateOf(currentConfig?.dailyLimitMinutes == 0) }
    var showHoursDropdown by remember { mutableStateOf(false) }
    var showMinutesDropdown by remember { mutableStateOf(false) }
    
    val selectedHours = dailyLimitMinutes / 60
    val selectedMinutes = dailyLimitMinutes % 60
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = app.icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(app.name)
            }
        },
        text = {
            Column {
                Text(
                    text = "This app will be blocked during your selected block hours. Configure additional usage limits for outside those hours:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // No additional limits toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Block during block hours only")
                        Text(
                            text = "No additional time limits outside block hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isCompleteBlock,
                        onCheckedChange = { 
                            isCompleteBlock = it
                            if (it) dailyLimitMinutes = 0
                        }
                    )
                }
                
                if (!isCompleteBlock) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Daily limit outside block hours:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hours dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { showHoursDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${selectedHours}h")
                            }
                            DropdownMenu(
                                expanded = showHoursDropdown,
                                onDismissRequest = { showHoursDropdown = false }
                            ) {
                                for (hour in 0..5) {
                                    DropdownMenuItem(
                                        text = { Text("${hour}h") },
                                        onClick = {
                                            dailyLimitMinutes = hour * 60 + selectedMinutes
                                            showHoursDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Minutes dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { showMinutesDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${selectedMinutes}m")
                            }
                            DropdownMenu(
                                expanded = showMinutesDropdown,
                                onDismissRequest = { showMinutesDropdown = false }
                            ) {
                                for (minute in 0..55 step 5) {
                                    DropdownMenuItem(
                                        text = { Text("${minute}m") },
                                        onClick = {
                                            dailyLimitMinutes = selectedHours * 60 + minute
                                            showMinutesDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    if (dailyLimitMinutes > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Total: ${if (selectedHours > 0) "${selectedHours}h " else ""}${selectedMinutes}m per day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val config = AppTimerConfig(
                        packageName = app.packageName,
                        appName = app.name,
                        dailyLimitMinutes = if (isCompleteBlock) 0 else dailyLimitMinutes,
                        isBlocked = true
                    )
                    onConfigChanged(config)
                    onDismiss()
                }
            ) {
                Text("Add to Blocker Plan")
            }
        },
        dismissButton = {
            Row {
                if (currentConfig != null) {
                    TextButton(
                        onClick = {
                            onConfigChanged(null)
                            onDismiss()
                        }
                    ) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = remember {
        TimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = false
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedTime = LocalTime.of(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onTimeSelected(selectedTime)
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimeToAmPm(time: LocalTime): String {
    val hour = time.hour
    val minute = time.minute
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", displayHour, minute, amPm)
} 