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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (20 * resources.displayMetrics.density).toInt()
        val prefs = getSharedPreferences("lock", Context.MODE_PRIVATE)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(pad, pad, pad, pad)

        fun addText(value: String, size: Float) {
            val t = TextView(this)
            t.text = value
            t.textSize = size
            t.setPadding(0, pad / 2, 0, pad / 4)
            root.addView(t)
        }

        fun addButton(value: String, action: () -> Unit) {
            val b = Button(this)
            b.text = value
            b.setOnClickListener { action() }
            root.addView(b)
        }

        addText("Lockscreen setup", 24f)
        addText("Do these steps in order.", 16f)

        addButton("1. Allow display over other apps") {
            val i = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(i)
        }

        addButton("2. Allow notifications") {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        addText("3. Optional PIN for this lock screen (4 to 6 digits). Leave it unset to unlock with your phone's own lock instead.", 16f)

        val pinBox = EditText(this)
        pinBox.hint = "New PIN"
        pinBox.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        root.addView(pinBox)

        addButton("Save PIN") {
            val v = pinBox.text.toString()
            if (v.length in 4..6) {
                prefs.edit().putString("pin", v).apply()
                pinBox.setText("")
                Toast.makeText(this, "PIN saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Use 4 to 6 digits", Toast.LENGTH_SHORT).show()
            }
        }

        addButton("Remove PIN") {
            prefs.edit().remove("pin").apply()
            Toast.makeText(this, "PIN removed", Toast.LENGTH_SHORT).show()
        }

        addText("4. Start the lock screen. It will also start by itself after a reboot until you tap Stop.", 16f)

        addButton("Start lock screen") {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Do step 1 first", Toast.LENGTH_LONG).show()
            } else {
                prefs.edit().putBoolean("autostart", true).apply()
                startForegroundService(Intent(this, LockService::class.java))
                Toast.makeText(this, "Lock screen started", Toast.LENGTH_SHORT).show()
            }
        }

        addButton("Stop lock screen") {
            prefs.edit().putBoolean("autostart", false).apply()
            stopService(Intent(this, LockService::class.java))
            Toast.makeText(this, "Lock screen stopped", Toast.LENGTH_SHORT).show()
        }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }
}
