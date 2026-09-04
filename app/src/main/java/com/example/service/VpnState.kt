package com.example.service

data class VpnState(
    val isRunning: Boolean = false,
    val isStarting: Boolean = false,
    val blockedCount: Int = 0,
    val allowedCount: Int = 0,
    val startTime: Long = 0L,
    val packetsDropped: Long = 0L,
    val errorMessage: String? = null
)
