package com.lockscreen.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val pickCode = 42
    private lateinit var notificationStatus: TextView
    private lateinit var serviceNotificationStatus: TextView
    private lateinit var locationStatus: TextView
    private lateinit var serviceStatus: TextView
    private lateinit var overlayStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val prefs = getSharedPreferences("lock", Context.MODE_PRIVATE)
        // Remove the legacy app-only PIN. It was not equivalent to Android's secure keyguard.
        prefs.edit().remove("pin").apply()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        fun addText(value: String, size: Float): TextView = TextView(this).apply {
            text = value
            textSize = size
            setPadding(0, pad / 2, 0, pad / 4)
            root.addView(this)
        }
        fun addButton(value: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                text = value
                setOnClickListener { action() }
            })
        }

        addText("Lockscreen-style display", 24f)
        addText(
            "Important: Android remains responsible for locking and unlocking your device. This app is a visual screen only; it cannot replace or strengthen your phone's system lock. Keep a secure screen lock enabled in Android Settings.",
            16f
        )
        addText(
            "The screen-on display is best-effort and may be restricted by some Android versions or manufacturers. If it does not open automatically, tap the app's ongoing notification.",
            14f
        )

        addText("Service", 18f)
        serviceStatus = addText("Status: checking…", 14f)
        addButton("Start lock screen display") {
            try {
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(Intent(this, LockService::class.java))
                else startService(Intent(this, LockService::class.java))
                prefs.edit().putBoolean("autostart", true).apply()
                Toast.makeText(this, "Display service started", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                prefs.edit().putBoolean("autostart", false).apply()
                Toast.makeText(this, "Could not start service: ${e.localizedMessage ?: "check app permissions"}", Toast.LENGTH_LONG).show()
            }
            updateStatuses()
        }
        addButton("Stop lock screen display") {
            prefs.edit().putBoolean("autostart", false).apply()
            stopService(Intent(this, LockService::class.java))
            Toast.makeText(this, "Display service stopped", Toast.LENGTH_SHORT).show()
            updateStatuses()
        }

        addText("Optional features and permissions", 18f)
        overlayStatus = addText("Auto-open permission: checking…", 14f)
        addButton("Allow display over other apps (auto-open)") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        addText(
            "Optional. Android only lets the display open by itself when the screen turns on if this is allowed. It lets the app draw above other apps and does not let it read your PIN or other apps' content. Leave it off if you prefer to open the display from the app's ongoing notification.",
            14f
        )
        serviceNotificationStatus = addText("Service notification permission: checking…", 14f)
        addButton("Allow service notifications") {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            } else {
                Toast.makeText(this, "Notifications are controlled by Android settings on this version", Toast.LENGTH_SHORT).show()
            }
        }
        notificationStatus = addText("Notification access: checking…", 14f)
        addButton("Manage notification access") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        addText(
            "Notification access lets the app read notification titles and previews so it can show them on the display. Leave it off if you do not want that. Notification text is hidden by default.",
            14f
        )
        addButton("Notification previews: hidden / after confirm / always") {
            val next = (Notifs.previewMode(this) + 1) % 3
            prefs.edit().putInt("preview_mode", next).apply()
            Toast.makeText(
                this,
                listOf("Previews hidden", "Previews shown after you confirm it's you", "Previews always shown")[next],
                Toast.LENGTH_SHORT
            ).show()
        }

        locationStatus = addText("Weather location: checking…", 14f)
        addButton("Allow approximate location for weather") {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 2)
            }
        }
        addText(
            "Weather is optional. When enabled, the app uses the last known approximate location and sends its coordinates to Open-Meteo to request current conditions. It does not continuously track location.",
            14f
        )
        addButton("Clear cached weather") {
            prefs.edit().remove("weather").remove("weather_time").apply()
            Toast.makeText(this, "Cached weather cleared", Toast.LENGTH_SHORT).show()
        }

        addText("Appearance", 18f)
        addText("Colors are used when no photo wallpaper is set.", 14f)
        for (i in Themes.names.indices) {
            addButton("Color: ${Themes.names[i]}") {
                prefs.edit().putInt("theme", i).apply()
                Toast.makeText(this, "${Themes.names[i]} selected", Toast.LENGTH_SHORT).show()
            }
        }
        addButton("Choose photo wallpaper") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, pickCode)
        }
        addButton("Remove photo wallpaper") {
            Wallpaper.clear(this)
            Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show()
        }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        updateStatuses()
    }

    override fun onResume() {
        super.onResume()
        if (::notificationStatus.isInitialized) updateStatuses()
    }

    private fun updateStatuses() {
        val prefs = getSharedPreferences("lock", Context.MODE_PRIVATE)
        val listenerEnabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        notificationStatus.text = "Notification access: ${if (listenerEnabled) "enabled" else "off"}"
        val hasLocation = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        locationStatus.text = "Approximate location for weather: ${if (hasLocation) "allowed" else "off"}"
        overlayStatus.text = "Auto-open permission: ${if (Settings.canDrawOverlays(this)) "allowed" else "off (tap the notification to open the display)"}"
        val running = LockService.isRunning
        serviceStatus.text = "Display service: ${if (running) "running" else "not running"}; autostart after reboot: ${if (prefs.getBoolean("autostart", false)) "on" else "off"}"
        val notificationGranted = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        serviceNotificationStatus.text = "Service notifications: ${if (notificationGranted) "allowed" else "not allowed (Android may hide the notification)"}"
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickCode && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            Thread {
                val ok = Wallpaper.save(this, uri)
                runOnUiThread {
                    Toast.makeText(this, if (ok) "Photo saved" else "Could not use that photo", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }
}
