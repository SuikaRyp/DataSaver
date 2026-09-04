package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MyVpnService : VpnService() {

    companion object {
        const val TAG = "DataSaverShieldVPN"
        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_STOP = "com.example.service.action.STOP"
        const val ACTION_UPDATE_RULES = "com.example.service.action.UPDATE_RULES"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "datasaver_shield_vpn_channel"

        fun start(context: Context) {
            val intent = Intent(context, MyVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MyVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateRules(context: Context) {
            val intent = Intent(context, MyVpnService::class.java).apply {
                action = ACTION_UPDATE_RULES
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetLoopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startVpn()
            }
            ACTION_STOP -> {
                stopVpn()
                stopSelf()
            }
            ACTION_UPDATE_RULES -> {
                if (VpnStateManager.state.value.isRunning) {
                    // Restart interface with updated rule set
                    serviceScope.launch {
                        reloadVpnRules()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn() {
        VpnStateManager.setStarting(true)
        val notification = createNotification("Mengaktifkan perlindungan kuota...")
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            try {
                configureAndEstablishTunnel()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting VPN tunnel", e)
                VpnStateManager.setError(e.localizedMessage ?: "Gagal memulai VPN")
                stopVpn()
                stopSelf()
            }
        }
    }

    private suspend fun reloadVpnRules() {
        try {
            configureAndEstablishTunnel()
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading VPN rules", e)
        }
    }

    private suspend fun configureAndEstablishTunnel() {
        val db = AppDatabase.getInstance(applicationContext)
        val blockedPackages = db.appRuleDao().getBlockedPackageNames()
        val allRules = db.appRuleDao().getAllRulesDirect()
        val allowedCount = allRules.size - blockedPackages.size

        // Close existing interface before recreating
        packetLoopJob?.cancel()
        vpnInterface?.close()
        vpnInterface = null

        val builder = Builder()
            .setSession("DataSaver Shield")
            .setMtu(1500)
            // Virtual IP for our local blackhole tunnel
            .addAddress("10.120.0.1", 32)
            // Route all standard IPv4 & IPv6 traffic destined from the selected apps into our blackhole TUN
            .addRoute("0.0.0.0", 0)

        // Route only BLOCKED apps into this blackhole interface.
        // Apps NOT added to the builder bypass VPN completely and connect directly to internet!
        var addedAnyBlocked = false
        for (pkg in blockedPackages) {
            try {
                builder.addAllowedApplication(pkg)
                addedAnyBlocked = true
            } catch (e: Exception) {
                Log.w(TAG, "Cannot add package $pkg to VPN interface: ${e.message}")
            }
        }

        // If no apps are blocked yet, add dummy or our own package to keep VPN alive safely
        if (!addedAnyBlocked) {
            // Note: If no applications are allowed, Android VPN builder would route all traffic or error out.
            // When no apps blocked, we add an impossible non-existent package or our own inactive hook
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                // ignore
            }
        }

        // Establish the TUN interface
        val pfd = builder.establish()
        if (pfd == null) {
            Log.e(TAG, "VpnService.Builder.establish() returned null")
            VpnStateManager.setError("Izin VPN belum aktif atau ditolak oleh sistem")
            return
        }

        vpnInterface = pfd
        VpnStateManager.setRunning(true, blockedCount = blockedPackages.size, allowedCount = allowedCount)
        updateForegroundNotification("${blockedPackages.size} aplikasi diblokir dari internet")

        // Start blackhole packet consumer loop to drop captured packets
        startPacketDropLoop(pfd)
    }

    private fun startPacketDropLoop(pfd: ParcelFileDescriptor) {
        packetLoopJob?.cancel()
        packetLoopJob = serviceScope.launch {
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val buffer = ByteArray(32767)
            try {
                while (isActive) {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        // Drop packet by doing nothing, increment statistics
                        VpnStateManager.incrementDroppedPackets(1)
                    } else if (length < 0) {
                        break
                    }
                }
            } catch (e: IOException) {
                if (isActive) {
                    Log.d(TAG, "VPN Packet loop terminated: ${e.message}")
                }
            }
        }
    }

    private fun stopVpn() {
        packetLoopJob?.cancel()
        packetLoopJob = null

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null

        VpnStateManager.setRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.vpn_channel_name)
            val descriptionText = getString(R.string.vpn_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MyVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("DataSaver Shield Aktif")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.vpn_action_stop),
                stopPendingIntent
            )
            .build()
    }

    private fun updateForegroundNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
