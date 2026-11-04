package com.lockscreen.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class PinPad(
    context: Context,
    private val getPin: () -> String?,
    private val onCorrect: () -> Unit,
    private val onClose: () -> Unit,
    private val onPhoneLock: () -> Unit
) : LinearLayout(context) {

    private val dotsRow = LinearLayout(context)
    private val rightLabel = TextView(context)
    private var entered = ""
    private var busy = false

    private val letters = mapOf(
        "2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL",
        "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ"
    )

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private val keySize = minOf(dp(74), (resources.displayMetrics.heightPixels * 0.095f).toInt())

    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundColor(Color.parseColor("#80000000"))
        isClickable = true
        val h = resources.displayMetrics.heightPixels
        setPadding(0, (h * 0.11f).toInt(), 0, dp(24))

        val title = TextView(context)
        title.text = "Enter PIN"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        addView(title)

        val sub = TextView(context)
        sub.text = "Or choose Phone lock below"
        sub.textSize = 13f
        sub.setTextColor(Color.parseColor("#B3FFFFFF"))
        sub.gravity = Gravity.CENTER
        sub.setPadding(0, dp(6), 0, 0)
        addView(sub)

        dotsRow.orientation = LinearLayout.HORIZONTAL
        dotsRow.gravity = Gravity.CENTER
        val dotsParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dotsParams.setMargins(0, dp(22), 0, dp(26))
        addView(dotsRow, dotsParams)

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "")
        )
        for (row in rows) {
            val line = LinearLayout(context)
            line.orientation = LinearLayout.HORIZONTAL
            line.gravity = Gravity.CENTER
            for (label in row) {
                line.addView(key(label))
            }
            addView(line)
        }

        val bottom = LinearLayout(context)
        bottom.orientation = LinearLayout.HORIZONTAL
        bottom.gravity = Gravity.CENTER
        val colW = keySize + dp(18)

        val left = TextView(context)
        left.text = "Phone lock"
        left.textSize = 16f
        left.setTextColor(Color.WHITE)
        left.gravity = Gravity.CENTER
        left.setPadding(0, dp(12), 0, dp(12))
        left.setOnClickListener { onPhoneLock() }
        bottom.addView(left, LinearLayout.LayoutParams(colW, ViewGroup.LayoutParams.WRAP_CONTENT))

        bottom.addView(View(context), LinearLayout.LayoutParams(colW, 1))

        rightLabel.textSize = 16f
        rightLabel.setTextColor(Color.WHITE)
        rightLabel.gravity = Gravity.CENTER
        rightLabel.setPadding(0, dp(12), 0, dp(12))
        rightLabel.setOnClickListener { onRight() }
        bottom.addView(rightLabel, LinearLayout.LayoutParams(colW, ViewGroup.LayoutParams.WRAP_CONTENT))

        val bottomParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        bottomParams.setMargins(0, dp(10), 0, 0)
        addView(bottom, bottomParams)

        updateDots()
    }

    private fun key(label: String): View {
        val params = LinearLayout.LayoutParams(keySize, keySize)
        params.setMargins(dp(9), dp(7), dp(9), dp(7))

        if (label.isEmpty()) {
            val spacer = View(context)
            spacer.layoutParams = params
            return spacer
        }

        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(Color.parseColor("#33FFFFFF"))
        bg.setStroke(dp(1), Color.parseColor("#44FFFFFF"))
        box.background = bg
        box.layoutParams = params

        val digit = TextView(context)
        digit.text = label
        digit.textSize = 28f
        digit.setTextColor(Color.WHITE)
        digit.gravity = Gravity.CENTER
        digit.includeFontPadding = false
        box.addView(digit)

        val sub = letters[label]
        if (sub != null) {
            val t = TextView(context)
            t.text = sub
            t.textSize = 10f
            t.letterSpacing = 0.15f
            t.setTypeface(null, Typeface.BOLD)
            t.setTextColor(Color.WHITE)
            t.gravity = Gravity.CENTER
            t.includeFontPadding = false
            box.addView(t)
        }

        box.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_DOWN) {
                bg.setColor(Color.parseColor("#77FFFFFF"))
            } else if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
                bg.setColor(Color.parseColor("#33FFFFFF"))
            }
            false
        }
        box.setOnClickListener { onDigit(label) }
        return box
    }

    fun reset() {
        entered = ""
        busy = false
        updateDots()
    }

    private fun updateDots() {
        val total = getPin()?.length ?: 4
        dotsRow.removeAllViews()
        for (i in 0 until total) {
            val v = View(context)
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            if (i < entered.length) g.setColor(Color.WHITE) else g.setColor(Color.TRANSPARENT)
            g.setStroke(dp(2), Color.WHITE)
            v.background = g
            val p = LinearLayout.LayoutParams(dp(14), dp(14))
            p.setMargins(dp(9), 0, dp(9), 0)
            dotsRow.addView(v, p)
        }
        rightLabel.text = if (entered.isEmpty()) "Cancel" else "Delete"
    }

    private fun onRight() {
        if (busy) return
        if (entered.isEmpty()) {
            onClose()
        } else {
            entered = entered.dropLast(1)
            updateDots()
        }
    }

    private fun onDigit(c: String) {
        if (busy) return
        val pin = getPin() ?: return
        if (entered.length >= pin.length) return
        entered += c
        updateDots()
        if (entered.length == pin.length) {
            if (entered == pin) {
                entered = ""
                onCorrect()
            } else {
                busy = true
                shake()
                postDelayed({
                    busy = false
                    entered = ""
                    updateDots()
                }, 380)
            }
        }
    }

    private fun shake() {
        val d = dp(14).toFloat()
        dotsRow.animate().translationX(d).setDuration(60).withEndAction {
            dotsRow.animate().translationX(-d).setDuration(80).withEndAction {
                dotsRow.animate().translationX(0f).setDuration(60).start()
            }.start()
        }.start()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView === this && visibility == View.VISIBLE) {
            alpha = 0f
            animate().alpha(1f).setDuration(180).start()
        }
    }
}
