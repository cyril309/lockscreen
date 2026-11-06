package com.lockscreen.app

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LockActivity : Activity() {

    private lateinit var torchButton: ImageView
    private var startY = 0f
    private var torchOn = false

    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
    private val match = ViewGroup.LayoutParams.MATCH_PARENT

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun lp(
        w: Int, h: Int, g: Int,
        l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0
    ): FrameLayout.LayoutParams {
        val p = FrameLayout.LayoutParams(w, h, g)
        p.setMargins(l, t, r, b)
        return p
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 28) {
            val attrs = window.attributes
            attrs.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = attrs
        }
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val prefs = getSharedPreferences("lock", Context.MODE_PRIVATE)
        var theme = prefs.getInt("theme", 0)
        if (theme < 0 || theme >= Themes.names.size) theme = 0

        val screenH = resources.displayMetrics.heightPixels
        val root = FrameLayout(this)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            Themes.gradient(theme)
        )

        val photo = Wallpaper.load(this)
        if (photo != null) {
            val iv = ImageView(this)
            iv.setImageBitmap(photo)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            root.addView(iv, lp(match, match, Gravity.TOP))
            val shade = View(this)
            shade.setBackgroundColor(Color.parseColor("#33000000"))
            root.addView(shade, lp(match, match, Gravity.TOP))
        }

        val bm = getSystemService(BatteryManager::class.java)
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val battery = TextView(this)
        battery.text = "$level%"
        battery.textSize = 13f
        battery.setTextColor(Color.WHITE)
        root.addView(battery, lp(wrap, wrap, Gravity.TOP or Gravity.END, 0, dp(12), dp(20), 0))

        val lock = ImageView(this)
        lock.setImageResource(R.drawable.ic_lock_open)
        root.addView(lock, lp(dp(20), dp(20), Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, dp(36), 0, 0))

        val date = TextView(this)
        val dateText = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date())
        val cachedWeather = Weather.cached(this)
        date.text = if (cachedWeather != null) "$dateText  ·  $cachedWeather" else dateText
        Weather.refresh(this) { w ->
            runOnUiThread { date.text = "$dateText  ·  $w" }
        }
        date.textSize = 17f
        date.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        date.setTextColor(Color.parseColor("#E6FFFFFF"))
        root.addView(date, lp(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, dp(64), 0, 0))

        val clock = ClockView(this)
        clock.setColor(Themes.clock(theme))
        root.addView(clock, lp(match, (screenH * 0.42f).toInt(), Gravity.TOP, 0, (screenH * 0.14f).toInt(), 0, 0))

        // Notification cards sit on the display like iOS. Previews are hidden unless you turn them on in the app.
        val cards = Notifs.buildRow(this)
        root.addView(cards, lp(match, wrap, Gravity.TOP, 0, (screenH * 0.57f).toInt(), 0, 0))

        torchButton = glassButton(R.drawable.ic_torch)
        torchButton.contentDescription = "Flashlight"
        torchButton.setOnClickListener { toggleTorch() }
        root.addView(torchButton, lp(dp(52), dp(52), Gravity.BOTTOM or Gravity.START, dp(40), 0, 0, dp(52)))

        val camera = glassButton(R.drawable.ic_camera)
        camera.contentDescription = "Camera"
        camera.setOnClickListener {
            try {
                startActivity(
                    Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                Toast.makeText(this, "No camera found", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(camera, lp(dp(52), dp(52), Gravity.BOTTOM or Gravity.END, 0, 0, dp(40), dp(52)))

        val homeBar = View(this)
        val barBg = GradientDrawable()
        barBg.cornerRadius = dp(3).toFloat()
        barBg.setColor(Color.parseColor("#D9FFFFFF"))
        homeBar.background = barBg
        root.addView(homeBar, lp(dp(134), dp(5), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0, 0, dp(10)))

        setContentView(root)
                   root.setOnLongClickListener {
               ClockCustomizer.show(this, root, clock)
               true
           }
        hideBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Rebuild so the time, date, weather, battery and notifications are current on every wake.
        recreate()
    }

    @Suppress("DEPRECATION")
    private fun hideBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    private fun flashlightId(cm: CameraManager): String? = cm.cameraIdList.firstOrNull {
        cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    private fun toggleTorch() {
        try {
            val cm = getSystemService(CameraManager::class.java)
            val id = flashlightId(cm)
            if (id == null) {
                Toast.makeText(this, "No flashlight found", Toast.LENGTH_SHORT).show()
                return
            }
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
            setTorchLook(torchOn)
        } catch (e: Exception) {
            torchOn = false
            setTorchLook(false)
            Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setTorchLook(on: Boolean) {
        val bg = torchButton.background as GradientDrawable
        bg.setColor(Color.parseColor(if (on) "#F2FFFFFF" else "#40FFFFFF"))
        torchButton.setColorFilter(if (on) Color.parseColor("#222222") else Color.WHITE)
    }

    private fun glassButton(res: Int): ImageView {
        val v = ImageView(this)
        v.setImageResource(res)
        v.setColorFilter(Color.WHITE)
        v.setPadding(dp(14), dp(14), dp(14), dp(14))
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(Color.parseColor("#40FFFFFF"))
        g.setStroke(dp(1), Color.parseColor("#55FFFFFF"))
        v.background = g
        return v
    }

    private fun dismissSystemLock() {
        val km = getSystemService(KeyguardManager::class.java)
        if (!km.isKeyguardLocked) {
            finish()
            return
        }
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                finish()
            }
        })
    }

    override fun onStop() {
        if (torchOn) {
            try {
                val cm = getSystemService(CameraManager::class.java)
                val id = flashlightId(cm)
                if (id != null) cm.setTorchMode(id, false)
            } catch (_: Exception) {
                // The camera may have been taken by another app or become unavailable.
            }
            torchOn = false
            if (::torchButton.isInitialized) setTorchLook(false)
        }
        super.onStop()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            startY = ev.rawY
        }
        if (ev.action == MotionEvent.ACTION_UP) {
                if (!ClockCustomizer.isOpen && startY - ev.rawY > dp(100)) {
                dismissSystemLock()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Stay on the display. Swiping up opens Android's own unlock screen.
    }
}
