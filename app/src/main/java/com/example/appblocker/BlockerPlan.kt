package com.example.appblocker

import java.time.LocalTime

data class BlockerPlan(
    val id: String,
    val name: String,
    val timeRange: TimeRange,
    val appTimers: Map<String, AppTimerConfig> = emptyMap(),
    val isActive: Boolean = true
)

data class AppTimerConfig(
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 0, // 0 means blocked during block hours only, >0 means additional daily limit outside block hours
    val isBlocked: Boolean = true
)

data class TimeRange(
    val startTime: LocalTime,
    val endTime: LocalTime
) {
    fun isCurrentTimeInRange(): Boolean {
        val now = LocalTime.now()
        return if (startTime.isBefore(endTime)) {
            now.isAfter(startTime) && now.isBefore(endTime)
        } else {
            // Handles overnight ranges like 10 PM - 6 AM
            now.isAfter(startTime) || now.isBefore(endTime)
        }
    }
} 