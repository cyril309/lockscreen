package com.lockscreen.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class LockService : Service() {
    companion object {
        @Volatile var isRunning: Boolean = false
            private set
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_SCREEN_ON) return
            // Android may block activity launches from background services. The ongoing
            // notification always provides a user-initiated route into the display.
            try {
                val activityIntent = Intent(context, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(activityIntent)
            } catch (_: Exception) {
                // Keep the service and its notification alive; do not crash on vendor restrictions.
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val channelId = "lockscreen_service"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Lockscreen display", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows that the lockscreen-style display is active and lets you open it."
            }
        )
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Lockscreen display is running")
            .setContentText("Tap to open. Android's system lock remains responsible for security.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
        getSharedPreferences("lock", Context.MODE_PRIVATE).edit().putBoolean("service_active", true).apply()

        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Receiver was already unregistered during process teardown.
        }
        getSharedPreferences("lock", Context.MODE_PRIVATE).edit().putBoolean("service_active", false).apply()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
