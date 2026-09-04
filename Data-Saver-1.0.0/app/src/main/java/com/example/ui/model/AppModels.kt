package com.example.ui.model

import android.graphics.drawable.Drawable

enum class AppFilterType(val label: String) {
    ALL("Semua"),
    BLOCKED("Diblokir"),
    ALLOWED("Diizinkan"),
    USER_ONLY("Aplikasi Pengguna"),
    SYSTEM_ONLY("Sistem")
}

data class AppItem(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val isBlocked: Boolean,
    val isSystemApp: Boolean,
    val icon: Drawable? = null
)

data class MainUiState(
    val apps: List<AppItem> = emptyList(),
    val filteredApps: List<AppItem> = emptyList(),
    val searchQuery: String = "",
    val filterType: AppFilterType = AppFilterType.ALL,
    val isLoading: Boolean = true,
    val totalAppsCount: Int = 0,
    val blockedAppsCount: Int = 0,
    val allowedAppsCount: Int = 0
)
