package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object VpnStateManager {
    private val _state = MutableStateFlow(VpnState())
    val state: StateFlow<VpnState> = _state.asStateFlow()

    fun setRunning(running: Boolean, blockedCount: Int = _state.value.blockedCount, allowedCount: Int = _state.value.allowedCount) {
        _state.update {
            it.copy(
                isRunning = running,
                isStarting = false,
                blockedCount = blockedCount,
                allowedCount = allowedCount,
                startTime = if (running) System.currentTimeMillis() else 0L,
                errorMessage = null
            )
        }
    }

    fun setStarting(starting: Boolean) {
        _state.update {
            it.copy(isStarting = starting, errorMessage = null)
        }
    }

    fun updateStats(blockedCount: Int, allowedCount: Int) {
        _state.update {
            it.copy(blockedCount = blockedCount, allowedCount = allowedCount)
        }
    }

    fun incrementDroppedPackets(count: Long = 1) {
        _state.update {
            it.copy(packetsDropped = it.packetsDropped + count)
        }
    }

    fun setError(message: String) {
        _state.update {
            it.copy(isRunning = false, isStarting = false, errorMessage = message)
        }
    }
}
