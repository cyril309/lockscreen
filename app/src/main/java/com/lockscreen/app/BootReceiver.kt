package com.lockscreen.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supportedAction = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (!supportedAction) return

        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("autostart", false)) return

        try {
            val service = Intent(context, LockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service)
            else context.startService(service)
        } catch (_: Exception) {
            // Android may restrict foreground-service starts after boot on some versions/devices.
            prefs.edit().putBoolean("service_active", false).apply()
        }
    }
}
