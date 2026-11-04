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
        addText("Do steps 1 to 4 in order.", 16f)

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

        addText("Colors (used when no photo is set)", 18f)

        for (i in Themes.names.indices) {
            addButton("Color: " + Themes.names[i]) {
                prefs.edit().putInt("theme", i).apply()
                Toast.makeText(this, Themes.names[i] + " selected", Toast.LENGTH_SHORT).show()
            }
        }

        addText("Photo wallpaper", 18f)

        addButton("Choose photo wallpaper") {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)
            i.type = "image/*"
            startActivityForResult(i, pickCode)
        }

        addButton("Remove photo wallpaper") {
            Wallpaper.clear(this)
            Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show()
        }

        addText("Weather (coming in the next update)", 18f)

        addButton("Allow location for weather") {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 2)
        }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickCode && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            Thread {
                val ok = Wallpaper.save(this, uri)
                runOnUiThread {
                    val msg = if (ok) "Photo saved" else "Could not use that photo"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }
}
