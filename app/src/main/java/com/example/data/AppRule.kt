package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val uid: Int,
    val isBlocked: Boolean = false,
    val isSystemApp: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
