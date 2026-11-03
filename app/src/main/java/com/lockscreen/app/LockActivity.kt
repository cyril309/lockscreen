package com.lockscreen.app

import android.app.Activity
import android.app.AlarmManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ClockView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 10000)
        }
    }

    init {
        paint.color = Color.parseColor("#BCD2EE")
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(tick)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val cal = Calendar.getInstance()
        var hour = cal.get(Calendar.HOUR)
        if (hour == 0) hour = 12
        val minute = cal.get(Calendar.MINUTE)
        val hourDigits = hour.toString().map { it - '0' }
        val digits = hourDigits + listOf(minute / 10, minute % 10)

        val h = height.toFloat()
        val units = digits.size + 0.35f + 0.3f * (hourDigits.size + 2)
        var dw = width * 0.92f / units
        val maxDw = h * 0.21f
        if (dw > maxDw) dw = maxDw
        val gap = dw * 0.3f
        val colonW = dw * 0.35f
        val sw = dw * 0.28f
        val total = digits.size * dw + colonW + (hourDigits.size + 2) * gap
        var x = (width - total) / 2f

        for (i in digits.indices) {
            if (i == hourDigits.size) {
                paint.style = Paint.Style.FILL
                val cx = x + colonW / 2f
                canvas.drawCircle(cx, h * 0.25f, sw * 0.5f, paint)
                canvas.drawCircle(cx, h * 0.72f, sw * 0.5f, paint)
                x += colonW + gap
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = sw
            canvas.save()
            canvas.translate(x + sw / 2f, sw / 2f)
            canvas.drawPath(digitPath(digits[i], dw - sw, h - sw), paint)
            canvas.restore()
            x += dw + gap
        }
    }

    private fun digitPath(d: Int, w: Float, h: Float): Path {
        val p = Path()
        val r = w / 2f
        when (d) {
            0 -> p.addRoundRect(RectF(0f, 0f, w, h), r, r, Path.Direction.CW)
            1 -> {
                p.moveTo(w * 0.1f, h * 0.1f)
                p.lineTo(w / 2f, 0f)
                p.lineTo(w / 2f, h)
            }
            2 -> {
                p.moveTo(0f, r)
                p.quadTo(0f, 0f, w / 2f, 0f)
                p.quadTo(w, 0f, w, r)
                p.cubicTo(w, h * 0.5f, 0f, h * 0.5f, 0f, h)
                p.lineTo(w, h)
            }
            3 -> {
                p.moveTo(0f, r)
                p.quadTo(0f, 0f, w / 2f, 0f)
                p.quadTo(w, 0f, w, r)
                p.lineTo(w, h / 2f - r)
                p.quadTo(w, h / 2f, w / 2f, h / 2f)
                p.lineTo(w * 0.3f, h / 2f)
                p.moveTo(w / 2f, h / 2f)
                p.quadTo(w, h / 2f, w, h / 2f + r)
                p.lineTo(w, h - r)
                p.quadTo(w, h, w / 2f, h)
                p.quadTo(0f, h, 0f, h - r)
            }
            4 -> {
                p.moveTo(w * 0.75f, h)
                p.lineTo(w * 0.75f, 0f)
                p.lineTo(0f, h * 0.62f)
                p.lineTo(w, h * 0.62f)
            }
            5 -> {
                p.moveTo(w, 0f)
                p.lineTo(0f, 0f)
                p.lineTo(0f, h * 0.44f)
                p.lineTo(w / 2f, h * 0.44f)
                p.quadTo(w, h * 0.44f, w, h * 0.44f + r)
                p.lineTo(w, h - r)
                p.quadTo(w, h, w / 2f, h)
                p.quadTo(0f, h, 0f, h - r)
            }
            6 -> {
                p.addRoundRect(RectF(0f, h * 0.45f, w, h), r, r, Path.Direction.CW)
                p.moveTo(w, r)
                p.quadTo(w, 0f, w / 2f, 0f)
                p.quadTo(0f, 0f, 0f, r)
                p.lineTo(0f, h - r)
            }
            7 -> {
                p.moveTo(0f, 0f)
                p.lineTo(w, 0f)
                p.lineTo(w * 0.3f, h)
            }
            8 -> {
                p.addRoundRect(RectF(0f, 0f, w, h * 0.52f), r, r, Path.Direction.CW)
                p.addRoundRect(RectF(0f, h * 0.48f, w, h), r, r, Path.Direction.CW)
            }
            9 -> {
                p.addRoundRect(RectF(0f, 0f, w, h * 0.55f), r, r, Path.Direction.CW)
                p.moveTo(w, r)
                p.lineTo(w, h - r)
                p.quadTo(w, h, w / 2f, h)
                p.quadTo(0f, h, 0f, h - r)
            }
        }
        return p
    }
}

class LockActivity : Activity() {

    private lateinit var clock: ClockView
    private lateinit var pinPanel: LinearLayout
    private lateinit var dots: TextView
    private lateinit var bottomRow: LinearLayout
    private lateinit var arrows: TextView
    private var entered = ""
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
        root.addView(arrows, lp(wrap, wrap, Gravity.BOTTOM or Gravity.END, 0, 0, dp(26), dp(84)))

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

        root.addView(bottomRow, lp(match, wrap, Gravity.BOTTOM, dp(16), 0, dp(16), dp(24)))

        pinPanel = buildPinPanel()
        root.addView(pinPanel, lp(match, (screenH * 0.55f).toInt(), Gravity.BOTTOM))

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

    private fun buildPinPanel(): LinearLayout {
        val p = LinearLayout(this)
        p.orientation = LinearLayout.VERTICAL
        p.gravity = Gravity.CENTER_HORIZONTAL
        val bg = GradientDrawable()
        val r = dp(28).toFloat()
        bg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        bg.setColor(Color.parseColor("#80000000"))
        p.background = bg
        p.setPadding(0, dp(16), 0, dp(12))
        p.visibility = View.GONE
        p.isClickable = true

        dots = TextView(this)
        dots.textSize = 22f
        dots.setTextColor(Color.WHITE)
        dots.gravity = Gravity.CENTER
        dots.setPadding(0, 0, 0, dp(8))
        p.addView(dots)

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("X", "0", "<")
        )
        for (row in rows) {
            val line = LinearLayout(this)
            line.orientation = LinearLayout.HORIZONTAL
            for (label in row) {
                line.addView(key(label))
            }
            p.addView(line)
        }

        val phoneLock = TextView(this)
        phoneLock.text = "Fingerprint / phone lock"
        phoneLock.textSize = 14f
        phoneLock.setTextColor(Color.WHITE)
        phoneLock.setPadding(dp(12), dp(14), dp(12), dp(8))
        phoneLock.setOnClickListener { dismissSystemLock() }
        p.addView(phoneLock)
        return p
    }

    private fun key(label: String): TextView {
        val t = TextView(this)
        t.text = label
        t.textSize = 20f
        t.gravity = Gravity.CENTER
        t.setTextColor(Color.WHITE)
        val g = GradientDrawable()
        g.cornerRadius = dp(24).toFloat()
        g.setColor(Color.parseColor("#33FFFFFF"))
        t.background = g
        val params = LinearLayout.LayoutParams(dp(64), dp(48))
        params.setMargins(dp(6), dp(4), dp(6), dp(4))
        t.layoutParams = params
        t.setOnClickListener {
            when
