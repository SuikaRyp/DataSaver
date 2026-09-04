package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules ORDER BY appName ASC")
    fun getAllRules(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules")
    suspend fun getAllRulesDirect(): List<AppRule>

    @Query("SELECT packageName FROM app_rules WHERE isBlocked = 1")
    suspend fun getBlockedPackageNames(): List<String>

    @Query("SELECT uid FROM app_rules WHERE isBlocked = 1")
    suspend fun getBlockedUids(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rule: AppRule)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<AppRule>)

    @Query("UPDATE app_rules SET isBlocked = :isBlocked, updatedAt = :timestamp WHERE packageName = :packageName")
    suspend fun setBlockedStatus(packageName: String, isBlocked: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_rules SET isBlocked = :isBlocked, updatedAt = :timestamp WHERE (:onlyUserApps = 0 OR isSystemApp = 0)")
    suspend fun setAllBlockedStatus(isBlocked: Boolean, onlyUserApps: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM app_rules WHERE packageName NOT IN (:existingPackages)")
    suspend fun cleanOldPackages(existingPackages: List<String>)
}
