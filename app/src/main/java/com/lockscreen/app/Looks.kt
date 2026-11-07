package com.lockscreen.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView

object Looks {

    // Draws the date line inside an outlined capsule when "Date capsule" is switched on.
    fun styleDate(context: Context, date: TextView) {
        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("date_capsule", false)) return
        val d = context.resources.displayMetrics.density
        date.setPadding((16 * d).toInt(), (6 * d).toInt(), (16 * d).toInt(), (6 * d).toInt())
        val g = GradientDrawable()
        g.cornerRadius = 22 * d
        g.setColor(Color.parseColor("#26FFFFFF"))
        g.setStroke((1 * d).toInt().coerceAtLeast(1), Color.parseColor("#80FFFFFF"))
        date.background = g
    }
}
