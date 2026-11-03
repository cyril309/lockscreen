package com.lockscreen.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class PinPad(
    context: Context,
    private val getPin: () -> String?,
    private val onCorrect: () -> Unit,
    private val onClose: () -> Unit,
    private val onPhoneLock: () -> Unit
) : LinearLayout(context) {

    private val dots = TextView(context)
    private var entered = ""

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val bg = GradientDrawable()
        val r = dp(28).toFloat()
        bg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        bg.setColor(Color.parseColor("#80000000"))
        background = bg
        setPadding(0, dp(16), 0, dp(12))
        isClickable = true

        dots.textSize = 22f
        dots.setTextColor(Color.WHITE)
        dots.gravity = Gravity.CENTER
        dots.setPadding(0, 0, 0, dp(8))
        addView(dots)

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("X", "0", "<")
        )
        for (row in rows) {
            val line = LinearLayout(context)
            line.orientation = LinearLayout.HORIZONTAL
                        line.gravity = Gravity.CENTER_HORIZONTAL
            for (label in row) {
                line.addView(key(label))
            }
            addView(line)
        }

        val phoneLock = TextView(context)
        phoneLock.text = "Fingerprint / phone lock"
        phoneLock.textSize = 14f
        phoneLock.setTextColor(Color.WHITE)
        phoneLock.setPadding(dp(12), dp(14), dp(12), dp(8))
        phoneLock.setOnClickListener { onPhoneLock() }
        addView(phoneLock)
    }

    private fun key(label: String): TextView {
        val t = TextView(context)
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
            when (label) {
                "X" -> onClose()
                "<" -> {
                    entered = entered.dropLast(1)
                    updateDots()
                }
                else -> onDigit(label)
            }
        }
        return t
    }

    fun reset() {
        entered = ""
        updateDots()
    }

    private fun updateDots() {
        val total = getPin()?.length ?: 4
        dots.text = (0 until total).joinToString("  ") {
            if (it < entered.length) "●" else "○"
        }
    }

    private fun onDigit(c: String) {
        val pin = getPin() ?: return
        if (entered.length >= pin.length) return
        entered += c
        updateDots()
        if (entered.length == pin.length) {
            if (entered == pin) {
                entered = ""
                onCorrect()
            } else {
                entered = ""
                updateDots()
                Toast.makeText(context, "Wrong PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
