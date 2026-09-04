package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRule
import com.example.data.AppRuleRepository
import com.example.service.MyVpnService
import com.example.service.VpnState
import com.example.service.VpnStateManager
import com.example.ui.model.AppFilterType
import com.example.ui.model.AppItem
import com.example.ui.model.MainUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRuleRepository
    val vpnState: StateFlow<VpnState> = VpnStateManager.state

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow(AppFilterType.ALL)
    private val _isLoading = MutableStateFlow(true)
    private val _cachedIcons = mutableMapOf<String, android.graphics.drawable.Drawable?>()

    val uiState: StateFlow<MainUiState>

    init {
        val db = AppDatabase.getInstance(application)
        repository = AppRuleRepository(application, db.appRuleDao())

        // Initial sync of installed apps on device
        viewModelScope.launch {
            repository.syncInstalledApps()
            _isLoading.value = false
        }

        uiState = combine(
            repository.allRulesFlow,
            _searchQuery,
            _filterType,
            _isLoading
        ) { rules, query, filter, loading ->
            val appItems = rules.map { rule ->
                val icon = _cachedIcons.getOrPut(rule.packageName) {
                    repository.getAppIcon(rule.packageName)
                }
                AppItem(
                    packageName = rule.packageName,
                    appName = rule.appName,
                    uid = rule.uid,
                    isBlocked = rule.isBlocked,
                    isSystemApp = rule.isSystemApp,
                    icon = icon
                )
            }

            val blockedCount = appItems.count { it.isBlocked }
            val allowedCount = appItems.size - blockedCount

            val filtered = appItems.filter { app ->
                val matchesQuery = query.isBlank() ||
                        app.appName.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)

                val matchesFilter = when (filter) {
                    AppFilterType.ALL -> true
                    AppFilterType.BLOCKED -> app.isBlocked
                    AppFilterType.ALLOWED -> !app.isBlocked
                    AppFilterType.USER_ONLY -> !app.isSystemApp
                    AppFilterType.SYSTEM_ONLY -> app.isSystemApp
                }

                matchesQuery && matchesFilter
            }

            MainUiState(
                apps = appItems,
                filteredApps = filtered,
                searchQuery = query,
                filterType = filter,
                isLoading = loading,
                totalAppsCount = appItems.size,
                blockedAppsCount = blockedCount,
                allowedAppsCount = allowedCount
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState()
        )
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChanged(filter: AppFilterType) {
        _filterType.value = filter
    }

    fun toggleAppBlock(packageName: String, isCurrentlyBlocked: Boolean) {
        viewModelScope.launch {
            val newBlocked = !isCurrentlyBlocked
            repository.toggleAppBlock(packageName, newBlocked)
            if (vpnState.value.isRunning) {
                MyVpnService.updateRules(getApplication())
            }
        }
    }

    fun blockAll(onlyUserApps: Boolean) {
        viewModelScope.launch {
            repository.setAllBlocked(isBlocked = true, onlyUserApps = onlyUserApps)
            if (vpnState.value.isRunning) {
                MyVpnService.updateRules(getApplication())
            }
        }
    }

    fun allowAll() {
        viewModelScope.launch {
            repository.setAllBlocked(isBlocked = false, onlyUserApps = false)
            if (vpnState.value.isRunning) {
                MyVpnService.updateRules(getApplication())
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.syncInstalledApps()
            _isLoading.value = false
        }
    }

    fun toggleVpnMaster(onNeedVpnPermission: () -> Unit) {
        val currentRunning = vpnState.value.isRunning
        if (currentRunning) {
            MyVpnService.stop(getApplication())
        } else {
            onNeedVpnPermission()
        }
    }

    fun startVpnService() {
        MyVpnService.start(getApplication())
    }
}
