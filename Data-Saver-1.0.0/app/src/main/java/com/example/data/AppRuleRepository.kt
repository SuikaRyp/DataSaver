package com.example.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRuleRepository(
    private val context: Context,
    private val dao: AppRuleDao
) {
    val allRulesFlow: Flow<List<AppRule>> = dao.getAllRules()

    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val ownPackageName = context.packageName

        val currentRules = dao.getAllRulesDirect().associateBy { it.packageName }
        val newRules = mutableListOf<AppRule>()
        val existingPackageNames = mutableListOf<String>()

        for (appInfo in installedApps) {
            // Do not restrict our own app
            if (appInfo.packageName == ownPackageName) continue

            existingPackageNames.add(appInfo.packageName)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val existing = currentRules[appInfo.packageName]
            if (existing == null) {
                val appLabel = try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    appInfo.packageName
                }
                newRules.add(
                    AppRule(
                        packageName = appInfo.packageName,
                        appName = if (appLabel.isNotBlank()) appLabel else appInfo.packageName,
                        uid = appInfo.uid,
                        isBlocked = false,
                        isSystemApp = isSystem
                    )
                )
            }
        }

        if (newRules.isNotEmpty()) {
            dao.insertAll(newRules)
        }
    }

    suspend fun toggleAppBlock(packageName: String, isBlocked: Boolean) = withContext(Dispatchers.IO) {
        dao.setBlockedStatus(packageName, isBlocked)
    }

    suspend fun setAllBlocked(isBlocked: Boolean, onlyUserApps: Boolean) = withContext(Dispatchers.IO) {
        dao.setAllBlockedStatus(isBlocked, onlyUserApps)
    }

    suspend fun getBlockedPackages(): List<String> = withContext(Dispatchers.IO) {
        dao.getBlockedPackageNames()
    }

    suspend fun getBlockedUids(): List<Int> = withContext(Dispatchers.IO) {
        dao.getBlockedUids()
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }
}
