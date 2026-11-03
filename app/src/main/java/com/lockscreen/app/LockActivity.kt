package com.lockscreen.app

import android.app.Activity
import android.app.AlarmManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LockActivity : Activity() {

    private lateinit var clock: ClockView
    private lateinit var pinPad: PinPad
    private lateinit var bottomRow: LinearLayout
    private lateinit var arrows: TextView
    private var startY = 0f

    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
    private val match = ViewGroup.LayoutParams.MATCH_PARENT

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun savedPin(): String? =
        getSharedPreferences("lock", Context.MODE_PRIVATE).getString("pin", null)

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
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val screenH = resources.displayMetrics.heightPixels
        val root = FrameLayout(this)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#C8532B"),
                Color.parseColor("#CF6A35"),
                Color.parseColor("#B96A4C"),
                Color.parseColor("#3B3A52"),
                Color.parseColor("#4A6A96"),
                Color.parseColor("#8FB4D9"),
                Color.parseColor("#A9C8E6")
            )
        )

        val bm = getSystemService(BatteryManager::class.java)
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val battery = TextView(this)
        battery.text = "$level%"
        battery.textSize = 13f
        battery.setTextColor(Color.WHITE)
        root.addView(battery, lp(wrap, wrap, Gravity.TOP or Gravity.END, 0, dp(12), dp(20), 0))

        val lock = ImageView(this)
        lock.setImageResource(android.R.drawable.ic_lock_idle_lock)
        lock.setColorFilter(Color.WHITE)
        root.addView(lock, lp(dp(18), dp(18), Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, dp(36), 0, 0))

        val date = TextView(this)
        date.text = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
        date.textSize = 15f
        date.setTextColor(Color.parseColor("#F6DCCB"))
        root.addView(date, lp(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, dp(64), 0, 0))

        clock = ClockView(this)
        root.addView(clock, lp(match, (screenH * 0.42f).toInt(), Gravity.TOP, 0, (screenH * 0.14f).toInt(), 0, 0))

        arrows = TextView(this)
        arrows.text = "▲\n▲"
        arrows.textSize = 12f
        arrows.gravity = Gravity.CENTER
        arrows.setTextColor(Color.parseColor("#E8E2DC"))
        root.addView(arrows, lp(wrap, wrap, Gravity.BOTTOM or Gravity.END, 0, 0, dp(26), dp(116)))

        bottomRow = LinearLayout(this)
        bottomRow.orientation = LinearLayout.HORIZONTAL
        bottomRow.gravity = Gravity.CENTER_VERTICAL

        val assistant = circle("★", Color.parseColor("#F4F1EC"), Color.parseColor("#5B7BD5"))
        assistant.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(this, "No assistant found", Toast.LENGTH_SHORT).show()
            }
        }
        bottomRow.addView(assistant, LinearLayout.LayoutParams(dp(44), dp(44)))

        val am = getSystemService(AlarmManager::class.java)
        val next = am.nextAlarmClock
        val pill = TextView(this)
        pill.text = if (next != null) {
            "Alarm " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(next.triggerTime))
        } else {
            "Swipe up to unlock"
        }
        pill.textSize = 14f
        pill.gravity = Gravity.CENTER
        pill.setTextColor(Color.WHITE)
        val pillBg = GradientDrawable()
        pillBg.cornerRadius = dp(22).toFloat()
        pillBg.setColor(Color.parseColor("#4A3F48"))
        pill.background = pillBg
        val pillParams = LinearLayout.LayoutParams(0, dp(44), 1f)
        pillParams.setMargins(dp(10), 0, dp(10), 0)
        bottomRow.addView(pill, pillParams)

        val camera = circle("◉", Color.parseColor("#A85D3A"), Color.WHITE)
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
        bottomRow.addView(camera, LinearLayout.LayoutParams(dp(44), dp(44)))

        root.addView(bottomRow, lp(match, wrap, Gravity.BOTTOM, dp(16), 0, dp(16), dp(56)))

        pinPad = PinPad(
            this,
            { savedPin() },
            { dismissSystemLock() },
            { hidePin() },
            { dismissSystemLock() }
        )
        pinPad.visibility = View.GONE
        root.addView(pinPad, lp(match, (screenH * 0.55f).toInt(), Gravity.BOTTOM))

        setContentView(root)
        hideBars()
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

    private fun circle(label: String, bg: Int, fg: Int): TextView {
        val t = TextView(this)
        t.text = label
        t.textSize = 18f
        t.setTextColor(fg)
        t.gravity = Gravity.CENTER
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(bg)
        t.background = g
        return t
    }

    private fun showPin() {
        pinPad.reset()
        pinPad.visibility = View.VISIBLE
        bottomRow.visibility = View.INVISIBLE
        arrows.visibility = View.INVISIBLE
        clock.pivotX = clock.width / 2f
        clock.pivotY = 0f
        clock.animate().scaleX(0.5f).scaleY(0.5f).setDuration(200).start()
    }

    private fun hidePin() {
        pinPad.reset()
        pinPad.visibility = View.GONE
        bottomRow.visibility = View.VISIBLE
        arrows.visibility = View.VISIBLE
        clock.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    private fun requestUnlock() {
        if (savedPin() == null) dismissSystemLock() else showPin()
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

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            startY = ev.rawY
        }
        if (ev.action == MotionEvent.ACTION_UP) {
            if (pinPad.visibility != View.VISIBLE && startY - ev.rawY > dp(100)) {
                requestUnlock()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (pinPad.visibility == View.VISIBLE) hidePin()
    }
}
