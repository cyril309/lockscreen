package com.lockscreen.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        if (prefs.getBoolean("autostart", false)) {
            context.startForegroundService(Intent(context, LockService::class.java))
        }
    }
}
