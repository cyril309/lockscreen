package com.lockscreen.app

import android.app.AlarmManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.BatteryManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Widgets {

    val ids = listOf("battery", "alarm", "weather", "day")
    val names = listOf("Battery", "Alarm", "Weather", "Day")

    fun enabled(context: Context): Set<String> {
        val raw = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
            .getString("widgets", "battery,alarm,weather") ?: ""
        return raw.split(",").filter { it.isNotEmpty() }.toSet()
    }

    fun save(context: Context, chosen: Set<String>) {
        val ordered = ids.filter { it in chosen }.joinToString(",")
        context.getSharedPreferences("lock", Context.MODE_PRIVATE)
            .edit().putString("widgets", ordered).apply()
    }

    // Where the widget row starts, depending on the clock layout.
    fun topMargin(context: Context, screenH: Int): Int {
        val d = context.resources.displayMetrics.density
        val compact = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
            .getInt("clock_layout", 0) == 1
        return if (compact) (screenH * 0.14f + 100 * d).toInt() else (screenH * 0.575f).toInt()
    }

    fun buildRow(context: Context): LinearLayout {
        val d = context.resources.displayMetrics.density
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        val chosen = enabled(context)
        for (id in ids) {
            if (id !in chosen) continue
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins((6 * d).toInt(), 0, (6 * d).toInt(), 0)
            row.addView(WidgetView(context, id), lp)
        }
        return row
    }
}

class WidgetView(context: Context, private val kind: String) : View(context) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = (64 * d).toInt()
        setMeasuredDimension(s, s)
    }

    private fun label(c: Canvas, text: String, y: Float, sizeDp: Float, bold: Boolean, alpha: Int) {
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = Color.argb(alpha, 255, 255, 255)
        p.textAlign = Paint.Align.CENTER
        p.textSize = sizeDp * d
        p.isFakeBoldText = bold
        c.drawText(text, width / 2f, y, p)
        p.isFakeBoldText = false
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = width / 2f - d

        p.shader = null
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#40FFFFFF")
        c.drawCircle(cx, cy, r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = d
        p.color = Color.parseColor("#55FFFFFF")
        c.drawCircle(cx, cy, r, p)

        when (kind) {
            "battery" -> {
                val bm = context.getSystemService(BatteryManager::class.java)
                val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
                val inset = 6 * d
                val oval = RectF(inset, inset, width - inset, height - inset)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 4 * d
                p.strokeCap = Paint.Cap.ROUND
                p.color = Color.parseColor("#40FFFFFF")
                c.drawArc(oval, 0f, 360f, false, p)
                p.color = Color.WHITE
                c.drawArc(oval, -90f, 360f * level / 100f, false, p)
                p.strokeCap = Paint.Cap.BUTT
                label(c, "$level", cy + 5 * d, 15f, true, 255)
            }
            "alarm" -> {
                val am = context.getSystemService(AlarmManager::class.java)
                val next = am.nextAlarmClock
                val time = if (next != null) {
                    SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(next.triggerTime))
                } else {
                    "--"
                }
                label(c, time, cy + 2 * d, 16f, true, 255)
                label(c, "ALARM", cy + 18 * d, 8f, true, 170)
            }
            "weather" -> {
                val cached = Weather.cached(context)
                val temp = cached?.substringBefore(' ') ?: "--"
                var word = cached?.substringAfter(' ', "") ?: ""
                if (word.length > 8) word = word.take(7) + "…"
                label(c, temp, cy + 2 * d, 16f, true, 255)
                label(c, if (word.isEmpty()) "WEATHER" else word.uppercase(), cy + 18 * d, 8f, true, 170)
            }
            else -> {
                val cal = Calendar.getInstance()
                val day = cal.get(Calendar.DAY_OF_MONTH).toString()
                val week = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time).uppercase()
                label(c, day, cy + 4 * d, 20f, true, 255)
                label(c, week, cy + 20 * d, 8f, true, 170)
            }
        }
    }
}
